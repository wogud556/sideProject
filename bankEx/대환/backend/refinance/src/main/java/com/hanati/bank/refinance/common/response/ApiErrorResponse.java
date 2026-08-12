package com.hanati.bank.refinance.common.response;

import lombok.Getter;

@Getter
public class ApiErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final Object data = null;

    public ApiErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
