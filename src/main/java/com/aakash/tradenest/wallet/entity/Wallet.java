package com.aakash.tradenest.wallet.entity;

import com.aakash.tradenest.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false,unique = true)
    private User user;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at",nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if(this.balance == null){
            this.balance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate(){
        this.updatedAt = Instant.now();
    }
}
