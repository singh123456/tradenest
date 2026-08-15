package com.aakash.tradenest.portfolio.service;

import com.aakash.tradenest.common.exception.HoldingNotFoundException;
import com.aakash.tradenest.common.exception.InsufficientHoldingException;
import com.aakash.tradenest.portfolio.entity.Holding;
import com.aakash.tradenest.portfolio.dto.HoldingResponse;
import com.aakash.tradenest.portfolio.mapper.HoldingMapper;
import com.aakash.tradenest.portfolio.repository.HoldingRepository;
import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final HoldingMapper holdingMapper;

    public void updateHoldingOnBuy(Long userId, Stock stock, Long quantity, BigDecimal tradePrice){

        Optional<Holding> holdingOfUser = holdingRepository.findByUser_IdAndStock_Id(userId,stock.getId());
        User user = userRepository.findById(userId)
               .orElseThrow(()-> new UsernameNotFoundException("User not found"));

        if(holdingOfUser.isPresent()){
            Long existingQuantity = holdingOfUser.get().getQuantity();
            BigDecimal existingAvgPrice = holdingOfUser.get().getAvgBuyPrice();
            BigDecimal newAvgPrice = (( existingAvgPrice.multiply(BigDecimal.valueOf(existingQuantity))).add(( tradePrice.multiply(BigDecimal.valueOf(quantity))))).divide (BigDecimal.valueOf(existingQuantity + quantity),4, RoundingMode.HALF_UP);
            Long newQuantity = existingQuantity + quantity;
            holdingOfUser.get().setAvgBuyPrice(newAvgPrice);
            holdingOfUser.get().setQuantity(newQuantity);
            holdingRepository.save(holdingOfUser.get());

        }else{
            holdingRepository.save(Holding.builder()
                    .quantity(quantity)
                    .avgBuyPrice(tradePrice)
                    .stock(stock)
                    .user(user)
                    .build());
        }
    }

    public void updateHoldingOnSell(Long userId, Stock stock, Long quantity){
        Holding holding = holdingRepository.findByUser_IdAndStock_Id(userId, stock.getId())
                .orElseThrow(()-> new HoldingNotFoundException("You don't have any holdings of this stock to sell"));

        if(holding.getQuantity() < quantity){
            throw new InsufficientHoldingException("You don't have enough share to sell");
        }

        Long remainingQuantity = holding.getQuantity() - quantity;

        if(remainingQuantity == 0L){
            holdingRepository.delete(holding);
        }else{
            holding.setQuantity(remainingQuantity);
            holdingRepository.save(holding);
        }
    }

    public Holding getHoldingForSymbol(Long userId, Stock stock){
        return holdingRepository.findByUser_IdAndStock_Id(userId, stock.getId())
                .orElseThrow(()-> new HoldingNotFoundException(
                        "Holding for this stock is not present for the user"
                ));
    }

    public List<HoldingResponse> getHoldings(Long userId) {
        return holdingRepository.findByUserId(userId)
                .stream()
                .map(holdingMapper::toResponse)
                .toList();
    }
}
