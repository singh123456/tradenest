package com.aakash.tradenest.portfolio.controller;

import com.aakash.tradenest.common.exception.StockNotFoundException;
import com.aakash.tradenest.portfolio.entity.Holding;
import com.aakash.tradenest.portfolio.dto.HoldingResponse;
import com.aakash.tradenest.portfolio.mapper.HoldingMapper;
import com.aakash.tradenest.portfolio.service.PortfolioService;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserService userService;
    private final HoldingMapper holdingMapper;
    private final StockRepository stockRepository;

    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getPortfolio(
            @AuthenticationPrincipal UserDetails principal
            ){
        Long userId = userService.getCurrentUserId(principal.getUsername());

        return ResponseEntity.ok(portfolioService.getHoldings(userId));
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<HoldingResponse> getHolding(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String symbol
    ){
        Long userid = userService.getCurrentUserId(principal.getUsername());
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(()->
                        new StockNotFoundException(symbol));

        Holding holding = portfolioService.getHoldingForSymbol(
                userid,
                stock
        );

        return ResponseEntity.ok(
                holdingMapper.toResponse(holding)
        );
    }
}
