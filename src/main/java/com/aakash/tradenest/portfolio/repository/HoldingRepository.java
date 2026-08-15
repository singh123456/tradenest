package com.aakash.tradenest.portfolio.repository;

import com.aakash.tradenest.portfolio.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository  extends JpaRepository<Holding,Long> {

    List<Holding> findByUserId(Long userId);

    Optional<Holding> findByUser_IdAndStock_Id(Long userId, Long stockId);

}
