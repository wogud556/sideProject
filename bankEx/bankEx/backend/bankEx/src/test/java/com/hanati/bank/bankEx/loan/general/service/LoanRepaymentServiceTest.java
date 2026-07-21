package com.hanati.bank.bankEx.loan.general.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.loan.general.domain.LoanApplication;
import com.hanati.bank.bankEx.loan.general.domain.LoanProduct;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepayPreviewResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentDetailResponse;
import com.hanati.bank.bankEx.loan.general.mapper.LoanApplicationMapper;
import com.hanati.bank.bankEx.loan.general.mapper.LoanProductMapper;
import com.hanati.bank.bankEx.loan.general.mapper.LoanRepaymentHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentServiceTest {

    @Mock
    private LoanApplicationMapper loanApplicationMapper;
    @Mock
    private LoanProductMapper loanProductMapper;
    @Mock
    private LoanRepaymentHistoryMapper historyMapper;
    @Mock
    private transService transService;

    @InjectMocks
    private LoanRepaymentService loanRepaymentService;

    // ---------- 공통 헬퍼 ----------

    private LoanApplication application(Long id, String status, Long remainingBalance,
                                        Long requestAmount, int loanPeriod, String repaymentType) {
        return LoanApplication.builder()
                .applicationId(id)
                .userId("user01")
                .accountNumber("123-456-789")
                .productId(1L)
                .requestAmount(requestAmount)
                .loanPeriod(loanPeriod)
                .status(status)
                .remainingBalance(remainingBalance)
                .repaymentType(repaymentType)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private LoanProduct product(double interestRate) {
        return LoanProduct.builder()
                .productId(1L)
                .productName("직장인 신용대출")
                .interestRate(interestRate)
                .maxLimit(50_000_000L)
                .description("설명")
                .build();
    }

    // =========================================================
    // EQUAL_PRINCIPAL 케이스
    // =========================================================

    @Test
    void repay_equalPrincipal_firstPayment_principalInterestTotalCorrect() {
        // 원금 10,000,000 / 12 = 833,333, 이자 10,000,000 * 4.8/1200 = 40,000
        // 총납부 = 873,333
        LoanApplication app = application(1001L, "실행완료", 10_000_000L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(1001L)).thenReturn(0); // 1회차

        LoanRepaymentDetailResponse response = loanRepaymentService.repay(1001L);

        assertEquals(833_333L, response.getPrincipalAmount());
        assertEquals(40_000L, response.getInterestAmount());
        assertEquals(873_333L, response.getPaymentAmount());
        assertEquals(9_166_667L, response.getRemainingPrincipal());
        assertEquals("실행완료", response.getStatus());
    }

    @Test
    void repay_equalPrincipal_firstPayment_remainingBalanceUpdated() {
        // 상환 후 잔여원금 = 10,000,000 - 833,333 = 9,166,667
        LoanApplication app = application(1001L, "실행완료", 10_000_000L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(1001L)).thenReturn(0);

        loanRepaymentService.repay(1001L);

        assertEquals(9_166_667L, app.getRemainingBalance());
    }

    @Test
    void repay_equalPrincipal_lastPayment_statusChangesToCompleted() {
        // 마지막 회차: paymentSeq = 12 >= loanPeriod = 12 → 상환완료
        // 잔여원금이 마지막 회차에는 실제 잔액 전액(833,337 가정: 반올림 오차 누적)으로 처리되어야 함
        // 여기서는 잔여원금 833_337을 마지막 잔액으로 설정
        LoanApplication app = application(1001L, "실행완료", 833_337L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(1001L)).thenReturn(11); // 12회차(마지막)

        LoanRepaymentDetailResponse response = loanRepaymentService.repay(1001L);

        assertEquals("상환완료", response.getStatus());
        assertEquals(0L, response.getRemainingPrincipal());
    }

    // =========================================================
    // BULLET 케이스
    // =========================================================

    @Test
    void repay_bullet_middlePayment_principalZeroInterestOnly() {
        // BULLET 1회차: 원금 0, 이자 40,000, 총납부 40,000
        LoanApplication app = application(2001L, "실행완료", 10_000_000L, 10_000_000L, 3, "BULLET");
        when(loanApplicationMapper.findById(2001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(2001L)).thenReturn(0); // 1회차

        LoanRepaymentDetailResponse response = loanRepaymentService.repay(2001L);

        assertEquals(0L, response.getPrincipalAmount());
        assertEquals(40_000L, response.getInterestAmount());
        assertEquals(40_000L, response.getPaymentAmount());
    }

    @Test
    void repay_bullet_lastPayment_principalPlusInterest() {
        // BULLET 마지막 회차: 원금 10,000,000 + 이자 40,000 = 10,040,000
        LoanApplication app = application(2001L, "실행완료", 10_000_000L, 10_000_000L, 3, "BULLET");
        when(loanApplicationMapper.findById(2001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(2001L)).thenReturn(2); // 3회차(마지막)

        LoanRepaymentDetailResponse response = loanRepaymentService.repay(2001L);

        assertEquals(10_000_000L, response.getPrincipalAmount());
        assertEquals(40_000L, response.getInterestAmount());
        assertEquals(10_040_000L, response.getPaymentAmount());
        assertEquals("상환완료", response.getStatus());
    }

    // =========================================================
    // 예외 케이스
    // =========================================================

    @Test
    void repay_loanNotFound_throwsLoanApplicationNotFound() {
        when(loanApplicationMapper.findById(9999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanRepaymentService.repay(9999L));

        assertEquals(ErrorCode.LOAN_APPLICATION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void repay_alreadyCompleted_remainingBalanceZero_throwsLoanAlreadyCompleted() {
        // remainingBalance = 0 이면 이미 상환완료
        LoanApplication app = application(1001L, "실행완료", 0L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanRepaymentService.repay(1001L));

        assertEquals(ErrorCode.LOAN_ALREADY_COMPLETED, ex.getErrorCode());
    }

    @Test
    void repay_notApprovedStatus_throwsLoanNotApproved() {
        // status가 "실행완료"가 아닌 경우 (예: "심사중")
        LoanApplication app = application(1001L, "심사중", 10_000_000L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanRepaymentService.repay(1001L));

        assertEquals(ErrorCode.LOAN_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void repay_nullRepaymentType_throwsLoanInvalidRepaymentType() {
        // repaymentType이 null이면 resolveType에서 예외 발생 (historyMapper는 호출되지 않음)
        LoanApplication app = application(1001L, "실행완료", 10_000_000L, 10_000_000L, 12, null);
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanRepaymentService.repay(1001L));

        assertEquals(ErrorCode.LOAN_INVALID_REPAYMENT_TYPE, ex.getErrorCode());
    }

    // =========================================================
    // preview 케이스
    // =========================================================

    @Test
    void preview_doesNotCallTransServiceDebit() {
        // preview는 DB 변경 없이 예상 납부액만 반환 → transService.debit 호출 금지
        LoanApplication app = application(1001L, "실행완료", 10_000_000L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(1001L)).thenReturn(0);

        LoanRepayPreviewResponse response = loanRepaymentService.preview(1001L);

        verify(transService, never()).debit(any(), any(), any(), any());
        assertEquals(833_333L, response.getExpectedPrincipal());
        assertEquals(40_000L, response.getExpectedInterest());
        assertEquals(873_333L, response.getExpectedTotal());
    }

    @Test
    void preview_returnsCorrectRemainingPrincipalWithoutModifying() {
        // preview 호출 후 잔여원금이 변경되지 않아야 함 (update 미호출)
        LoanApplication app = application(1001L, "실행완료", 10_000_000L, 10_000_000L, 12, "EQUAL_PRINCIPAL");
        when(loanApplicationMapper.findById(1001L)).thenReturn(Optional.of(app));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(4.8)));
        when(historyMapper.countByApplicationId(1001L)).thenReturn(0);

        LoanRepayPreviewResponse response = loanRepaymentService.preview(1001L);

        verify(loanApplicationMapper, never()).update(any());
        assertEquals(10_000_000L, response.getRemainingPrincipal());
        assertEquals("EQUAL_PRINCIPAL", response.getRepaymentType());
    }
}
