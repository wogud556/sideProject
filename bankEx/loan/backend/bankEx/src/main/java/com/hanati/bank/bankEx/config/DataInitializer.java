package com.hanati.bank.bankEx.config;

import com.hanati.bank.bankEx.entity.LoanProduct;
import com.hanati.bank.bankEx.repository.LoanProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LoanProductRepository loanProductRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (loanProductRepository.count() > 0) return;

        loanProductRepository.save(LoanProduct.builder()
                .productName("신용대출")
                .interestRate(4.5)
                .maxLimit(100_000_000L)
                .description("별도 담보 없이 신용도를 기반으로 대출받을 수 있습니다. 재직증명서 및 소득증빙 서류가 필요합니다.")
                .build());

        loanProductRepository.save(LoanProduct.builder()
                .productName("전세대출")
                .interestRate(3.2)
                .maxLimit(500_000_000L)
                .description("전세 계약을 담보로 저금리 대출을 받을 수 있습니다. 전세계약서 및 등기부등본이 필요합니다.")
                .build());

        loanProductRepository.save(LoanProduct.builder()
                .productName("비상금대출")
                .interestRate(6.9)
                .maxLimit(3_000_000L)
                .description("갑작스러운 비용이 필요할 때 즉시 대출받을 수 있는 소액 대출 상품입니다.")
                .build());
    }
}
