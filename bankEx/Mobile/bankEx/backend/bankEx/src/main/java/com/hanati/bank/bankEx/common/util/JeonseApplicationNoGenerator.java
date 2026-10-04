package com.hanati.bank.bankEx.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.function.Predicate;

public class JeonseApplicationNoGenerator {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private JeonseApplicationNoGenerator() {
    }

    public static String generate(Predicate<String> existsByApplicationId) {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String candidate;
        do {
            candidate = "JL" + datePart + String.format("%04d", RANDOM.nextInt(10_000));
        } while (existsByApplicationId.test(candidate));
        return candidate;
    }
}
