package com.aakash.tradenest.stock.controller;

import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<Stock>> getAllStocks(){
        return ResponseEntity.ok()
                .body(stockService.getAllStocks());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Stock> getStockBySymbol(@PathVariable String symbol){
        return ResponseEntity.ok()
                .body(stockService.getStockBySymbol(symbol));
    }
}
