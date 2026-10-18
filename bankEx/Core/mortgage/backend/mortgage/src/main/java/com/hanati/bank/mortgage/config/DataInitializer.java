package com.hanati.bank.mortgage.config;

import com.hanati.bank.mortgage.application.dto.ApplicationCreateRequest;
import com.hanati.bank.mortgage.application.enums.*;
import com.hanati.bank.mortgage.application.service.MortgageApplicationService;
import com.hanati.bank.mortgage.collateral.dto.CollateralEvaluationRequest;
import com.hanati.bank.mortgage.collateral.enums.LienType;
import com.hanati.bank.mortgage.collateral.enums.ValuationMethod;
import com.hanati.bank.mortgage.collateral.service.CollateralService;
import com.hanati.bank.mortgage.limit.service.LimitCalculationRequest;
import com.hanati.bank.mortgage.limit.service.MortgageLimitService;
import com.hanati.bank.mortgage.operator.Operator;
import com.hanati.bank.mortgage.operator.OperatorRepository;
import com.hanati.bank.mortgage.operator.OperatorRole;
import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.enums.PolicyType;
import com.hanati.bank.mortgage.policy.repository.MortgagePolicyRepository;
import com.hanati.bank.mortgage.product.MortgageProduct;
import com.hanati.bank.mortgage.product.MortgageProductRepository;
import com.hanati.bank.mortgage.screening.service.MortgageScreeningService;
import com.hanati.bank.mortgage.screening.service.ScreeningRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.hanati.bank.mortgage.policy.entity.MortgagePolicy.ALL;

