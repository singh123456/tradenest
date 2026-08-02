package com.aakash.tradenest.stock.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "stocks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,unique = true,length = 20)
    private String symbol;

    @Column(nullable = false,length = 150)
    private String name;

    @Column(name = "current_price",nullable = false,precision = 19,scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

   @PrePersist
   void onCreate(){
       this.updatedAt = Instant.now();
   }

    @PreUpdate
    void onUpdate(){
        this.updatedAt = Instant.now();
    }
}
