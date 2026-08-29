package com.aakash.tradenest.watchlist.repository;

import com.aakash.tradenest.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WatchlistRepository  extends JpaRepository<Watchlist,Long> {

    List<Watchlist> findByUser_Id(Long userId);

    Optional<Watchlist> findByUser_IdAndStock_Id(Long userId, Long stockId);

    boolean existsByUser_IdAndStock_Id(Long userId, Long stockId);

}
