# TradeNest

A backend trading platform simulating a mini Zerodha/Groww — built to demonstrate backend engineering depth: concurrent data structures, event-driven settlement, layered service architecture, and real test coverage (including a genuine concurrency test on the matching engine).

Built as a **Java/Spring Boot monolith with service-oriented package boundaries**, so each module could be lifted into its own microservice with minimal rework.

---

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [The Matching Engine](#the-matching-engine)
- [Tech Stack](#tech-stack)
- [Running Locally](#running-locally)
- [API Reference](#api-reference)
- [Testing](#testing)
- [Design Decisions](#design-decisions)
- [Roadmap](#roadmap)

---

## Overview

TradeNest lets a user register, fund a wallet, browse live-simulated stock prices, place buy/sell orders that match against each other with real price-time priority, and track their resulting holdings and profit/loss — the full lifecycle of a trade, end to end.

**Five core services, all built and tested:**

| Service | Responsibility |
|---|---|
| **User** | Registration, login, JWT authentication |
| **Wallet** | Balance management, deposits, trade settlement (debit/credit), transaction ledger |
| **Stock** | Stock listing, simulated real-time price movement |
| **Order** | Order placement, validation, and a real in-memory matching engine |
| **Portfolio** | Holdings tracking, weighted-average cost basis, live unrealized P&L |

---

## Architecture

```
com.aakash.tradenest
├── user/           User accounts, auth
│   ├── entity / repository / service / controller / dto
├── wallet/         Balance, transactions
│   ├── entity / repository / service / controller / dto
├── stock/          Stock listing, price simulation
│   ├── entity / repository / service / controller / config (seeder)
├── order/          Order placement + matching engine
│   ├── entity / repository / service / controller / dto / mapper
│   └── matching/   OrderBook, OrderBookManager, OrderMatchingEngine
├── portfolio/      Holdings, P&L
│   ├── entity / repository / service / controller / dto / mapper
├── security/       JWT generation/validation, auth filter
├── config/         Spring Security configuration
└── common/
    └── exception/  Custom exceptions + centralized GlobalExceptionHandler
```

**Layering within every module:** `Controller → Service → Repository → Entity`, with DTOs at every API boundary. Entities are never returned directly over REST — every response is mapped to a DTO that exposes only what the client needs (notably: `User`'s password hash and internal entity relationships never leak into any response, even transitively through nested objects like wallet transactions or order responses).

**Cross-cutting concerns**, shared across all modules:
- Stateless JWT authentication (`JwtAuthFilter` + `JwtService`), with an explicit `AuthenticationEntryPoint` so unauthenticated requests correctly return `401` rather than Spring Security's default `403`.
- Centralized exception handling (`GlobalExceptionHandler`) — every custom exception maps to a clean, consistent JSON error body (`ApiError`) with the correct HTTP status, instead of leaking stack traces.
- A single PostgreSQL database shared across all modules (a deliberate monolith-first choice — see [Design Decisions](#design-decisions)).

### How the services connect

```
 Registration ──► auto-creates a Wallet (zero balance)

 Place Order ──► OrderService validates:
                  • BUY: sufficient wallet balance?
                  • SELL: sufficient holdings?
                 → persists Order (status: OPEN)
                 → hands off to OrderMatchingEngine

 OrderMatchingEngine ──► matches against the in-memory order book
                      ──► on every match:
                           • creates a Trade record
                           • WalletService.debit(buyer) / credit(seller)
                           • PortfolioService.updateHoldingOnBuy/Sell
```

Order Service is the hub: it's the only module that calls into Wallet and Portfolio directly. Wallet and Portfolio never call each other or know about Order Service — they just expose operations (`debit`, `credit`, `updateHoldingOnBuy`, `updateHoldingOnSell`) that the matching engine invokes as trades settle.

---

## Services

### User Service
- `POST /api/auth/register`, `POST /api/auth/login` — JWT issued on both
- `GET /api/users/me` — protected profile endpoint
- Passwords hashed with BCrypt; `User` implements Spring Security's `UserDetails` directly
- Registration synchronously creates a zero-balance `Wallet` for the new user, inside the same transaction — a user can never exist without a wallet

### Wallet Service
- `POST /api/wallet/add-money`, `GET /api/wallet/balance`, `GET /api/wallet/transactions` (paginated)
- Every balance change is paired with a `WalletTransaction` audit-trail row (`type`: CREDIT/DEBIT, `reason`: DEPOSIT/TRADE_BUY/TRADE_SELL) — `balance` is a derived/cached value; the transaction log is the source of truth
- **Optimistic locking** via a `@Version` field on `Wallet`, verified with a dedicated concurrency test that forces a real version conflict at the database level
- `debit`/`credit` are not exposed as public endpoints — they're only called internally by the matching engine during trade settlement

### Stock Service
- `GET /api/stocks`, `GET /api/stocks/{symbol}` — public, no auth required
- 10 stocks seeded on startup via a `CommandLineRunner` (idempotent — checks `count() > 0` before inserting)
- A `@Scheduled` job nudges every stock's price by a random ±2% every 5 seconds, with a floor guard against zero/negative prices — this is what makes the order book and portfolio P&L feel alive in a live demo

### Order Service
See [The Matching Engine](#the-matching-engine) below — this is the centerpiece of the project.
- `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}`
- LIMIT orders only (MARKET orders are modeled in the schema but explicitly rejected — a deliberate scope decision, not an oversight)
- BUY orders validate sufficient wallet balance; SELL orders validate sufficient holdings — both *before* the order is accepted

### Portfolio Service
- `GET /api/portfolio` (all holdings, with live P&L), `GET /api/portfolio/{symbol}`
- One row per `(user, stock)` pair — a **current-state view**, not a ledger. Unlike Wallet, Portfolio doesn't need its own transaction log, because `Trade` already serves as the permanent audit trail; holdings are always reconstructable from trade history
- Weighted-average cost basis recalculated on every buy: `newAvg = ((existingQty × existingAvg) + (tradeQty × tradePrice)) / (existingQty + tradeQty)`
- Selling never changes `avgBuyPrice` — only quantity. A holding is deleted entirely once its quantity reaches zero, rather than left as a dangling zero-quantity row
- Live P&L computed on read (not stored): `(currentPrice − avgBuyPrice) × quantity`, using the stock's *current* simulated price

---

## The Matching Engine

The part of this project modeled most closely on real exchange architecture — inspired by NSE's own split between an order/trade/inquiry layer and a dedicated matching core (NSE's matching runs in C for latency reasons; this project's matching engine is Java, since it isn't operating at real exchange latency, but the *architectural separation* is the same idea).

**Design:**
- Each stock gets its own `OrderBook` — two `PriorityQueue`s (buy-side max-heap, sell-side min-heap), ordered by **price-time priority**: best price first, earliest timestamp breaks ties.
- `OrderBookManager` holds one `OrderBook` per symbol in a `ConcurrentHashMap`, created lazily and atomically via `computeIfAbsent` — this prevents a race where two threads could each create a separate book for the same symbol.
- Each `OrderBook` is guarded by its own `ReentrantLock` — **locking is per-stock, not global**, so a match on RELIANCE never blocks a match on TCS.
- The entire "peek best opposite order → check if prices cross → execute → poll if fully consumed" sequence for one incoming order is wrapped in a **single lock acquisition** for that stock's book — not four separate lock/unlock cycles — because the whole sequence must be atomic: releasing the lock between steps would let another thread interleave and match against a stale view of the book.

**Matching algorithm** (`OrderMatchingEngine.processNewOrder`):
1. While the incoming order still has unfilled quantity:
   - Peek the best order on the opposite side of the book.
   - If the book is empty on that side, or prices don't cross, stop.
   - Otherwise, match `min(incoming remaining, resting remaining)` shares at the **resting order's price** (rewards the order that was already providing liquidity).
   - Create a `Trade` record, update both orders' `filledQuantity`/`status`, settle funds and holdings.
   - If the resting order is now fully filled, remove it from the book.
2. Whatever remains unfilled (fully or partially) rests on the book for future matching.

**Verified under real concurrency**, not just sequential tests: `OrderConcurrencyTest` uses a three-latch pattern (`readyLatch` / `startLatch` / `doneLatch`) to force two threads to submit crossing orders at genuinely the same instant, then asserts the trade executed exactly once with correct quantities — proving the per-stock lock design actually prevents lost or duplicated matches under contention, not just that it compiles.

---

## Tech Stack
- **Java 21**, Spring Boot 3.5.x
- Spring Security + JWT (`jjwt`)
- Spring Data JPA + PostgreSQL (H2 for tests)
- Lombok
- JUnit 5, MockMvc, AssertJ

---

## Running Locally

### 1. Start Postgres
```bash
docker compose up -d
```

### 2. Run the app
```bash
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`. Tables auto-created (`ddl-auto: update`); 10 stocks seed automatically on first startup.

### 3. Run tests
```bash
./mvnw test
```
Runs entirely against an in-memory H2 database — no Postgres/Docker required.

---
