package com.hanati.bank.repayment.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/** RP + yyyyMMdd + 6자리 일련번호 (명세 8.2 응답 예시의 RP20261004000001 형식). */
public final class TransactionNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TransactionNumberGenerator() {
    }

    public static String generate(LocalDate businessDate, Predicate<String> exists) {
        String prefix = "RP" + businessDate.format(DATE);
        String candidate;
        do {
            candidate = prefix + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        } while (exists.test(candidate));
        return candidate;
    }
}
