package com.hanati.bank.bankEx.loan.jeonse;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseContractRequest;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanApplyRequest;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanApplyResponse;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanExecuteResponse;
import com.hanati.bank.bankEx.loan.jeonse.dto.JeonseLoanReviewResponse;
import com.hanati.bank.bankEx.loan.jeonse.service.jeonseLoanService;
import com.hanati.bank.bankEx.login.service.loginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class JeonseLoanFlowTests {

    private static final String PRODUCT_ID = "JEONSE_HF_001";

    @Autowired
    private jeonseLoanService jeonseLoanService;
    @Autowired
    private loginService loginService;

    private void signup(String userId) {
        loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"));
    }

    private JeonseContractRequest contract(long deposit, long downPayment, String capitalAreaYn,
                                            String fixedDateYn, LocalDate start, LocalDate end) {
        return new JeonseContractRequest("홍길동", "010-1111-2222", "인천광역시 서구 청라동", "OFFICETEL",
                capitalAreaYn, deposit, downPayment, start, end, fixedDateYn, "Y", 0L);
    }

    private JeonseLoanApplyRequest request(String userId, long requestAmount, long annualIncome, long existingDebt,
                                            int creditScore, String homelessYn, String householderYn,
                                            String guaranteeOrg, boolean salaryTransferYn, boolean cardUsageYn,
                                            JeonseContractRequest contract) {
        return new JeonseLoanApplyRequest(userId, PRODUCT_ID, requestAmount, annualIncome, existingDebt,
                creditScore, homelessYn, householderYn, guaranteeOrg, "MATURITY",
                salaryTransferYn, cardUsageYn, false, contract);
    }

    @Test
    void approvalCase_fullFlowEndsInExecuted() {
        String userId = "jeonseUser1";
        signup(userId);

        JeonseContractRequest contract = contract(130_000_000L, 10_000_000L, "Y", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 80_000_000L, 65_000_000L, 20_000_000L,
                820, "Y", "Y", "HF", true, true, contract);

        JeonseLoanApplyResponse applyResponse = jeonseLoanService.apply(applyRequest);
        assertEquals("LIMIT_CALCULATED", applyResponse.getStatus());
        assertEquals(104_000_000L, applyResponse.getAvailableLimitAmount());
        assertEquals(4.10, applyResponse.getEstimatedRate(), 0.001);

        JeonseLoanReviewResponse reviewResponse = jeonseLoanService.review(applyResponse.getApplicationId());
        assertEquals("APPROVED", reviewResponse.getStatus());
        assertEquals(80_000_000L, reviewResponse.getApprovedAmount());
        assertEquals(4.10, reviewResponse.getLoanRate(), 0.001);

        JeonseLoanExecuteResponse executeResponse = jeonseLoanService.execute(applyResponse.getApplicationId());
        assertEquals("EXECUTED", executeResponse.getStatus());
        assertEquals(80_000_000L, executeResponse.getExecutedAmount());
        assertEquals(LocalDate.now().plusMonths(1).withDayOfMonth(1), executeResponse.getFirstPaymentDate());
    }

    @Test
    void reviewBeforeLimitCalculated_throws() {
        assertThrows(BusinessException.class, () -> jeonseLoanService.review("NOT_EXIST"));
    }

    @Test
    void downPaymentShortage_isRejected() {
        String userId = "jeonseUser2";
        signup(userId);

        JeonseContractRequest contract = contract(130_000_000L, 1_000_000L, "Y", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 80_000_000L, 65_000_000L, 20_000_000L,
                820, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void capitalAreaDepositOverLimit_isRejected() {
        String userId = "jeonseUser3";
        signup(userId);

        JeonseContractRequest contract = contract(750_000_000L, 50_000_000L, "Y", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 80_000_000L, 65_000_000L, 20_000_000L,
                820, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void nonCapitalAreaDepositOverLimit_isRejected() {
        String userId = "jeonseUser4";
        signup(userId);

        JeonseContractRequest contract = contract(520_000_000L, 50_000_000L, "N", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 80_000_000L, 65_000_000L, 20_000_000L,
                820, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void lowCreditScore_isRejected() {
        String userId = "jeonseUser5";
        signup(userId);

        JeonseContractRequest contract = contract(130_000_000L, 10_000_000L, "Y", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 80_000_000L, 65_000_000L, 20_000_000L,
                599, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void highDebtRatio_isRejected() {
        String userId = "jeonseUser6";
        signup(userId);

        JeonseContractRequest contract = contract(130_000_000L, 10_000_000L, "Y", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 5_000_000L, 65_000_000L, 50_000_000L,
                820, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void requestAmountOverCalculatedLimit_isRejected() {
        String userId = "jeonseUser7";
        signup(userId);

        JeonseContractRequest contract = contract(130_000_000L, 10_000_000L, "Y", "Y",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 200_000_000L, 65_000_000L, 20_000_000L,
                820, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void noFixedDate_isHeldForReview() {
        String userId = "jeonseUser8";
        signup(userId);

        JeonseContractRequest contract = contract(130_000_000L, 10_000_000L, "Y", "N",
                LocalDate.of(2026, 8, 1), LocalDate.of(2028, 7, 31));
        JeonseLoanApplyRequest applyRequest = request(userId, 80_000_000L, 65_000_000L, 20_000_000L,
                820, "Y", "Y", "HF", false, false, contract);

        JeonseLoanApplyResponse response = jeonseLoanService.apply(applyRequest);
        assertEquals("VALIDATING", response.getStatus());
    }
}
