package com.hanati.bank.bankEx.common.util;

import java.util.Random;
import java.util.function.Predicate;

public class AccountNoGenerator {

    private static final Random RANDOM = new Random();

    private AccountNoGenerator() {
    }

    public static String generate(Predicate<String> existsByAccountNo) {
        String candidate;
        do {
            candidate = "100" + String.format("%09d", (long) (RANDOM.nextDouble() * 1_000_000_000L));
        } while (existsByAccountNo.test(candidate));
        return candidate;
    }
}
