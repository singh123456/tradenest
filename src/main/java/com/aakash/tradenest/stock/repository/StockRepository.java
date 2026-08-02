package com.aakash.tradenest.stock.repository;

import com.aakash.tradenest.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock,Long> {
    Optional<Stock> findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);
}
