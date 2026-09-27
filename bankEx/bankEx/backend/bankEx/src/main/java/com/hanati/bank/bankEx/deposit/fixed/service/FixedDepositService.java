package com.hanati.bank.bankEx.deposit.fixed.service;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.common.util.AccountNoGenerator;
import com.hanati.bank.bankEx.deposit.fixed.domain.FixedDepositAccount;
import com.hanati.bank.bankEx.deposit.fixed.domain.FixedDepositProduct;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositAccountResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositMaturityPreviewResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositProductResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositSubscribeRequest;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositSubscribeResponse;
import com.hanati.bank.bankEx.deposit.fixed.dto.FixedDepositTerminateResponse;
import com.hanati.bank.bankEx.deposit.fixed.enums.FixedDepositProductStatus;
import com.hanati.bank.bankEx.deposit.fixed.enums.FixedDepositStatus;
import com.hanati.bank.bankEx.deposit.fixed.mapper.FixedDepositAccountMapper;
import com.hanati.bank.bankEx.deposit.fixed.mapper.FixedDepositProductMapper;
import com.hanati.bank.bankEx.deposit.general.entity.AccountInfo;
import com.hanati.bank.bankEx.deposit.general.repository.AccountInfoRepository;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.entity.UserInfo;
import com.hanati.bank.bankEx.login.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedDepositService {

    private final FixedDepositProductMapper fixedDepositProductMapper;
    private final FixedDepositAccountMapper fixedDepositAccountMapper;
    private final UserInfoRepository userInfoRepository;
    private final AccountInfoRepository accountInfoRepository;
    private final transService transService;
    private final FixedDepositInterestCalculator fixedDepositInterestCalculator;

    public List<FixedDepositProductResponse> getProducts() {
        return fixedDepositProductMapper.findAllOnSale().stream()
                .map(product -> new FixedDepositProductResponse(
                        product.getDepositProductId(),
                        product.getProductName(),
                        product.getMinimumAmount(),
                        product.getMaximumAmount(),
                        product.getTermMonths(),
                        product.getInterestRate(),
                        product.getEarlyTerminationRate(),
                        product.getStatus().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public FixedDepositSubscribeResponse subscribe(FixedDepositSubscribeRequest request) {
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
        if (withdrawAccount.getBalance() < request.getPrincipalAmount()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        FixedDepositProduct product = fixedDepositProductMapper.findById(request.getDepositProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_DEPOSIT_PRODUCT_NOT_FOUND));
        if (product.getStatus() != FixedDepositProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.FIXED_DEPOSIT_PRODUCT_CLOSED);
        }
        if (request.getPrincipalAmount() < product.getMinimumAmount()
                || request.getPrincipalAmount() > product.getMaximumAmount()) {
            throw new BusinessException(ErrorCode.FIXED_DEPOSIT_AMOUNT_OUT_OF_RANGE);
        }
        if (!product.getTermMonths().equals(request.getTermMonths())) {
            throw new BusinessException(ErrorCode.FIXED_DEPOSIT_INVALID_TERM);
        }

        long principal = request.getPrincipalAmount();
        long expectedInterest = fixedDepositInterestCalculator.calculateInterest(
                principal, product.getInterestRate(), product.getTermMonths());
        long expectedTax = fixedDepositInterestCalculator.calculateTax(expectedInterest);
        long expectedMaturityAmount = principal + expectedInterest - expectedTax;

        transService.debit(request.getWithdrawAccountNo(), principal,
                "FIXED_DEPOSIT_SUBSCRIBE", "정기예금 예치");

        String depositAccountId = AccountNoGenerator.generate(
                id -> fixedDepositAccountMapper.findByDepositAccountId(id).isPresent());
        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(product.getTermMonths());
        LocalDateTime now = LocalDateTime.now();

        FixedDepositAccount account = FixedDepositAccount.builder()
                .depositAccountId(depositAccountId)
                .userId(request.getUserId())
                .depositProductId(product.getDepositProductId())
                .withdrawAccountNo(request.getWithdrawAccountNo())
                .principalAmount(principal)
                .interestRate(product.getInterestRate())
                .startDate(startDate)
                .maturityDate(maturityDate)
                .expectedInterest(expectedInterest)
                .expectedMaturityAmount(expectedMaturityAmount)
                .status(FixedDepositStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        fixedDepositAccountMapper.insert(account);

        return new FixedDepositSubscribeResponse(depositAccountId, FixedDepositStatus.ACTIVE.name(),
                principal, product.getInterestRate(), product.getTermMonths(), startDate, maturityDate,
                expectedInterest, expectedMaturityAmount, "정기예금 가입이 완료되었습니다.");
    }

    public FixedDepositAccountResponse getAccount(String depositAccountId) {
        FixedDepositAccount account = getAccountOrThrow(depositAccountId);
        FixedDepositProduct product = fixedDepositProductMapper.findById(account.getDepositProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_DEPOSIT_PRODUCT_NOT_FOUND));

        return new FixedDepositAccountResponse(
                account.getDepositAccountId(),
                product.getProductName(),
                account.getPrincipalAmount(),
                account.getInterestRate(),
                account.getStartDate(),
                account.getMaturityDate(),
                account.getExpectedInterest(),
                account.getExpectedMaturityAmount(),
                account.getStatus().name()
        );
    }

    public FixedDepositMaturityPreviewResponse getMaturityPreview(String depositAccountId) {
        FixedDepositAccount account = getAccountOrThrow(depositAccountId);
        long tax = fixedDepositInterestCalculator.calculateTax(account.getExpectedInterest());

        return new FixedDepositMaturityPreviewResponse(
                account.getDepositAccountId(),
                account.getPrincipalAmount(),
                account.getInterestRate(),
                account.getMaturityDate(),
                account.getExpectedInterest(),
                tax,
                account.getExpectedMaturityAmount()
        );
    }

    /**
     * 만기일 도달 전 중도해지. 만기일이 지난 계좌는 만기 배치가 처리한다.
     * 경과 개월 수에 따라 이자가 달라지므로, 만기 배치와 같이 기준일을 인자로 받는다.
     */
    @Transactional
    public FixedDepositTerminateResponse terminate(String depositAccountId, LocalDate today) {
        FixedDepositAccount account = getAccountOrThrow(depositAccountId);
        if (account.getStatus() != FixedDepositStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FIXED_DEPOSIT_ALREADY_TERMINATED);
        }

        if (!today.isBefore(account.getMaturityDate())) {
            throw new BusinessException(ErrorCode.FIXED_DEPOSIT_MATURITY_REACHED);
        }

        FixedDepositProduct product = fixedDepositProductMapper.findById(account.getDepositProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_DEPOSIT_PRODUCT_NOT_FOUND));

        int elapsedMonths = fixedDepositInterestCalculator.elapsedMonths(account.getStartDate(), today);
        long interest = fixedDepositInterestCalculator.calculateInterest(
                account.getPrincipalAmount(), product.getEarlyTerminationRate(), elapsedMonths);
        long tax = fixedDepositInterestCalculator.calculateTax(interest);
        long payout = account.getPrincipalAmount() + interest - tax;

        transService.credit(account.getWithdrawAccountNo(), payout,
                "FIXED_DEPOSIT_TERMINATE", "정기예금 중도해지 원리금 지급 - " + depositAccountId);

        account.setStatus(FixedDepositStatus.TERMINATED);
        account.setUpdatedAt(LocalDateTime.now());
        fixedDepositAccountMapper.update(account);

        return new FixedDepositTerminateResponse(depositAccountId, FixedDepositStatus.TERMINATED.name(),
                payout, interest, tax, "중도해지가 완료되었습니다.");
    }

    private FixedDepositAccount getAccountOrThrow(String depositAccountId) {
        return fixedDepositAccountMapper.findByDepositAccountId(depositAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_DEPOSIT_ACCOUNT_NOT_FOUND));
    }

    private void validateRequest(FixedDepositSubscribeRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getDepositProductId() == null || request.getDepositProductId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getWithdrawAccountNo() == null || request.getWithdrawAccountNo().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getPrincipalAmount() == null || request.getPrincipalAmount() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        if (request.getTermMonths() == null || request.getTermMonths() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
