package com.hanati.bank.bankEx.loan.mortgage.controller;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.loan.mortgage.dto.MortgageDisbursementRequest;
import com.hanati.bank.bankEx.loan.mortgage.dto.MortgageDisbursementResponse;
import com.hanati.bank.bankEx.loan.mortgage.service.mortgageDisbursementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Core/mortgage 전용 내부 API. 고객 경로(/api/bank/user)와 분리하고 공유 토큰을 확인한다.
 * 실제 운영이라면 망 분리·mTLS 같은 서버 간 인증이 필요하다.
 */
@RestController
@RequestMapping("/api/internal/mortgage")
@RequiredArgsConstructor
public class mortgageInternalController {

    private final mortgageDisbursementService disbursementService;

    @Value("${mortgage.internal-token}")
    private String internalToken;

    @PostMapping("/disbursements")
    public ResponseEntity<MortgageDisbursementResponse> disburse(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody MortgageDisbursementRequest request) {
        if (!internalToken.equals(token)) {
            throw new BusinessException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }
        return ResponseEntity.ok(disbursementService.disburse(request));
    }
}
