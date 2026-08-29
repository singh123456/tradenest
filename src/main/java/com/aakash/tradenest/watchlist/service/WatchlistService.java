package com.aakash.tradenest.watchlist.service;

import com.aakash.tradenest.common.exception.StockNotFoundException;
import com.aakash.tradenest.common.exception.WatchlistEntryNotFoundException;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import com.aakash.tradenest.watchlist.dto.WatchlistResponse;
import com.aakash.tradenest.watchlist.entity.Watchlist;
import com.aakash.tradenest.watchlist.repository.WatchlistRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;

    public WatchlistResponse addToWatchlist(Long userId, String symbol){

        User user = userRepository.findById(userId)
                .orElseThrow(()->new UsernameNotFoundException("User not found with the id..."));
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(()->new StockNotFoundException("Stock with this symbol doesnt exist"));

        Watchlist watchlist = watchlistRepository.findByUser_IdAndStock_Id(userId, stock.getId())
                .orElseGet(()-> watchlistRepository.save(
                        Watchlist.builder()
                                .stock(stock)
                                .user(user)
                                .build()
                ));
        return new WatchlistResponse(
                watchlist.getStock().getSymbol(),
                watchlist.getStock().getName(),
                watchlist.getStock().getCurrentPrice()
        );
    }

    public void removeFromWatchlist(Long userId, String symbol){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new UsernameNotFoundException("User not found with the id..."));
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(()->new StockNotFoundException("Stock with this symbol doesnt exist"));

        Watchlist watchlistToBeDeleted = watchlistRepository.findByUser_IdAndStock_Id(userId,stock.getId())
                .orElseThrow(()-> new WatchlistEntryNotFoundException("This stock is not in your watchlist"));
        watchlistRepository.delete(watchlistToBeDeleted);

    }

    public List<WatchlistResponse> getWatchlist(Long userId){

        return watchlistRepository.findByUser_Id(userId).stream()
                .map(w-> new WatchlistResponse(
                        w.getStock().getSymbol(),
                        w.getStock().getName(),
                        w.getStock().getCurrentPrice()
                ))
                .toList();
    }

}
