package com.aakash.tradenest.order.entity;


import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id",nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private OrderType orderType;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "filled_quantity",nullable = false)
    @Builder.Default
    private Long filledQuantity = 0L;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private OrderStatus status;

    @Column(name = "created_at",nullable = false,updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }
}
