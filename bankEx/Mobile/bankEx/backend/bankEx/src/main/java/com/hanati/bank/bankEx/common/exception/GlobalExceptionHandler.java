package com.hanati.bank.bankEx.common.exception;

import com.hanati.bank.bankEx.common.response.ApiErrorResponse;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e) {
        ApiErrorResponse body = new ApiErrorResponse(e.getErrorCode().name(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MortgageCoreException.class)
    public ResponseEntity<ApiErrorResponse> handleMortgageCoreException(MortgageCoreException e) {
        ApiErrorResponse body = new ApiErrorResponse(e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
