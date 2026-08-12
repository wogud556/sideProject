package com.hanati.bank.refinance.config;

import com.hanati.bank.refinance.customer.entity.Customer;
import com.hanati.bank.refinance.customer.repository.CustomerRepository;
import com.hanati.bank.refinance.gateway.DemoScenarioAccounts;
import com.hanati.bank.refinance.loan.entity.ExistingLoan;
import com.hanati.bank.refinance.loan.repository.ExistingLoanRepository;
import com.hanati.bank.refinance.operator.entity.Operator;
import com.hanati.bank.refinance.operator.enums.OperatorRole;
import com.hanati.bank.refinance.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 명세 33번 "테스트 데이터" 요구사항에 맞춘 Mock 데이터.
 * 정상/연체/잔액없음/정상대환/신규대출실행실패/신규성공+상환실패(재처리) 시나리오를 각각 구분되는 고객으로 시딩한다.
 */
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final ExistingLoanRepository existingLoanRepository;
    private final OperatorRepository operatorRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedOperators();
        seedCustomersAndLoans();
    }

    private void seedOperators() {
        if (operatorRepository.count() > 0) return;
        operatorRepository.saveAll(java.util.List.of(
                Operator.builder().operatorId("teller01").name("창구직원").role(OperatorRole.ROLE_TELLER).build(),
                Operator.builder().operatorId("reviewer01").name("심사역").role(OperatorRole.ROLE_REVIEWER).build(),
                Operator.builder().operatorId("approver01").name("승인권자").role(OperatorRole.ROLE_APPROVER).build(),
                Operator.builder().operatorId("operator01").name("실행운영자").role(OperatorRole.ROLE_OPERATOR).build(),
                Operator.builder().operatorId("admin01").name("관리자").role(OperatorRole.ROLE_ADMIN).build()
        ));
    }

    private void seedCustomersAndLoans() {
        if (customerRepository.count() > 0) return;

        Customer normal = save("C000001", "홍정상", LocalDate.of(1985, 3, 12), "010-1111-2222");
        Customer overdue = save("C000002", "김연체", LocalDate.of(1979, 7, 1), "010-2222-3333");
        Customer noBalance = save("C000003", "이완납", LocalDate.of(1990, 11, 20), "010-3333-4444");
        Customer refinanceSuccess = save("C000004", "박정상대환", LocalDate.of(1988, 5, 5), "010-4444-5555");
        Customer executionFail = save("C000005", "최실행실패", LocalDate.of(1992, 1, 15), "010-5555-6666");
        Customer repaymentFail = save("C000006", "정재처리", LocalDate.of(1983, 9, 9), "010-6666-7777");

        // 정상 고객: 대환 가능한 대출 보유
        loan(normal, "004", "국민은행", "111-1111-1111", "KB 신용대출", "신용대출",
                new BigDecimal("30000000"), new BigDecimal("18000000"), new BigDecimal("6.20"),
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(2), "원리금균등분할상환", "N");

        // 연체 고객: 대환 불가
        loan(overdue, "088", "신한은행", "222-2222-2222", "신한 마이카대출", "신용대출",
                new BigDecimal("20000000"), new BigDecimal("15000000"), new BigDecimal("7.80"),
                LocalDate.now().minusMonths(8), LocalDate.now().plusYears(1), "원리금균등분할상환", "Y");

        // 대출잔액 없음: 대환 불가
        loan(noBalance, "020", "우리은행", "333-3333-3333", "우리 직장인대출", "신용대출",
                new BigDecimal("10000000"), BigDecimal.ZERO, new BigDecimal("5.90"),
                LocalDate.now().minusYears(2), LocalDate.now().minusMonths(1), "원리금균등분할상환", "N");

        // 정상 대환 데모: 신규대출 실행 및 기존대출 상환 모두 성공
        loan(refinanceSuccess, "011", "농협은행", "444-4444-4444", "NH 급여대출", "신용대출",
                new BigDecimal("25000000"), new BigDecimal("20000000"), new BigDecimal("6.50"),
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(3), "원리금균등분할상환", "N");

        // 신규대출 실행 실패 데모
        loan(executionFail, "003", "기업은행", DemoScenarioAccounts.EXECUTION_FAILURE_ACCOUNT, "IBK 직장인대출", "신용대출",
                new BigDecimal("15000000"), new BigDecimal("12000000"), new BigDecimal("6.90"),
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(2), "원리금균등분할상환", "N");

        // 신규대출 성공 + 기존대출 상환 실패(재처리 대상) 데모
        loan(repaymentFail, "081", "하나은행", DemoScenarioAccounts.REPAYMENT_FAILURE_ACCOUNT, "하나 신용대출", "신용대출",
                new BigDecimal("40000000"), new BigDecimal("28000000"), new BigDecimal("6.10"),
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(3), "원리금균등분할상환", "N");
    }

    private Customer save(String customerNo, String name, LocalDate birthDate, String phone) {
        return customerRepository.save(Customer.builder()
                .customerNo(customerNo)
                .name(name)
                .birthDate(birthDate)
                .phone(phone)
                .status("ACTIVE")
                .build());
    }

    private void loan(Customer customer, String institutionCode, String institutionName, String accountNo,
                       String productName, String loanType, BigDecimal originalAmount, BigDecimal currentBalance,
                       BigDecimal interestRate, LocalDate executionDate, LocalDate maturityDate,
                       String repaymentMethod, String overdueYn) {
        existingLoanRepository.save(ExistingLoan.builder()
                .customerId(customer.getCustomerId())
                .financialInstitutionCode(institutionCode)
                .financialInstitutionName(institutionName)
                .loanAccountNo(accountNo)
                .loanProductCode(institutionCode + "-P01")
                .loanProductName(productName)
                .loanType(loanType)
                .originalAmount(originalAmount)
                .currentBalance(currentBalance)
                .interestRate(interestRate)
                .executionDate(executionDate)
                .maturityDate(maturityDate)
                .repaymentMethod(repaymentMethod)
                .overdueYn(overdueYn)
                .status("ACTIVE")
                .build());
    }
}
