package com.hanati.bank.bankEx.loan.general.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.loan.general.domain.LoanApplication;
import com.hanati.bank.bankEx.loan.general.domain.LoanProduct;
import com.hanati.bank.bankEx.loan.general.dto.LoanAccountValidateResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanApplicationResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanDisbursementResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanProductResponse;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentRequest;
import com.hanati.bank.bankEx.loan.general.dto.LoanRepaymentResponse;
import com.hanati.bank.bankEx.loan.general.mapper.LoanApplicationMapper;
import com.hanati.bank.bankEx.loan.general.mapper.LoanProductMapper;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * loanService(일반 대출)에 대한 순수 Mockito 단위 테스트 (Spring context, DB 불필요).
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanProductMapper loanProductMapper;
    @Mock
    private LoanApplicationMapper loanApplicationMapper;
    @Mock
    private UserInfoRepository userInfoRepository;
    @Mock
    private AccountInfoRepository accountInfoRepository;
    @Mock
    private transService transService;

    @InjectMocks
    private loanService loanService;

    private LoanProduct product(long productId, long maxLimit) {
        return LoanProduct.builder()
                .productId(productId)
                .productName("직장인 신용대출")
                .interestRate(4.5)
                .maxLimit(maxLimit)
                .description("설명")
                .build();
    }

    private LoanApplication application(Long applicationId, String status, Long remainingBalance) {
        return LoanApplication.builder()
                .applicationId(applicationId)
                .userId("user1")
                .accountNumber("111-222-33")
                .productId(1L)
                .requestAmount(1_000_000L)
                .loanPeriod(12)
                .status(status)
                .remainingBalance(remainingBalance)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserInfo activeUser() {
        return UserInfo.builder().userId("user1").customerStatus("ACTIVE").userName("홍길동").build();
    }

    private AccountInfo activeAccount() {
        return AccountInfo.builder()
                .userId("user1")
                .accountNumber("111-222-33")
                .accountStatus("ACTIVE")
                .balance(0L)
                .build();
    }

    // ---------- getProducts / getProduct ----------

    @Test
    void getProducts_returnsMappedList() {
        when(loanProductMapper.findAll()).thenReturn(List.of(product(1L, 10_000_000L)));

        List<LoanProductResponse> result = loanService.getProducts();

        assertEquals(1, result.size());
        assertEquals("직장인 신용대출", result.get(0).getProductName());
    }

    @Test
    void getProduct_success() {
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(1L, 10_000_000L)));

        LoanProductResponse result = loanService.getProduct(1L);

        assertEquals(1L, result.getProductId());
        assertEquals(10_000_000L, result.getMaxLimit());
    }

    @Test
    void getProduct_notFound_throwsIllegalArgumentException() {
        when(loanProductMapper.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> loanService.getProduct(99L));
    }

    // ---------- applyLoan ----------

    @Test
    void applyLoan_success_savesApplicationWithReviewingStatus() {
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(1L, 10_000_000L)));
        LoanApplicationRequest request = new LoanApplicationRequest("user1", "111-222-33", 1L, 5_000_000L, 12);

        LoanApplicationResponse response = loanService.applyLoan(request);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationMapper).insert(captor.capture());
        assertEquals("심사중", captor.getValue().getStatus());
        assertEquals(5_000_000L, captor.getValue().getRequestAmount());
        assertEquals("심사중", response.getStatus());
        assertEquals("직장인 신용대출", response.getProductName());
    }

    @Test
    void applyLoan_productNotFound_throwsIllegalArgumentException() {
        when(loanProductMapper.findById(99L)).thenReturn(Optional.empty());
        LoanApplicationRequest request = new LoanApplicationRequest("user1", "111-222-33", 99L, 5_000_000L, 12);

        assertThrows(IllegalArgumentException.class, () -> loanService.applyLoan(request));
        verify(loanApplicationMapper, never()).insert(any());
    }

    @Test
    void applyLoan_amountExceedsMaxLimit_throwsIllegalArgumentException() {
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(1L, 10_000_000L)));
        LoanApplicationRequest request = new LoanApplicationRequest("user1", "111-222-33", 1L, 10_000_001L, 12);

        assertThrows(IllegalArgumentException.class, () -> loanService.applyLoan(request));
        verify(loanApplicationMapper, never()).insert(any());
    }

    // ---------- getMyApplications ----------

    @Test
    void getMyApplications_returnsMappedListWithProductName() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "심사중", null)));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(1L, 10_000_000L)));

        List<LoanApplicationResponse> result = loanService.getMyApplications("user1");

        assertEquals(1, result.size());
        assertEquals("직장인 신용대출", result.get(0).getProductName());
    }

    @Test
    void getMyApplications_unknownProduct_fallsBackToDefaultName() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "심사중", null)));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.empty());

        List<LoanApplicationResponse> result = loanService.getMyApplications("user1");

        assertEquals("알 수 없음", result.get(0).getProductName());
    }

    // ---------- hasActiveLoan ----------

    @Test
    void hasActiveLoan_reviewingStatus_returnsTrue() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "심사중", null)));

        assertTrue(loanService.hasActiveLoan("user1", "111-222-33"));
    }

    @Test
    void hasActiveLoan_approvedStatus_returnsTrue() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "승인", null)));

        assertTrue(loanService.hasActiveLoan("user1", "111-222-33"));
    }

    @Test
    void hasActiveLoan_disbursedWithRemainingBalance_returnsTrue() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "실행완료", 500_000L)));

        assertTrue(loanService.hasActiveLoan("user1", "111-222-33"));
    }

    @Test
    void hasActiveLoan_disbursedWithZeroRemainingBalance_returnsFalse() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "실행완료", 0L)));

        assertFalse(loanService.hasActiveLoan("user1", "111-222-33"));
    }

    @Test
    void hasActiveLoan_differentAccountNumber_returnsFalse() {
        when(loanApplicationMapper.findByUserId("user1")).thenReturn(List.of(application(1L, "심사중", null)));

        assertFalse(loanService.hasActiveLoan("user1", "다른계좌"));
    }

    // ---------- validateCustomerAccount ----------

    @Test
    void validateCustomerAccount_success_returnsValidTrue() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(activeAccount()));

        LoanAccountValidateResponse response = loanService.validateCustomerAccount("user1", "111-222-33");

        assertTrue(response.isValid());
    }

    @Test
    void validateCustomerAccount_userNotFound_returnsInvalidWithMessage() {
        when(userInfoRepository.findById("noUser")).thenReturn(Optional.empty());

        LoanAccountValidateResponse response = loanService.validateCustomerAccount("noUser", "111-222-33");

        assertFalse(response.isValid());
        assertEquals("고객을 찾을 수 없습니다.", response.getMessage());
    }

    @Test
    void validateCustomerAccount_userNotActive_returnsInvalidWithMessage() {
        UserInfo inactiveUser = UserInfo.builder().userId("user1").customerStatus("INACTIVE").build();
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(inactiveUser));

        LoanAccountValidateResponse response = loanService.validateCustomerAccount("user1", "111-222-33");

        assertFalse(response.isValid());
        assertEquals("정상 상태의 고객이 아닙니다.", response.getMessage());
    }

    @Test
    void validateCustomerAccount_accountNotFound_returnsInvalidWithMessage() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        when(accountInfoRepository.findByAccountNumber("없는계좌")).thenReturn(Optional.empty());

        LoanAccountValidateResponse response = loanService.validateCustomerAccount("user1", "없는계좌");

        assertFalse(response.isValid());
        assertEquals("계좌를 찾을 수 없습니다.", response.getMessage());
    }

    @Test
    void validateCustomerAccount_accountOwnerMismatch_returnsInvalidWithMessage() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        AccountInfo othersAccount = AccountInfo.builder()
                .userId("otherUser").accountNumber("111-222-33").accountStatus("ACTIVE").build();
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(othersAccount));

        LoanAccountValidateResponse response = loanService.validateCustomerAccount("user1", "111-222-33");

        assertFalse(response.isValid());
        assertEquals("계좌 소유자가 일치하지 않습니다.", response.getMessage());
    }

    @Test
    void validateCustomerAccount_accountNotActive_returnsInvalidWithMessage() {
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        AccountInfo closedAccount = AccountInfo.builder()
                .userId("user1").accountNumber("111-222-33").accountStatus("CLOSED").build();
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(closedAccount));

        LoanAccountValidateResponse response = loanService.validateCustomerAccount("user1", "111-222-33");

        assertFalse(response.isValid());
        assertEquals("정상 상태의 계좌가 아닙니다.", response.getMessage());
    }

    // ---------- approveApplication ----------

    @Test
    void approveApplication_success_changesStatusToApproved() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "심사중", null)));
        when(loanProductMapper.findById(1L)).thenReturn(Optional.of(product(1L, 10_000_000L)));

        LoanApplicationResponse response = loanService.approveApplication(1L);

        assertEquals("승인", response.getStatus());
        verify(loanApplicationMapper).update(any(LoanApplication.class));
    }

    @Test
    void approveApplication_notFound_throwsBusinessException() {
        when(loanApplicationMapper.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> loanService.approveApplication(99L));

        assertEquals(ErrorCode.LOAN_APPLICATION_NOT_FOUND, ex.getErrorCode());
    }

    // ---------- disburse ----------

    @Test
    void disburse_success_creditsAccountAndSetsExecutedStatus() {
        LoanApplication approved = application(1L, "승인", null);
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(approved));
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(activeAccount()));

        LoanDisbursementResponse response = loanService.disburse(new LoanDisbursementRequest(1L));

        verify(transService).credit("111-222-33", 1_000_000L, "LOAN_DISBURSEMENT", "대출 실행금 입금");
        assertEquals("실행완료", approved.getStatus());
        assertEquals(1_000_000L, approved.getRemainingBalance());
        assertEquals(1_000_000L, response.getLoanAmount());
    }

    @Test
    void disburse_notApproved_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "심사중", null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(1L)));

        assertEquals(ErrorCode.LOAN_NOT_APPROVED, ex.getErrorCode());
        verify(transService, never()).credit(any(), any(), any(), any());
    }

    @Test
    void disburse_applicationNotFound_throwsBusinessException() {
        when(loanApplicationMapper.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(99L)));

        assertEquals(ErrorCode.LOAN_APPLICATION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void disburse_customerNotFound_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "승인", null)));
        when(userInfoRepository.findById("user1")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(1L)));

        assertEquals(ErrorCode.CUSTOMER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void disburse_customerNotActive_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "승인", null)));
        UserInfo inactiveUser = UserInfo.builder().userId("user1").customerStatus("INACTIVE").build();
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(inactiveUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(1L)));

        assertEquals(ErrorCode.CUSTOMER_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void disburse_accountNotFound_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "승인", null)));
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(1L)));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void disburse_accountOwnerMismatch_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "승인", null)));
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        AccountInfo othersAccount = AccountInfo.builder()
                .userId("otherUser").accountNumber("111-222-33").accountStatus("ACTIVE").build();
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(othersAccount));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(1L)));

        assertEquals(ErrorCode.LOAN_ACCOUNT_MISMATCH, ex.getErrorCode());
    }

    @Test
    void disburse_accountNotActive_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "승인", null)));
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        AccountInfo closedAccount = AccountInfo.builder()
                .userId("user1").accountNumber("111-222-33").accountStatus("CLOSED").build();
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(closedAccount));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.disburse(new LoanDisbursementRequest(1L)));

        assertEquals(ErrorCode.ACCOUNT_NOT_ACTIVE, ex.getErrorCode());
    }

    // ---------- repay ----------

    @Test
    void repay_success_debitsAccountAndReducesRemainingBalance() {
        LoanApplication disbursed = application(1L, "실행완료", 1_000_000L);
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(disbursed));
        when(userInfoRepository.findById("user1")).thenReturn(Optional.of(activeUser()));
        when(accountInfoRepository.findByAccountNumber("111-222-33")).thenReturn(Optional.of(activeAccount()));

        LoanRepaymentResponse response = loanService.repay(new LoanRepaymentRequest(1L, 300_000L));

        verify(transService).debit("111-222-33", 300_000L, "LOAN_REPAYMENT", "대출 상환 출금");
        assertEquals(700_000L, disbursed.getRemainingBalance());
        assertEquals(700_000L, response.getRemainingBalance());
    }

    @Test
    void repay_nullAmount_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "실행완료", 1_000_000L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.repay(new LoanRepaymentRequest(1L, null)));

        assertEquals(ErrorCode.INVALID_AMOUNT, ex.getErrorCode());
    }

    @Test
    void repay_zeroAmount_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "실행완료", 1_000_000L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.repay(new LoanRepaymentRequest(1L, 0L)));

        assertEquals(ErrorCode.INVALID_AMOUNT, ex.getErrorCode());
    }

    @Test
    void repay_amountExceedsRemainingBalance_throwsBusinessException() {
        when(loanApplicationMapper.findById(1L)).thenReturn(Optional.of(application(1L, "실행완료", 500_000L)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.repay(new LoanRepaymentRequest(1L, 600_000L)));

        assertEquals(ErrorCode.INVALID_AMOUNT, ex.getErrorCode());
        verify(transService, never()).debit(any(), any(), any(), any());
    }

    @Test
    void repay_applicationNotFound_throwsBusinessException() {
        when(loanApplicationMapper.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.repay(new LoanRepaymentRequest(99L, 100_000L)));

        assertEquals(ErrorCode.LOAN_APPLICATION_NOT_FOUND, ex.getErrorCode());
    }
}
