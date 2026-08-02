package com.aakash.tradenest.stock.service;

import com.aakash.tradenest.common.exception.StockNotFoundException;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    public List<Stock> getAllStocks(){
        return stockRepository.findAll();
    }

    public Stock getStockBySymbol(String symbol){
        return stockRepository.findBySymbol(symbol)
                .orElseThrow(()-> new StockNotFoundException(symbol));
    }
}
