package com.hanati.bank.repayment.common.exception;

import com.hanati.bank.repayment.common.response.ApiErrorResponse;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e) {
        ApiErrorResponse body = new ApiErrorResponse(e.getErrorCode().name(), e.getMessage());
        HttpStatus status = switch (e.getErrorCode()) {
            case LOAN_ACCOUNT_NOT_FOUND, REPAYMENT_TRANSACTION_NOT_FOUND, OVERPAYMENT_NOT_FOUND
                    -> HttpStatus.NOT_FOUND;
            case DUPLICATE_REPAYMENT_REQUEST, REPAYMENT_ALREADY_REVERSED,
                 REPAYMENT_CONCURRENCY_CONFLICT, FULL_REPAYMENT_AMOUNT_CHANGED,
                 OVERPAYMENT_ALREADY_REFUNDED -> HttpStatus.CONFLICT;
            case REPAYMENT_ALLOCATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(body);
    }

    /** 비관적 잠금 획득 실패 / 낙관적 잠금 충돌을 409로 환원한다 (명세 10번). */
    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class,
            OptimisticLockingFailureException.class})
    public ResponseEntity<ApiErrorResponse> handleLockFailure(Exception e) {
        ErrorCode code = ErrorCode.REPAYMENT_CONCURRENCY_CONFLICT;
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(code.name(), code.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(ErrorCode.INVALID_REQUEST.name(), detail));
    }
}
