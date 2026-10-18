package com.hanati.bank.bankEx.loan.mortgage.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.common.util.TransactionNoGenerator;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.entity.DepositTransaction;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.repository.DepositTransactionRepository;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreClient;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos.*;
import com.hanati.bank.bankEx.loan.mortgage.dto.*;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 주담대 고객 채널. 신청·진행 조회·약정·철회·상환을 Core/mortgage에 전달한다.
 * bankEx가 직접 확인하는 것은 회원 여부와 수신계좌 소유·잔액뿐이고, 업무 규칙은 Core가 판단한다.
 */
@Service
@RequiredArgsConstructor
public class mortgageLoanService {

    static final String REPAYMENT_TRANSACTION_TYPE = "MORTGAGE_REPAYMENT";

    private static final Map<String, String> NOTICE = Map.ofEntries(
            Map.entry("APPLIED", "신청이 접수되었습니다. 담당자가 담보 평가를 진행합니다."),
            Map.entry("COLLATERAL_EVALUATED", "담보 평가가 끝나 대출 한도를 산정하고 있습니다."),
            Map.entry("LIMIT_CALCULATED", "여신 심사를 진행하고 있습니다."),
            Map.entry("DOCUMENT_REQUIRED", "소득 증빙 서류 확인이 필요합니다. 영업점에 서류를 제출해 주세요."),
            Map.entry("SCREENING_APPROVED", "심사를 통과했습니다. 최종 승인을 기다리고 있습니다."),
            Map.entry("CONDITIONALLY_APPROVED", "조건부로 심사를 통과했습니다. 최종 승인을 기다리고 있습니다."),
            Map.entry("REJECTED", "심사 결과 대출이 어렵습니다. 자세한 내용은 영업점에 문의해 주세요."),
            Map.entry("APPROVED", "대출이 승인되었습니다. 승인 조건을 확인하고 약정을 진행해 주세요."),
            Map.entry("AGREED", "약정이 완료되었습니다. 담보권 설정이 확인되면 대출이 실행됩니다."),
            Map.entry("EXECUTION_READY", "실행 준비가 끝났습니다. 곧 대출금이 입금됩니다."),
            Map.entry("EXECUTED", "대출금이 입금되었습니다."),
            Map.entry("WITHDRAWN", "신청을 철회했습니다."));

    private final MortgageCoreClient coreClient;
    private final UserInfoRepository userInfoRepository;
    private final AccountInfoRepository accountInfoRepository;
    private final DepositTransactionRepository depositTransactionRepository;
    private final transService transService;

    public List<Product> getProducts() {
        return coreClient.getProducts();
    }

    public MortgageApplicationResponse apply(MortgageApplyRequest request) {
        UserInfo user = userInfoRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        ApplicationDetail detail = coreClient.apply(new ApplyCommand(user.getUserId(), user.getUserName(),
                request.productCode(), request.requestedAmount(), request.termMonths(), request.repaymentMethod(),
                request.loanPurpose(), request.houseCount(), request.firstHomeBuyer(), request.property(),
                request.income(), request.debts() == null ? List.of() : request.debts()));
        return toResponse(detail);
    }

    /** 목록은 요약만 돌려준다. 승인·약정 내용은 상세 조회에서 본다 */
    public List<MortgageApplicationResponse> getApplications(String userId) {
        return coreClient.getApplications(userId).stream()
                .map(a -> new MortgageApplicationResponse(a.applicationId(), a.applicationNo(), a.productCode(),
                        a.requestedAmount(), null, null, a.status(), a.statusLabel(),
                        NOTICE.getOrDefault(a.status(), a.statusLabel()), a.appliedAt(), null, null, null, null))
                .toList();
    }

    public MortgageApplicationResponse getApplication(Long applicationId, String userId) {
        return toResponse(ownedApplication(applicationId, userId));
    }

    /** 약정. 실행금을 받을 계좌가 본인의 정상 계좌인지 여기서 확인한 뒤 Core에 넘긴다. */
    public MortgageApplicationResponse agree(Long applicationId, MortgageAgreementRequest request) {
        ownedApplication(applicationId, request.userId());
        AccountInfo account = ownedActiveAccount(request.disbursementAccountNumber(), request.userId());
        return toResponse(coreClient.agree(applicationId, request.userId(), account.getAccountNumber(),
                Boolean.TRUE.equals(request.consent())));
    }

