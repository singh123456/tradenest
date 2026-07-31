package com.aakash.tradenest.wallet.service;

import com.aakash.tradenest.common.exception.InsufficientBalanceException;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.wallet.dto.WalletTransactionResponse;
import com.aakash.tradenest.wallet.entity.TransactionReason;
import com.aakash.tradenest.wallet.entity.TransactionType;
import com.aakash.tradenest.wallet.entity.Wallet;
import com.aakash.tradenest.wallet.entity.WalletTransaction;
import com.aakash.tradenest.wallet.repository.WalletRepository;
import com.aakash.tradenest.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Transactional
    public void createWallet(User user){
        Wallet wallet = Wallet.builder()
                        .user(user)
                        .build();
        walletRepository.save(wallet);
    }

    @Transactional
    public BigDecimal getBalance(Long userId){
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow(()->new UsernameNotFoundException("User not found with this id " +userId));
        return wallet.getBalance();
    }

    @Transactional
    public void addMoney(Long userId,BigDecimal amount){
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(()-> new UsernameNotFoundException("User not found with this id: "+userId));

        wallet.setBalance(wallet.getBalance().add(amount));

        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.CREDIT)
                .reason(TransactionReason.DEPOSIT)
                .transactionAmount(amount)
                .balanceAfter(wallet.getBalance())
                .build();
        walletTransactionRepository.save(transaction);
    }

    @Transactional
    public void debit(Long userId,BigDecimal amount){
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(()->new UsernameNotFoundException("User not found with the id: "+userId));

        if(wallet.getBalance().compareTo(amount) <0){
            throw  new InsufficientBalanceException("Insufficient balance for this transaction");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));

        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEBIT)
                .reason(TransactionReason.TRADE_BUY)
                .transactionAmount(amount)
                .balanceAfter(wallet.getBalance())
                .build();

        walletTransactionRepository.save(transaction);
    }

    @Transactional
    public void credit(Long userId, BigDecimal amount){
          if(amount == null || amount.compareTo(BigDecimal.ZERO) <=0){
              throw new IllegalArgumentException("Amount must be greater than zero");
          }
          Wallet wallet = walletRepository.findByUserId(userId)
                  .orElseThrow(()-> new UsernameNotFoundException("User not found with this id: "+userId));

          wallet.setBalance(wallet.getBalance().add(amount));

          walletRepository.save(wallet);

          WalletTransaction transaction = WalletTransaction.builder()
                  .wallet(wallet)
                  .type(TransactionType.CREDIT)
                  .reason(TransactionReason.TRADE_SELL)
                  .transactionAmount(amount)
                  .balanceAfter(wallet.getBalance())
                  .build();

          walletTransactionRepository.save(transaction);
    }

    public Page<WalletTransactionResponse> getTransactionHistory(Long userId, Pageable pageable){
         Wallet wallet = walletRepository.findByUserId(userId)
                 .orElseThrow(() -> new UsernameNotFoundException("User not found with this id: "+ userId));

         return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(),pageable)
                 .map(WalletTransactionResponse::from);
    }

}
