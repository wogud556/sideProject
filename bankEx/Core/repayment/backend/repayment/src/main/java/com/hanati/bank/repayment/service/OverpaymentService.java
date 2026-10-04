package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.common.util.TransactionNumberGenerator;
import com.hanati.bank.repayment.dto.OverpaymentRefundResponse;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.Overpayment;
import com.hanati.bank.repayment.entity.RepaymentTransaction;
import com.hanati.bank.repayment.enums.OverpaymentStatus;
import com.hanati.bank.repayment.enums.PaymentMethod;
import com.hanati.bank.repayment.enums.RepaymentTransactionStatus;
import com.hanati.bank.repayment.enums.RepaymentTransactionType;
import com.hanati.bank.repayment.gateway.WithdrawalGateway;
import com.hanati.bank.repayment.gateway.dto.RefundRequest;
import com.hanati.bank.repayment.gateway.dto.RefundResult;
import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.repository.LoanAccountRepository;
import com.hanati.bank.repayment.repository.OverpaymentRepository;
import com.hanati.bank.repayment.repository.RepaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 과오납 환급 (명세 3.8 / 8.6).
 *
 * <p>환급 입금은 외부 계좌 시스템 몫이라 {@link WithdrawalGateway}를 통해 Mock으로 처리한다.
 * 게이트웨이가 실패하면 과오납을 REFUND_FAILED로 남겨 재시도할 수 있게 하고, 환급 거래는
 * FAILED 상태로 기록한다 — 실패도 원장에 남긴다.
 */
@Service
@RequiredArgsConstructor
public class OverpaymentService {

    private final OverpaymentRepository overpaymentRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentTransactionRepository transactionRepository;
    private final WithdrawalGateway withdrawalGateway;
    private final AccountingEventPublisher accountingEventPublisher;

    @Transactional(readOnly = true)
    public List<Overpayment> getOverpayments(Long loanAccountId) {
        requireAccount(loanAccountId);
        return overpaymentRepository.findByLoanAccountIdOrderByIdDesc(loanAccountId);
    }

    @Transactional
    public OverpaymentRefundResponse refund(Long loanAccountId, Long overpaymentId) {
        LoanAccount account = requireAccount(loanAccountId);

        Overpayment overpayment = overpaymentRepository.findById(overpaymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OVERPAYMENT_NOT_FOUND));
        if (!overpayment.getLoanAccountId().equals(loanAccountId)) {
            throw new BusinessException(ErrorCode.OVERPAYMENT_NOT_FOUND,
                    "과오납이 대출계좌 " + loanAccountId + "에 속하지 않습니다");
        }
        if (overpayment.getStatus() == OverpaymentStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.OVERPAYMENT_ALREADY_REFUNDED);
        }
        if (overpayment.getStatus() == OverpaymentStatus.REFUND_CANCELLED) {
            throw new BusinessException(ErrorCode.OVERPAYMENT_NOT_FOUND, "취소된 과오납입니다");
        }

        LocalDate businessDate = LocalDate.now();
        RefundResult result = withdrawalGateway.refund(new RefundRequest(
                account.getWithdrawalAccountNumber(), overpayment.getAmount(),
                "과오납 환급 - " + account.getLoanAccountNumber()));

        RepaymentTransaction refundTxn = transactionRepository.save(RepaymentTransaction.builder()
                .transactionNumber(TransactionNumberGenerator.generate(businessDate,
                        transactionRepository::existsByTransactionNumber))
                .loanAccountId(loanAccountId)
                .transactionType(RepaymentTransactionType.OVERPAYMENT_REFUND)
                .paymentMethod(PaymentMethod.INTERNAL)
                .requestedAmount(overpayment.getAmount().negate())
                .allocatedAmount(Money.ZERO)
                .overpaymentAmount(overpayment.getAmount().negate())
                .prepaymentFee(Money.ZERO)
                .status(result.success() ? RepaymentTransactionStatus.COMPLETED : RepaymentTransactionStatus.FAILED)
                .businessDate(businessDate)
                .processedAt(LocalDateTime.now())
                .idempotencyKey("refund-" + overpaymentId + "-" + System.nanoTime())
                .accountStatusBefore(account.getStatus())
                .reason("과오납 환급")
                .build());

        if (result.success()) {
            overpayment.setStatus(OverpaymentStatus.REFUNDED);
            overpayment.setRefundedAt(LocalDateTime.now());
            overpayment.setRefundFailReason(null);
            accountingEventPublisher.publishOverpaymentRefunded(refundTxn, overpayment.getAmount());
        } else {
            overpayment.setStatus(OverpaymentStatus.REFUND_FAILED);
            overpayment.setRefundFailReason(result.message());
        }
        overpaymentRepository.save(overpayment);

        return new OverpaymentRefundResponse(
                overpayment.getId(),
                loanAccountId,
                overpayment.getAmount(),
                overpayment.getStatus().name(),
                result.success() ? refundTxn.getTransactionNumber() : null,
                result.message()
        );
    }

    private LoanAccount requireAccount(Long loanAccountId) {
        return loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND));
    }
}
