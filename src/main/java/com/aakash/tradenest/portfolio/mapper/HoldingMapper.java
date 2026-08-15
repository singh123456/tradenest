package com.aakash.tradenest.portfolio.mapper;

import com.aakash.tradenest.portfolio.entity.Holding;
import com.aakash.tradenest.portfolio.dto.HoldingResponse;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class HoldingMapper {

    public HoldingResponse toResponse(Holding holding){
        BigDecimal currentPrice = holding.getStock().getCurrentPrice();

        BigDecimal unrealizedPnL = currentPrice
                .subtract(holding.getAvgBuyPrice())
                .multiply(BigDecimal.valueOf(holding.getQuantity()));

        return new HoldingResponse(
                holding.getStock().getSymbol(),
                holding.getQuantity(),
                holding.getAvgBuyPrice(),
                currentPrice,
                unrealizedPnL
        );
    }
}
