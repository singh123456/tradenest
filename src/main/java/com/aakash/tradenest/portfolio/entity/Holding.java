package com.aakash.tradenest.portfolio.entity;


import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "holdings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id","stock_id"})
})
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "avg_buy_price",nullable = false, precision = 19, scale = 2)
    private BigDecimal avgBuyPrice;

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
