package com.hanati.bank.refinance.common.util;

public class MaskUtil {

    private MaskUtil() {
    }

    public static String maskName(String name) {
        if (name == null || name.length() <= 1) return name;
        char[] chars = name.toCharArray();
        for (int i = 1; i < chars.length - 1; i++) chars[i] = '*';
        return new String(chars);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return phone;
        String[] parts = phone.split("-");
        if (parts.length == 3) {
            return parts[0] + "-****-" + parts[2];
        }
        int len = phone.length();
        return phone.substring(0, len - 8) + "****" + phone.substring(len - 4);
    }

    public static String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) return accountNo;
        int visible = 4;
        int maskLen = accountNo.length() - visible;
        return "*".repeat(maskLen) + accountNo.substring(maskLen);
    }
}
