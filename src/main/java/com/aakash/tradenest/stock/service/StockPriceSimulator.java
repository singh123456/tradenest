package com.aakash.tradenest.stock.service;


import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockPriceSimulator {

    private final StockRepository stockRepository;

    @Scheduled(fixedRate = 5000)
    public void simulatePriceMovement(){
        List<Stock> stocks = stockRepository.findAll();

        for(Stock stock : stocks){
            double randomPercentage = (Math.random() * 4) - 2;
            BigDecimal fluctuatePrice = (stock.getCurrentPrice().multiply(BigDecimal.valueOf(randomPercentage))).divide(BigDecimal.valueOf(100),4, RoundingMode.HALF_UP);
            BigDecimal newPrice = stock.getCurrentPrice().add(fluctuatePrice);
            if(newPrice.compareTo(BigDecimal.ZERO) <= 0){
                newPrice = BigDecimal.valueOf(1);
            }
            stock.setCurrentPrice(newPrice);

        }
        stockRepository.saveAll(stocks);
    }
}
