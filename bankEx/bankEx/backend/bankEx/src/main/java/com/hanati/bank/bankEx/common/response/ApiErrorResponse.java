package com.hanati.bank.bankEx.common.response;

import lombok.Getter;

@Getter
public class ApiErrorResponse {

    private final boolean success;
    private final String code;
    private final String message;

    public ApiErrorResponse(String code, String message) {
        this.success = false;
        this.code = code;
        this.message = message;
    }
}
