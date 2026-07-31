package com.aakash.tradenest.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "wallet_transactions")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false,precision = 19,scale = 4)
    private BigDecimal transactionAmount;

    @Column(name = "balance_after",nullable = false,precision = 19,scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private TransactionReason reason;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }

}
