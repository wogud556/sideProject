package com.hanati.bank.repayment.controller;

import com.hanati.bank.repayment.common.exception.BusinessException;
import com.hanati.bank.repayment.common.exception.ErrorCode;
import com.hanati.bank.repayment.entity.LoanAccount;
import com.hanati.bank.repayment.entity.RepaymentSchedule;
import com.hanati.bank.repayment.repository.LoanAccountRepository;
import com.hanati.bank.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 대출계좌·스케줄 조회. 명세 8번에 없는 보조 API이며, 상환 결과를 확인하고 잔액 정합성을
 * 점검하기 위한 읽기 전용 엔드포인트다. 계좌 생성 API는 제공하지 않는다
 * (대출 실행은 이 코어의 책임이 아니므로 시드 데이터로만 적재한다).
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanAccountController {

    private final LoanAccountRepository loanAccountRepository;
    private final RepaymentScheduleRepository scheduleRepository;

    @GetMapping
    public ResponseEntity<List<LoanAccount>> getAccounts() {
        return ResponseEntity.ok(loanAccountRepository.findAll());
    }

    @GetMapping("/{loanAccountId}")
    public ResponseEntity<LoanAccount> getAccount(@PathVariable Long loanAccountId) {
        return ResponseEntity.ok(loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND)));
    }

    @GetMapping("/{loanAccountId}/schedules")
    public ResponseEntity<List<RepaymentSchedule>> getSchedules(@PathVariable Long loanAccountId) {
        if (!loanAccountRepository.existsById(loanAccountId)) {
            throw new BusinessException(ErrorCode.LOAN_ACCOUNT_NOT_FOUND);
        }
        return ResponseEntity.ok(scheduleRepository
                .findByLoanAccountIdOrderByDueDateAscInstallmentNumberAscIdAsc(loanAccountId));
    }
}
