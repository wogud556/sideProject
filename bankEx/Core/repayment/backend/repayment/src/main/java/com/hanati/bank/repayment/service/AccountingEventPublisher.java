package com.hanati.bank.repayment.service;

import com.hanati.bank.repayment.common.util.Money;
import com.hanati.bank.repayment.entity.AccountingEvent;
import com.hanati.bank.repayment.entity.RepaymentTransaction;
import com.hanati.bank.repayment.enums.AccountingEventType;
import com.hanati.bank.repayment.enums.AllocationType;
import com.hanati.bank.repayment.policy.RepaymentAllocationResult;
import com.hanati.bank.repayment.repository.AccountingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 회계 이벤트 Outbox 적재 (명세 11번). 호출 측과 동일한 트랜잭션에서 실행되므로
 * 상환 거래가 롤백되면 이벤트도 남지 않는다. 복식부기 분개는 이번 범위에서 구현하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AccountingEventPublisher {

    private final AccountingEventRepository accountingEventRepository;

    /** 상환 완료 이벤트. sign이 -1이면 취소(역거래) 이벤트로 음수 금액을 적재한다. */
    public void publishRepayment(RepaymentTransaction transaction, RepaymentAllocationResult result, int sign) {
        List<AccountingEvent> events = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal principal = result.totalOf(AllocationType.PRINCIPAL)
                .add(result.totalOf(AllocationType.OVERDUE_PRINCIPAL));
        BigDecimal interest = result.totalOf(AllocationType.INTEREST);
        BigDecimal overdueInterest = result.totalOf(AllocationType.OVERDUE_INTEREST);
        BigDecimal overpayment = result.getOverpaymentAmount();

        add(events, transaction, AccountingEventType.LOAN_PRINCIPAL_REPAID, principal, sign, now);
        add(events, transaction, AccountingEventType.LOAN_INTEREST_RECEIVED, interest, sign, now);
        add(events, transaction, AccountingEventType.OVERDUE_INTEREST_RECEIVED, overdueInterest, sign, now);
        add(events, transaction, AccountingEventType.OVERPAYMENT_RECEIVED, overpayment, sign, now);

        if (sign < 0) {
            add(events, transaction, AccountingEventType.REPAYMENT_REVERSED,
                    transaction.getAllocatedAmount().add(transaction.getOverpaymentAmount()).abs(), 1, now);
        }
        accountingEventRepository.saveAll(events);
    }

    public void publishPaidOff(RepaymentTransaction transaction) {
        accountingEventRepository.save(event(transaction, AccountingEventType.LOAN_PAID_OFF, Money.ZERO,
                LocalDateTime.now()));
    }

    public void publishOverpaymentRefunded(RepaymentTransaction transaction, BigDecimal amount) {
        accountingEventRepository.save(event(transaction, AccountingEventType.OVERPAYMENT_REFUNDED, amount,
                LocalDateTime.now()));
    }

    private void add(List<AccountingEvent> events, RepaymentTransaction transaction,
                      AccountingEventType type, BigDecimal amount, int sign, LocalDateTime now) {
        if (!Money.isPositive(amount)) {
            return;
        }
        events.add(event(transaction, type, sign < 0 ? amount.negate() : amount, now));
    }

    private AccountingEvent event(RepaymentTransaction transaction, AccountingEventType type,
                                   BigDecimal amount, LocalDateTime now) {
        return AccountingEvent.builder()
                .eventType(type)
                .loanAccountId(transaction.getLoanAccountId())
                .repaymentTransactionId(transaction.getId())
                .amount(amount)
                .occurredAt(now)
                .published(false)
                .build();
    }
}