    public MortgageApplicationResponse withdraw(Long applicationId, String userId) {
        ownedApplication(applicationId, userId);
        return toResponse(coreClient.withdraw(applicationId, userId));
    }

    public List<LoanAccount> getLoanAccounts(String userId) {
        return coreClient.getLoanAccounts(userId);
    }

    public LoanAccount getLoanAccount(Long loanAccountId, String userId) {
        return ownedLoanAccount(loanAccountId, userId);
    }

    public List<Installment> getSchedule(Long loanAccountId, String userId) {
        ownedLoanAccount(loanAccountId, userId);
        return coreClient.getSchedule(loanAccountId);
    }

    /**
     * 다음 회차 상환. 출금과 Core 상환 반영을 한 로컬 트랜잭션으로 묶는다.
     * Core가 거절하거나 연결이 끊기면 예외가 나서 출금도 롤백된다.
     * 출금 거래번호를 Core의 멱등 키로 넘기므로 Core 쪽에서 같은 출금이 두 번 반영되지 않는다.
     */
    @Transactional
    public MortgageRepayResponse repay(Long loanAccountId, MortgageRepayRequest request) {
        LoanAccount loan = ownedLoanAccount(loanAccountId, request.userId());
        Installment due = loan.nextInstallment();
        if (due == null) {
            throw new BusinessException(ErrorCode.MORTGAGE_FULLY_REPAID);
        }

        AccountInfo source = accountInfoRepository.findByAccountNumberForUpdate(request.sourceAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!source.getUserId().equals(request.userId())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OWNED);
        }
        if (!"ACTIVE".equals(source.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        String transactionNumber = TransactionNoGenerator.generate(depositTransactionRepository::existsByTransactionNumber);
        DepositTransaction debit = transService.debitLocked(source, due.totalPayment(), REPAYMENT_TRANSACTION_TYPE,
                "주택담보대출 " + loan.account().loanAccountNo() + " " + due.installmentNo() + "회차 상환",
                transactionNumber, null, LocalDateTime.now());

        Repayment repayment = coreClient.repay(loanAccountId, request.userId(), due.totalPayment(),
                source.getAccountNumber(), debit.getTransactionNumber());
        return new MortgageRepayResponse(repayment.installmentNo(), repayment.totalPaid(), repayment.principalPaid(),
                repayment.interestPaid(), repayment.outstandingAfter(), source.getAccountNumber(),
                debit.getBalanceAfter(), debit.getTransactionNumber());
    }

    private ApplicationDetail ownedApplication(Long applicationId, String userId) {
        ApplicationDetail detail = coreClient.getApplication(applicationId);
        if (!detail.application().customerId().equals(userId)) {
            throw new BusinessException(ErrorCode.MORTGAGE_NOT_OWNED);
        }
        return detail;
    }

    private LoanAccount ownedLoanAccount(Long loanAccountId, String userId) {
        LoanAccount loan = coreClient.getLoanAccount(loanAccountId);
        if (!loan.account().customerId().equals(userId)) {
            throw new BusinessException(ErrorCode.MORTGAGE_NOT_OWNED);
        }
        return loan;
    }

    private AccountInfo ownedActiveAccount(String accountNumber, String userId) {
        AccountInfo account = accountInfoRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_OWNED);
        }
        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return account;
    }

    private MortgageApplicationResponse toResponse(ApplicationDetail d) {
        ApplicationSummary a = d.application();
        return new MortgageApplicationResponse(a.applicationId(), a.applicationNo(), a.productCode(),
                a.requestedAmount(), d.termMonths(), d.repaymentMethod(), a.status(), a.statusLabel(),
                NOTICE.getOrDefault(a.status(), a.statusLabel()), a.appliedAt(),
                d.property() == null ? null : d.property().address(), d.approval(), d.agreement(), d.loanAccountId());
    }
}
