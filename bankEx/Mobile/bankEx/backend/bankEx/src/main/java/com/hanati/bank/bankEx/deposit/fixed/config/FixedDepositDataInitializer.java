package com.hanati.bank.bankEx.deposit.fixed.config;

import com.hanati.bank.bankEx.deposit.fixed.domain.FixedDepositProduct;
import com.hanati.bank.bankEx.deposit.fixed.enums.FixedDepositProductStatus;
import com.hanati.bank.bankEx.deposit.fixed.mapper.FixedDepositProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FixedDepositDataInitializer implements ApplicationRunner {

    private final FixedDepositProductMapper fixedDepositProductMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (!fixedDepositProductMapper.findAllOnSale().isEmpty()) return;

        fixedDepositProductMapper.insert(FixedDepositProduct.builder()
                .depositProductId("FD_BASIC_001")
                .productName("정기예금")
                .minimumAmount(1_000_000L)
                .maximumAmount(100_000_000L)
                .termMonths(12)
                .interestRate(3.50)
                .earlyTerminationRate(0.50)
                .status(FixedDepositProductStatus.ON_SALE)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
