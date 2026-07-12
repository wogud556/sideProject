package com.hanati.bank.bankEx.deposit.savings.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.common.util.AccountNoGenerator;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.deposit.savings.domain.AutoTransfer;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsAccount;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsPaymentHistory;
import com.hanati.bank.bankEx.deposit.savings.domain.SavingsProduct;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsAccountResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsPaymentRequest;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsPaymentResponse;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsSubscribeRequest;
import com.hanati.bank.bankEx.deposit.savings.dto.SavingsSubscribeResponse;
import com.hanati.bank.bankEx.deposit.savings.enums.AutoTransferStatus;
import com.hanati.bank.bankEx.deposit.savings.enums.PaymentStatus;
import com.hanati.bank.bankEx.deposit.savings.enums.SavingsAccountStatus;
import com.hanati.bank.bankEx.deposit.savings.enums.SavingsProductStatus;
import com.hanati.bank.bankEx.deposit.savings.mapper.AutoTransferMapper;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsAccountMapper;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsPaymentMapper;
import com.hanati.bank.bankEx.deposit.savings.mapper.SavingsProductMapper;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class savingsAccountService {

    private static final Set<Integer> VALID_TRANSFER_DAYS = Set.of(5, 10, 15, 20, 25);

    private final SavingsProductMapper savingsProductMapper;
    private final SavingsAccountMapper savingsAccountMapper;
    private final SavingsPaymentMapper savingsPaymentMapper;
    private final AutoTransferMapper autoTransferMapper;
    private final UserInfoRepository userInfoRepository;
    private final AccountInfoRepository accountInfoRepository;
    private final transService transService;
    private final savingsInterestService savingsInterestService;

    @Transactional
    public SavingsSubscribeResponse subscribe(SavingsSubscribeRequest request) {
        validateRequest(request);

        UserInfo user = userInfoRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        if (!"ACTIVE".equals(user.getCustomerStatus())) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_ACTIVE);
        }

        AccountInfo withdrawAccount = accountInfoRepository.findByAccountNumber(request.getWithdrawAccountNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!"ACTIVE".equals(withdrawAccount.getAccountStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        if (withdrawAccount.getBalance() < request.getMonthlyAmount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        SavingsProduct product = savingsProductMapper.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_PRODUCT_NOT_FOUND));
        if (product.getStatus() != SavingsProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.SAVINGS_PRODUCT_CLOSED);
        }
        if (request.getMonthlyAmount() < product.getMinAmount() || request.getMonthlyAmount() > product.getMaxAmount()) {
            throw new BusinessException(ErrorCode.SAVINGS_AMOUNT_OUT_OF_RANGE);
        }
        if (!product.getPeriod().equals(request.getPeriod())) {
            throw new BusinessException(ErrorCode.SAVINGS_INVALID_PERIOD);
        }
        if (!savingsAccountMapper.findActiveByUserIdAndProductId(request.getUserId(), request.getProductId()).isEmpty()) {
            throw new BusinessException(ErrorCode.SAVINGS_DUPLICATED_SUBSCRIPTION);
        }

        boolean autoTransferRequested = request.getTransferDay() != null;
        if (autoTransferRequested && !VALID_TRANSFER_DAYS.contains(request.getTransferDay())) {
            throw new BusinessException(ErrorCode.SAVINGS_INVALID_TRANSFER_DAY);
        }

        double finalRate = savingsInterestService.calculateFinalRate(product.getBaseRate(), product.getMaxRate(),
                autoTransferRequested, request.isSalaryTransferYn(), request.isMobileSignupYn(), request.isLongTermYn());

        String accountNo = AccountNoGenerator.generate(no -> savingsAccountMapper.findByAccountNo(no).isPresent());
        LocalDate openDate = LocalDate.now();
        LocalDate maturityDate = openDate.plusMonths(product.getPeriod());
        LocalDateTime now = LocalDateTime.now();
        SavingsAccountStatus initialStatus = autoTransferRequested ? SavingsAccountStatus.ACTIVE : SavingsAccountStatus.OPEN;

        SavingsAccount account = SavingsAccount.builder()
                .accountNo(accountNo)
                .userId(request.getUserId())
                .productId(product.getProductId())
                .withdrawAccountNo(request.getWithdrawAccountNo())
                .monthlyAmount(request.getMonthlyAmount())
                .period(product.getPeriod())
                .currentCount(0)
                .balance(0L)
                .interestRate(finalRate)
                .status(initialStatus)
                .openDate(openDate)
                .maturityDate(maturityDate)
                .createdAt(now)
                .updatedAt(now)
                .build();
        savingsAccountMapper.insert(account);

        if (autoTransferRequested) {
            autoTransferMapper.insert(AutoTransfer.builder()
                    .accountNo(accountNo)
                    .withdrawAccount(request.getWithdrawAccountNo())
                    .transferDay(request.getTransferDay())
                    .status(AutoTransferStatus.ACTIVE)
                    .createdAt(now)
                    .build());
        }

        return new SavingsSubscribeResponse(accountNo, initialStatus.name(), finalRate, request.getMonthlyAmount(),
                product.getPeriod(), openDate, maturityDate, "적금 가입이 완료되었습니다.");
    }

    public SavingsAccountResponse getAccount(String accountNo) {
        SavingsAccount account = getAccountOrThrow(accountNo);
        SavingsProduct product = savingsProductMapper.findById(account.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_PRODUCT_NOT_FOUND));

        return new SavingsAccountResponse(
                account.getAccountNo(),
                product.getProductName(),
                account.getMonthlyAmount(),
                account.getPeriod(),
                account.getCurrentCount(),
                account.getBalance(),
                account.getInterestRate(),
                account.getStatus().name(),
                account.getOpenDate(),
                account.getMaturityDate()
        );
    }

    @Transactional
    public SavingsPaymentResponse pay(String accountNo, SavingsPaymentRequest request) {
        SavingsAccount account = getAccountOrThrow(accountNo);
        if (account.getStatus() != SavingsAccountStatus.OPEN && account.getStatus() != SavingsAccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.SAVINGS_ALREADY_TERMINATED);
        }
        if (account.getCurrentCount() >= account.getPeriod()) {
            throw new BusinessException(ErrorCode.SAVINGS_FULLY_PAID);
        }

        int nextSeq = account.getCurrentCount() + 1;
        long amount = account.getMonthlyAmount();
        String fromAccount = (request != null && request.getWithdrawAccountNo() != null)
                ? request.getWithdrawAccountNo()
                : account.getWithdrawAccountNo();

        transService.debit(fromAccount, amount, "SAVINGS_PAYMENT", "적금 납입 - " + accountNo);

        LocalDateTime now = LocalDateTime.now();
        savingsPaymentMapper.insert(SavingsPaymentHistory.builder()
                .accountNo(accountNo)
                .paymentSeq(nextSeq)
                .paymentDate(LocalDate.now())
                .paymentAmount(amount)
                .status(PaymentStatus.SUCCESS)
                .createdAt(now)
                .build());

        account.setCurrentCount(nextSeq);
        account.setBalance(account.getBalance() + amount);
        if (account.getStatus() == SavingsAccountStatus.OPEN) {
            account.setStatus(SavingsAccountStatus.ACTIVE);
        }
        account.setUpdatedAt(now);
        savingsAccountMapper.update(account);

        return new SavingsPaymentResponse(accountNo, nextSeq, amount, account.getBalance(),
                PaymentStatus.SUCCESS.name(), "납입이 완료되었습니다.");
    }

    private SavingsAccount getAccountOrThrow(String accountNo) {
        return savingsAccountMapper.findByAccountNo(accountNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.SAVINGS_ACCOUNT_NOT_FOUND));
    }

    private void validateRequest(SavingsSubscribeRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getWithdrawAccountNo() == null || request.getWithdrawAccountNo().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getProductId() == null || request.getProductId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getMonthlyAmount() == null || request.getMonthlyAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (request.getPeriod() == null || request.getPeriod() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
