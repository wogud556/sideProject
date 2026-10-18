package com.hanati.bank.mortgage.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** 대표 번호 채번. DB 식별자를 기반으로 하므로 중복 검사가 필요 없다. */
public final class MortgageNoGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private MortgageNoGenerator() {
    }

    public static String applicationNo(LocalDate date, long applicationId) {
        return "MA-" + date.format(DATE) + "-" + String.format("%06d", applicationId);
    }

    public static String agreementNo(LocalDate date, long agreementId) {
        return "MAG-" + date.format(DATE) + "-" + String.format("%06d", agreementId);
    }

    public static String executionNo(LocalDate date, long executionId) {
        return "MEX-" + date.format(DATE) + "-" + String.format("%06d", executionId);
    }

    public static String loanAccountNo(long loanAccountId) {
        return "MLN-" + String.format("%08d", loanAccountId);
    }

    /**
     * bankEx 입금 거래의 멱등 키. 신청 건 하나당 하나로 고정해서,
     * 실행을 몇 번 재시도하더라도 고객 계좌에는 한 번만 입금되게 한다.
     */
    public static String disbursementKey(String applicationNo) {
        return "MGD-" + applicationNo;
    }
}
