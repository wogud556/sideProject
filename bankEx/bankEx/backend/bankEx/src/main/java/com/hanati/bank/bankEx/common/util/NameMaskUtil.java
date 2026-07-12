package com.hanati.bank.bankEx.common.util;

public class NameMaskUtil {

    private NameMaskUtil() {
    }

    public static String mask(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        char[] chars = name.toCharArray();
        for (int i = 1; i < chars.length - 1; i++) {
            chars[i] = '*';
        }
        return new String(chars);
    }
}
