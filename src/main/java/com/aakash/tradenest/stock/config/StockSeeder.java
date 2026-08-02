package com.aakash.tradenest.stock.config;

import com.aakash.tradenest.stock.entity.Stock;
import com.aakash.tradenest.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class StockSeeder  implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) throws Exception {
        if(stockRepository.count() > 0){
            return;
        }

        Stock reliance = Stock.builder()
                .symbol("RELIANCE")
                .name("Reliance Industries Ltd")
                .currentPrice(BigDecimal.valueOf(2850.00))
                .build();

        stockRepository.save(reliance);

        Stock tcs = Stock.builder()
                .symbol("TCS")
                .name("Tata Consultancy Services")
                .currentPrice(BigDecimal.valueOf(3300.00))
                .build();
        stockRepository.save(tcs);

        Stock infy = Stock.builder()
                .symbol("INFY")
                .name("Infosys Ltd")
                .currentPrice(BigDecimal.valueOf(1600.00))
                .build();
        stockRepository.save(infy);

        Stock hdfc = Stock.builder()
                .symbol("HDFCBANK")
                .name("HDFC Bank")
                .currentPrice(BigDecimal.valueOf(1560.00))
                .build();
        stockRepository.save(hdfc);

        Stock icici = Stock.builder()
                .symbol("ICICIBANK")
                .name("ICICI Bank")
                .currentPrice(BigDecimal.valueOf(1230.00))
                .build();
        stockRepository.save(icici);

        Stock lt = Stock.builder()
                .symbol("LT")
                .name("Larsen & Toubro")
                .currentPrice(BigDecimal.valueOf(3570.00))
                .build();
        stockRepository.save(lt);

        Stock sbi = Stock.builder()
                .symbol("SBIN")
                .name("State Bank of India")
                .currentPrice(BigDecimal.valueOf(765.00))
                .build();
        stockRepository.save(sbi);

        Stock airtel = Stock.builder()
                .symbol("BHARTIARTL")
                .name("Airtel")
                .currentPrice(BigDecimal.valueOf(1390.00))
                .build();
        stockRepository.save(airtel);

        Stock hal = Stock.builder()
                .symbol("HAL")
                .name("Hindustan Aeronautics")
                .currentPrice(BigDecimal.valueOf(4650.00))
                .build();
        stockRepository.save(hal);

        Stock adani = Stock.builder()
                .symbol("ADANIENT")
                .name("Adani Enterprises")
                .currentPrice(BigDecimal.valueOf(2899.00))
                .build();
        stockRepository.save(adani);
    }
}
