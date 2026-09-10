package org.scoula.member.util;

public final class NicknamePolicy {
    public static final int MAX_LENGTH = 7;

    private NicknamePolicy() {
    }

    public static boolean isValid(String nickname) {
        return nickname != null
                && !nickname.isBlank()
                && nickname.codePoints().noneMatch(Character::isWhitespace)
                && nickname.codePointCount(0, nickname.length()) <= MAX_LENGTH;
    }

    public static String normalizeGenerated(String nickname, String fallback) {
        String withoutWhitespace = nickname == null
                ? ""
                : nickname.codePoints()
                        .filter(codePoint -> !Character.isWhitespace(codePoint))
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
        String value = withoutWhitespace.isBlank() ? fallback : withoutWhitespace;
        return truncate(value, MAX_LENGTH);
    }

    public static String withSuffix(String base, String suffix) {
        int baseLength = Math.max(0, MAX_LENGTH - suffix.codePointCount(0, suffix.length()));
        return truncate(base, baseLength) + suffix;
    }

    private static String truncate(String value, int maxLength) {
        if (value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
