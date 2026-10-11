package com.hanati.bank.screening.config;

import com.hanati.bank.screening.dto.ScreeningRequest;
import com.hanati.bank.screening.enums.LoanType;
import com.hanati.bank.screening.repository.LoanScreeningRepository;
import com.hanati.bank.screening.service.LoanScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** 심사역 화면 데모용. 규칙별 결과가 하나씩 나오도록 심사 요청을 엔진에 그대로 통과시킨다 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LoanScreeningRepository screeningRepository;
    private final LoanScreeningService screeningService;

    @Override
    public void run(ApplicationArguments args) {
        if (screeningRepository.count() > 0) return;

        List.of(
                // 승인: 기본 조건 충족
                new ScreeningRequest("CUST001", "1", LoanType.GENERAL, 30_000_000L, 60_000_000L, 850, 10_000_000L),
                // 거절: 신용점수 600점 미만
                new ScreeningRequest("CUST002", "1", LoanType.GENERAL, 10_000_000L, 40_000_000L, 550, 0L),
                // 수동심사: 신청액이 연소득의 500% 초과
                new ScreeningRequest("CUST003", "JEONSE_HF_001", LoanType.JEONSE, 250_000_000L, 40_000_000L, 780, 0L),
                // 수동심사: 신용점수 600~699
                new ScreeningRequest("CUST004", "1", LoanType.GENERAL, 30_000_000L, 50_000_000L, 650, 5_000_000L),
                // 거절: 기존대출 + 신청액이 연소득의 10배 초과
                new ScreeningRequest("CUST005", "JEONSE_HF_001", LoanType.JEONSE, 100_000_000L, 30_000_000L, 820, 250_000_000L)
        ).forEach(screeningService::screen);
    }
}
