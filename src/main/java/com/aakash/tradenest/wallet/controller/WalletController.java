package com.aakash.tradenest.wallet.controller;

import com.aakash.tradenest.user.service.UserService;
import com.aakash.tradenest.wallet.dto.AddMoneyRequest;
import com.aakash.tradenest.wallet.dto.WalletTransactionResponse;
import com.aakash.tradenest.wallet.entity.WalletTransaction;
import com.aakash.tradenest.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserService userService;
    private final WalletService walletService;


    @PostMapping("/add-money")
    public ResponseEntity<Void> addMoney(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody AddMoneyRequest request){
        Long currentUserId = userService.getCurrentUserId(principal.getUsername());
        walletService.addMoney(currentUserId,request.amount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @AuthenticationPrincipal UserDetails principal
    ){
       Long currentUserId = userService.getCurrentUserId(principal.getUsername());

       return ResponseEntity.ok()
               .body(walletService.getBalance(currentUserId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long currentUserId = userService.getCurrentUserId(principal.getUsername());
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok()
                .body(walletService.getTransactionHistory(currentUserId,pageable));
    }
}