/**
 * 기준 데이터(담당자·상품·규제 정책)와 담당자 화면 시연용 신청 3건을 넣는다.
 * 정책 값은 학습용 예시이며 실제 감독규정 수치와 다를 수 있다.
 * 시연 신청은 bankEx에 없는 고객(demo01~03)이라 약정·실행까지는 가지 않는다.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final LocalDate POLICY_START = LocalDate.of(2024, 1, 1);
    private static final LocalDate STRESS_V2_START = LocalDate.of(2026, 7, 1);

    private final OperatorRepository operatorRepository;
    private final MortgageProductRepository productRepository;
    private final MortgagePolicyRepository policyRepository;
    private final MortgageApplicationService applicationService;
    private final CollateralService collateralService;
    private final MortgageLimitService limitService;
    private final MortgageScreeningService screeningService;

    @Override
    public void run(ApplicationArguments args) {
        if (operatorRepository.count() > 0) return;

        operatorRepository.saveAll(List.of(
                Operator.builder().operatorId("OPS01").operatorName("김실무").role(OperatorRole.OPERATIONS).build(),
                Operator.builder().operatorId("UW01").operatorName("이심사").role(OperatorRole.UNDERWRITER).build(),
                Operator.builder().operatorId("AP01").operatorName("박승인").role(OperatorRole.APPROVER).build()));

        productRepository.saveAll(List.of(
                MortgageProduct.builder().productCode("MG-STD").productName("하나 주택담보대출")
                        .minAmount(10_000_000L).maxAmount(1_000_000_000L)
                        .minTermMonths(120).maxTermMonths(480).baseRate(new BigDecimal("3.80"))
                        .description("주택 구입·생활안정 자금. 10~40년, 원리금균등·원금균등·만기일시 상환").build(),
                MortgageProduct.builder().productCode("MG-FIRST").productName("하나 첫집 주택담보대출")
                        .minAmount(10_000_000L).maxAmount(400_000_000L)
                        .minTermMonths(120).maxTermMonths(480).baseRate(new BigDecimal("3.30"))
                        .description("생애최초 주택 구입자 우대금리 상품. 최대 4억 원").build()));

        policyRepository.saveAll(List.of(
                policy(PolicyType.LTV, ALL, ALL, "70", "기본 LTV"),
                policy(PolicyType.LTV, "REGULATED", ALL, "50", "규제지역 LTV"),
                policy(PolicyType.LTV, "REGULATED", "MULTI_HOME", "0", "규제지역 다주택자 주담대 금지"),
                policy(PolicyType.LTV, ALL, "FIRST_HOME", "80", "생애최초 구입자 LTV 완화"),
                policy(PolicyType.DTI, ALL, ALL, "60", "기본 DTI"),
                policy(PolicyType.DTI, "REGULATED", ALL, "40", "규제지역 DTI"),
                policy(PolicyType.DSR, ALL, ALL, "40", "차주단위 DSR"),
                MortgagePolicy.builder().policyType(PolicyType.STRESS_RATE).regionCode(ALL).borrowerType(ALL)
                        .productCode(ALL).policyValue(new BigDecimal("0.75")).effectiveFrom(POLICY_START)
                        .effectiveTo(STRESS_V2_START.minusDays(1)).policyVersion(1)
                        .basis("스트레스 DSR 1단계").createdBy("SYSTEM").createdAt(LocalDateTime.now()).build(),
                MortgagePolicy.builder().policyType(PolicyType.STRESS_RATE).regionCode(ALL).borrowerType(ALL)
                        .productCode(ALL).policyValue(new BigDecimal("1.50")).effectiveFrom(STRESS_V2_START)
                        .policyVersion(2).basis("스트레스 DSR 3단계").createdBy("SYSTEM")
                        .createdAt(LocalDateTime.now()).build(),
                policy(PolicyType.MAX_LOAN_AMOUNT, ALL, ALL, "1000000000", "기본 최대 금액"),
                policy(PolicyType.MAX_LOAN_AMOUNT, "REGULATED", ALL, "600000000", "규제지역 주담대 최대 6억")));

        seedDemoApplications();
    }

    private void seedDemoApplications() {
        LocalDate today = LocalDate.now();

        // 1) 신청 접수 — 담보 평가 대기
        applicationService.apply(demo("demo01", "최하나", 300_000_000L, RegionCode.NON_REGULATED, 500_000_000L,
                65_000_000L, 0, true), today);

        // 2) 한도 산정 완료 — 심사 대기 (규제지역, 1주택자, 선순위 근저당 있음)
        Long second = applicationService.apply(demo("demo02", "정두리", 400_000_000L, RegionCode.REGULATED,
                900_000_000L, 120_000_000L, 1, false), today).getApplicationId();
        collateralService.evaluate(second, "OPS01", new CollateralEvaluationRequest(ValuationMethod.MARKET_PRICE,
                880_000_000L, List.of(new CollateralEvaluationRequest.SeniorClaim(
                        LienType.SENIOR_MORTGAGE, "국민은행", 60_000_000L, 1))), today);
        limitService.calculate(second, "UW01", new LimitCalculationRequest(820), today);

        // 3) 조건부 심사 승인 — 승인 대기 (소유권 서류 보완 조건)
        Long third = applicationService.apply(demo("demo03", "한세움", 250_000_000L, RegionCode.NON_REGULATED,
                450_000_000L, 70_000_000L, 0, false), today).getApplicationId();
        collateralService.evaluate(third, "OPS01", new CollateralEvaluationRequest(ValuationMethod.APPRAISAL,
                440_000_000L, List.of()), today);
        limitService.calculate(third, "UW01", new LimitCalculationRequest(760), today);
        screeningService.screen(third, "UW01", new ScreeningRequest(true, false, "등기권리증 사본 추후 제출 예정"));
    }

    private static ApplicationCreateRequest demo(String customerId, String name, long amount, RegionCode region,
                                                 long price, long income, int houseCount, boolean firstHome) {
        return new ApplicationCreateRequest(customerId, name, "MG-STD", amount, 360,
                RepaymentMethod.EQUAL_INSTALLMENT, LoanPurpose.PURCHASE, houseCount, firstHome,
                new ApplicationCreateRequest.Property("서울특별시 예시구 예시로 " + customerId.substring(4),
                        region, PropertyType.APARTMENT, new BigDecimal("84.95"), price),
                new ApplicationCreateRequest.Income(IncomeType.EMPLOYEE, income, "하나테크"),
                List.of(new ApplicationCreateRequest.Debt(DebtType.CREDIT, "하나카드", 20_000_000L,
                        4_000_000L, 1_000_000L)));
    }

    private static MortgagePolicy policy(PolicyType type, String region, String borrower, String value, String basis) {
        return MortgagePolicy.builder()
                .policyType(type).regionCode(region).borrowerType(borrower).productCode(ALL)
                .policyValue(new BigDecimal(value)).effectiveFrom(POLICY_START).policyVersion(1)
                .basis(basis).createdBy("SYSTEM").createdAt(LocalDateTime.now())
                .build();
    }
}
