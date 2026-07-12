package com.hanati.bank.bankEx.deposit.savings.config;

import com.hanati.bank.bankEx.deposit.savings.domain.SavingsProduct;
import com.hanati.bank.bankEx.deposit.savings.enums.SavingsProductStatus;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SavingsDataInitializer implements ApplicationRunner {

    private final SavingsProductMapper savingsProductMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (!savingsProductMapper.findAllOnSale().isEmpty()) return;

        savingsProductMapper.insert(SavingsProduct.builder()
                .productId("SAV_BASIC_001")
                .productName("정기적금")
                .baseRate(2.80)
                .maxRate(4.00)
                .minAmount(100_000L)
                .maxAmount(3_000_000L)
                .period(12)
                .autoTransferYn("Y")
                .status(SavingsProductStatus.ON_SALE)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
