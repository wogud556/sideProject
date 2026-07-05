package com.hanati.bank.bankEx.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.function.Predicate;

public class CustomerNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    private CustomerNoGenerator() {
    }

    public static String generate(Predicate<String> existsByCustomerNo) {
        String prefix = "C" + LocalDate.now().format(DATE_FORMAT);
        String candidate;
        do {
            candidate = prefix + String.format("%04d", RANDOM.nextInt(10000));
        } while (existsByCustomerNo.test(candidate));
        return candidate;
    }
}
