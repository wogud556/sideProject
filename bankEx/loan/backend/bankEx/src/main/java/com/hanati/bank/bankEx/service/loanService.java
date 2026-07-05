package com.hanati.bank.bankEx.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.dto.LoanAccountValidateResponse;
import com.hanati.bank.bankEx.dto.LoanApplicationRequest;
import com.hanati.bank.bankEx.dto.LoanApplicationResponse;
import com.hanati.bank.bankEx.dto.LoanDisbursementRequest;
import com.hanati.bank.bankEx.dto.LoanDisbursementResponse;
import com.hanati.bank.bankEx.dto.LoanProductResponse;
import com.hanati.bank.bankEx.dto.LoanRepaymentRequest;
import com.hanati.bank.bankEx.dto.LoanRepaymentResponse;
import com.hanati.bank.bankEx.entity.AccountInfo;
import com.hanati.bank.bankEx.entity.LoanApplication;
import com.hanati.bank.bankEx.entity.LoanProduct;
import com.hanati.bank.bankEx.entity.UserInfo;
import com.hanati.bank.bankEx.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.repository.LoanApplicationRepository;
import com.hanati.bank.bankEx.repository.LoanProductRepository;
import com.hanati.bank.bankEx.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class loanService {

    private final LoanProductRepository loanProductRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final UserInfoRepository userInfoRepository;
    private final AccountInfoRepository accountInfoRepository;
    private final transService transService;

    public List<LoanProductResponse> getProducts() {
        return loanProductRepository.findAll().stream()
                .map(p -> new LoanProductResponse(
                        p.getProductId(),
                        p.getProductName(),
                        p.getInterestRate(),
                        p.getMaxLimit(),
                        p.getDescription()
                ))
                .collect(Collectors.toList());
    }

    public LoanProductResponse getProduct(Long productId) {
        LoanProduct product = loanProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));
        return new LoanProductResponse(
                product.getProductId(),
                product.getProductName(),
                product.getInterestRate(),
                product.getMaxLimit(),
                product.getDescription()
        );
    }

    @Transactional
    public LoanApplicationResponse applyLoan(LoanApplicationRequest request) {
        LoanProduct product = loanProductRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다"));

        if (request.getRequestAmount() > product.getMaxLimit()) {
            throw new IllegalArgumentException("신청 금액이 한도를 초과합니다");
        }

        LoanApplication application = LoanApplication.builder()
                .userId(request.getUserId())
                .accountNumber(request.getAccountNumber())
                .productId(request.getProductId())
                .requestAmount(request.getRequestAmount())
                .loanPeriod(request.getLoanPeriod())
                .status("심사중")
                .createdAt(LocalDateTime.now())
                .build();
        LoanApplication saved = loanApplicationRepository.save(application);

        return new LoanApplicationResponse(
                saved.getApplicationId(),
                product.getProductName(),
                saved.getRequestAmount(),
                saved.getLoanPeriod(),
                saved.getStatus(),
                saved.getRemainingBalance(),
                saved.getCreatedAt().toString()
        );
    }

    public List<LoanApplicationResponse> getMyApplications(String userId) {
        return loanApplicationRepository.findByUserId(userId).stream()
                .map(a -> {
                    String productName = loanProductRepository.findById(a.getProductId())
                            .map(LoanProduct::getProductName)
                            .orElse("알 수 없음");
                    return new LoanApplicationResponse(
                            a.getApplicationId(),
                            productName,
                            a.getRequestAmount(),
                            a.getLoanPeriod(),
                            a.getStatus(),
                            a.getRemainingBalance(),
                            a.getCreatedAt().toString()
                    );
                })
                .collect(Collectors.toList());
    }

    public LoanAccountValidateResponse validateCustomerAccount(String userId, String accountNumber) {
        UserInfo user = userInfoRepository.findById(userId).orElse(null);
        if (user == null) {
            return new LoanAccountValidateResponse(false, userId, accountNumber, "고객을 찾을 수 없습니다.");
        }
        if (!"ACTIVE".equals(user.getCustomerStatus())) {
            return new LoanAccountValidateResponse(false, userId, accountNumber, "정상 상태의 고객이 아닙니다.");
        }

        AccountInfo account = accountInfoRepository.findByAccountNumber(accountNumber).orElse(null);
        if (account == null) {
            return new LoanAccountValidateResponse(false, userId, accountNumber, "계좌를 찾을 수 없습니다.");
        }
        if (!account.getUserId().equals(userId)) {
            return new LoanAccountValidateResponse(false, userId, accountNumber, "계좌 소유자가 일치하지 않습니다.");
        }
        if (!"ACTIVE".equals(account.getAccountStatus())) {
            return new LoanAccountValidateResponse(false, userId, accountNumber, "정상 상태의 계좌가 아닙니다.");
        }

        return new LoanAccountValidateResponse(true, userId, accountNumber, "대출 신청 가능한 고객 및 계좌입니다.");
    }

    @Transactional
    public LoanApplicationResponse approveApplication(Long applicationId) {
        LoanApplication application = getApplication(applicationId);
        application.setStatus("승인");
        loanApplicationRepository.save(application);

        String productName = loanProductRepository.findById(application.getProductId())
                .map(LoanProduct::getProductName)
                .orElse("알 수 없음");

        return new LoanApplicationResponse(
                application.getApplicationId(),
                productName,
                application.getRequestAmount(),
                application.getLoanPeriod(),
                application.getStatus(),
                application.getRemainingBalance(),
                application.getCreatedAt().toString()
        );
    }

    @Transactional
    public LoanDisbursementResponse disburse(LoanDisbursementRequest request) {
        LoanApplication application = getApplication(request.getApplicationId());
        if (!"승인".equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.LOAN_NOT_APPROVED);
        }

        validateActiveCustomerAndAccount(application);

        transService.credit(application.getAccountNumber(), application.getRequestAmount(),
                "LOAN_DISBURSEMENT", "대출 실행금 입금");

        application.setRemainingBalance(application.getRequestAmount());
        application.setStatus("실행완료");
        loanApplicationRepository.save(application);

        return new LoanDisbursementResponse(
                application.getApplicationId(),
                application.getUserId(),
                application.getAccountNumber(),
                application.getRequestAmount(),
                "대출 실행금 입금이 완료되었습니다."
        );
    }

    @Transactional
    public LoanRepaymentResponse repay(LoanRepaymentRequest request) {
        LoanApplication application = getApplication(request.getApplicationId());
        Long remainingBalance = application.getRemainingBalance() == null ? 0L : application.getRemainingBalance();

        if (request.getRepaymentAmount() == null || request.getRepaymentAmount() <= 0
                || request.getRepaymentAmount() > remainingBalance) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        validateActiveCustomerAndAccount(application);

        transService.debit(application.getAccountNumber(), request.getRepaymentAmount(),
                "LOAN_REPAYMENT", "대출 상환 출금");

        long newRemainingBalance = remainingBalance - request.getRepaymentAmount();
        application.setRemainingBalance(newRemainingBalance);
        loanApplicationRepository.save(application);

        return new LoanRepaymentResponse(
                application.getApplicationId(),
                application.getUserId(),
                application.getAccountNumber(),
                request.getRepaymentAmount(),
                newRemainingBalance,
                "대출 상환이 완료되었습니다."
        );
    }

    private LoanApplication getApplication(Long applicationId) {
        return loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_APPLICATION_NOT_FOUND));
    }

    private void validateActiveCustomerAndAccount(LoanApplication application) {
        UserInfo user = userInfoRepository.findById(application.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        if (!"ACTIVE".equals(user.getCustomerStatus())) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_ACTIVE);
        }

        AccountInfo account = accountInfoRepository.findByAccountNumber(application.getAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.getUserId().equals(application.getUserId())) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_MISMATCH);
        }
        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }
}
