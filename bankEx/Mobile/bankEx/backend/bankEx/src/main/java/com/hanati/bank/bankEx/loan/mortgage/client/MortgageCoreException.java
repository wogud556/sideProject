package com.hanati.bank.bankEx.loan.mortgage.client;

import lombok.Getter;

/** Core/mortgage가 업무 오류로 거절한 요청. Core의 코드와 메시지를 그대로 고객에게 전달한다. */
@Getter
public class MortgageCoreException extends RuntimeException {

    private final String code;

    public MortgageCoreException(String code, String message) {
        super(message);
        this.code = code;
    }
}
