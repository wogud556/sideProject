package com.hanati.bank.refinance.common.exception;

import com.hanati.bank.refinance.common.response.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e) {
        ApiErrorResponse body = new ApiErrorResponse(e.getErrorCode().name(), e.getMessage());
        HttpStatus status = switch (e.getErrorCode()) {
            case FORBIDDEN_ROLE -> HttpStatus.FORBIDDEN;
            case CONCURRENT_MODIFICATION, DUPLICATE_TRANSACTION -> HttpStatus.CONFLICT;
            case CUSTOMER_NOT_FOUND, LOAN_NOT_FOUND, APPLICATION_NOT_FOUND,
                 OPERATOR_NOT_FOUND, ERROR_RECORD_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(body);
    }
}
