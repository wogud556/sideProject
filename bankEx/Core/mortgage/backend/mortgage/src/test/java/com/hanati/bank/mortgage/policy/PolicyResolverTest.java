package com.hanati.bank.mortgage.policy;

import com.hanati.bank.mortgage.common.exception.BusinessException;
import com.hanati.bank.mortgage.common.exception.ErrorCode;
import com.hanati.bank.mortgage.policy.dto.PolicyCreateRequest;
import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.enums.PolicyType;
import com.hanati.bank.mortgage.policy.repository.MortgagePolicyRepository;
import com.hanati.bank.mortgage.policy.service.PolicyResolver;
import com.hanati.bank.mortgage.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** DataInitializer가 넣은 정책을 기준으로 검증한다 */
@SpringBootTest
@Transactional
class PolicyResolverTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 17);

    @Autowired PolicyResolver resolver;
    @Autowired PolicyService policyService;
    @Autowired MortgagePolicyRepository policyRepository;

    private BigDecimal ltv(String region, String borrower) {
        return resolver.resolve(PolicyType.LTV, region, borrower, "MG-STD", DAY).getPolicyValue();
    }

    @Test
    @DisplayName("지역·차주 유형별로 가장 구체적인 LTV 정책을 고른다")
    void specificity() {
        assertThat(ltv("NON_REGULATED", "ONE_HOME")).isEqualByComparingTo("70");
        assertThat(ltv("REGULATED", "ONE_HOME")).isEqualByComparingTo("50");
        assertThat(ltv("REGULATED", "MULTI_HOME")).isEqualByComparingTo("0");
        // 차주 유형(가중치 2)이 지역(1)보다 우선
        assertThat(ltv("REGULATED", "FIRST_HOME")).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("기준일에 따라 스트레스 가산금리 버전이 달라진다")
    void effectiveDate() {
        MortgagePolicy v1 = resolver.resolve(PolicyType.STRESS_RATE, "REGULATED", "NO_HOME", "MG-STD",
                LocalDate.of(2026, 6, 30));
        MortgagePolicy v2 = resolver.resolve(PolicyType.STRESS_RATE, "REGULATED", "NO_HOME", "MG-STD",
                LocalDate.of(2026, 7, 1));

        assertThat(v1.getPolicyValue()).isEqualByComparingTo("0.75");
        assertThat(v1.getPolicyVersion()).isEqualTo(1);
        assertThat(v2.getPolicyValue()).isEqualByComparingTo("1.50");
        assertThat(v2.getPolicyVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("기준일에 유효한 정책이 없으면 POLICY_NOT_FOUND")
    void notFound() {
        assertThatThrownBy(() -> resolver.resolve(PolicyType.LTV, "REGULATED", "NO_HOME", "MG-STD",
                LocalDate.of(2023, 12, 31)))
                .extracting("errorCode").isEqualTo(ErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    @DisplayName("새 버전을 등록하면 이전 버전은 전날로 닫히고, 이전 기준일 결과는 그대로다")
    void newVersion() {
        MortgagePolicy created = policyService.create("AP01", new PolicyCreateRequest(PolicyType.LTV, "ALL", "ALL",
                "ALL", new BigDecimal("65"), LocalDate.of(2027, 1, 1), null, "LTV 하향"));

        assertThat(created.getPolicyVersion()).isEqualTo(2);
        assertThat(resolver.resolve(PolicyType.LTV, "NON_REGULATED", "ONE_HOME", "MG-STD", LocalDate.of(2026, 12, 31))
                .getPolicyValue()).isEqualByComparingTo("70");
        assertThat(resolver.resolve(PolicyType.LTV, "NON_REGULATED", "ONE_HOME", "MG-STD", LocalDate.of(2027, 1, 1))
                .getPolicyValue()).isEqualByComparingTo("65");
        assertThat(policyRepository.findByPolicyType(PolicyType.LTV).stream()
                .filter(p -> p.getPolicyVersion() == 1 && p.getRegionCode().equals("ALL") && p.getBorrowerType().equals("ALL"))
                .findFirst().orElseThrow().getEffectiveTo()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("정책 등록은 승인권자만 할 수 있다")
    void onlyApprover() {
        assertThatThrownBy(() -> policyService.create("UW01", new PolicyCreateRequest(PolicyType.DSR, "ALL", "ALL",
                "ALL", new BigDecimal("35"), DAY, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OPERATOR_NOT_AUTHORIZED);
    }
}
