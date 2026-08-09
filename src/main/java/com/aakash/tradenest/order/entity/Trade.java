package com.aakash.tradenest.order.entity;


import com.aakash.tradenest.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Table(name = "trades")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "buy_order_id",nullable = false)
    private Order buyOrder;

    @ManyToOne
    @JoinColumn(name = "sell_order_id",nullable = false)
    private Order sellOrder;

    @ManyToOne
    @JoinColumn(name = "stock_id",nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false,precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "executed_at",nullable = false, updatable = false)
    private Instant executedAt;

    @PrePersist
    void onCreate(){
        this.executedAt = Instant.now();
    }
}
