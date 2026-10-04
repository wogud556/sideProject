package com.hanati.bank.refinance.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.function.Predicate;

public class RefinanceApplicationNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    private RefinanceApplicationNoGenerator() {
    }

    public static String generate(Predicate<String> existsByApplicationNo) {
        String prefix = "RF" + LocalDate.now().format(DATE_FORMAT);
        String candidate;
        do {
            candidate = prefix + String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (existsByApplicationNo.test(candidate));
        return candidate;
    }
}
