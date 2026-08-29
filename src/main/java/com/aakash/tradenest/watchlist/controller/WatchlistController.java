package com.aakash.tradenest.watchlist.controller;

import com.aakash.tradenest.user.service.UserService;
import com.aakash.tradenest.watchlist.dto.WatchlistResponse;
import com.aakash.tradenest.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final UserService userService;

    @PostMapping("/{symbol}")
    public ResponseEntity<WatchlistResponse> addToWatchlist(
            @AuthenticationPrincipal UserDetails principal,
             @PathVariable String symbol
            ){
        Long currentUserId = userService.getCurrentUserId(principal.getUsername());

        WatchlistResponse newWatchlist = watchlistService.addToWatchlist(currentUserId, symbol);

        return ResponseEntity.ok(newWatchlist);
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeFromWatchlist(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String symbol
    ){
        Long currentUserId = userService.getCurrentUserId(principal.getUsername());

        watchlistService.removeFromWatchlist(currentUserId, symbol);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WatchlistResponse>> getWatchlist(
            @AuthenticationPrincipal UserDetails principal
    ){
        Long currentUserId = userService.getCurrentUserId(principal.getUsername());

        return ResponseEntity.ok(watchlistService.getWatchlist(currentUserId));
    }

}
