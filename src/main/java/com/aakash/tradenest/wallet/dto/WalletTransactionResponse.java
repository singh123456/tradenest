package com.aakash.tradenest.wallet.dto;

import com.aakash.tradenest.wallet.entity.TransactionReason;
import com.aakash.tradenest.wallet.entity.TransactionType;
import com.aakash.tradenest.wallet.entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionResponse(
        Long id,
        BigDecimal transactionAmount,
        BigDecimal balanceAfter,
        TransactionType type,
        TransactionReason reason,
        Instant createdAt


) {
    public static WalletTransactionResponse from (WalletTransaction walletTransaction){
        return new WalletTransactionResponse(
                walletTransaction.getId(),
                walletTransaction.getTransactionAmount(),
                walletTransaction.getBalanceAfter(),
                walletTransaction.getType(),
                walletTransaction.getReason(),
                walletTransaction.getCreatedAt()
                );
    }
}
