package org.scoula.member.util;

public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8;
    private static final int MAX_ASCII = 0x7F;

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= MIN_LENGTH
                && password.chars().allMatch(character -> character <= MAX_ASCII);
    }
}
