package com.aakash.tradenest.order.repository;

import com.aakash.tradenest.order.entity.Order;
import com.aakash.tradenest.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStock_SymbolAndStatusIn(
            String symbol,
            List<OrderStatus> statuses
    );

    Optional<Order> findByUserIdAndId(Long userId, Long id);

    List<Order> findByStatusIn(
            List<OrderStatus> statuses);
}
