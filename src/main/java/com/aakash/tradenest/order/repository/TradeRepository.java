package com.aakash.tradenest.order.repository;

import com.aakash.tradenest.order.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade,Long> {
    List<Trade> findByBuyOrder_UserIdOrSellOrder_UserId(Long buyUserId, Long sellUserId);
}
