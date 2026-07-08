package com.hanati.bank.bankEx.config;

import com.hanati.bank.bankEx.entity.JeonseLoanProduct;
import com.hanati.bank.bankEx.repository.JeonseLoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JeonseDataInitializer implements ApplicationRunner {

    private final JeonseLoanProductRepository jeonseLoanProductRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (jeonseLoanProductRepository.count() > 0) return;

        jeonseLoanProductRepository.save(JeonseLoanProduct.builder()
                .productId("JEONSE_HF_001")
                .productName("일반 전세자금대출")
                .productType("JEONSE")
                .maxLimitAmount(222_000_000L)
                .baseRate(3.50)
                .minRate(2.50)
                .maxRate(8.00)
                .useYn("Y")
                .createdAt(LocalDateTime.now())
                .build());
    }
}
