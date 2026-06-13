package com.hanati.bank.screening.config;

import com.hanati.bank.screening.entity.CustomerCreditInfo;
import com.hanati.bank.screening.entity.LoanProduct;
import com.hanati.bank.screening.entity.UserInfo;
import com.hanati.bank.screening.repository.CustomerCreditInfoRepository;
import com.hanati.bank.screening.repository.LoanProductRepository;
import com.hanati.bank.screening.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserInfoRepository userInfoRepository;
    private final CustomerCreditInfoRepository creditInfoRepository;
    private final LoanProductRepository loanProductRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (userInfoRepository.count() > 0) return;

        UserInfo user = UserInfo.builder()
                .userId("test01")
                .password(passwordEncoder.encode("1234"))
                .userName("홍길동")
                .birthDate(LocalDate.of(1990, 5, 15))
                .phoneNumber("010-1234-5678")
                .build();
        userInfoRepository.save(user);

        CustomerCreditInfo credit = CustomerCreditInfo.builder()
                .userId("test01")
                .creditScore(820)
                .annualIncome(65_000_000L)
                .employmentType("정규직")
                .companyName("한아티은행")
                .employmentMonths(48)
                .existingDebt(20_000_000L)
                .annualRepayment(10_000_000L)
                .build();
        creditInfoRepository.save(credit);
    }

    private void seedProducts() {
        if (loanProductRepository.count() > 0) return;

        loanProductRepository.saveAll(List.of(
                LoanProduct.builder()
                        .productName("KB 직장인 신용대출")
                        .productType("신용대출")
                        .minInterestRate(new BigDecimal("3.50"))
                        .maxInterestRate(new BigDecimal("7.00"))
                        .maxLimitAmount(100_000_000L)
                        .loanPeriodMonths(60)
                        .build(),
                LoanProduct.builder()
                        .productName("KB 전세자금대출")
                        .productType("전세대출")
                        .minInterestRate(new BigDecimal("2.80"))
                        .maxInterestRate(new BigDecimal("5.50"))
                        .maxLimitAmount(300_000_000L)
                        .loanPeriodMonths(24)
                        .build(),
                LoanProduct.builder()
                        .productName("KB 소액 비상금대출")
                        .productType("신용대출")
                        .minInterestRate(new BigDecimal("5.00"))
                        .maxInterestRate(new BigDecimal("9.00"))
                        .maxLimitAmount(3_000_000L)
                        .loanPeriodMonths(12)
                        .build()
        ));
    }
}
