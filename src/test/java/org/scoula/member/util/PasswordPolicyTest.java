package org.scoula.member.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {
    @Test
    void rejectsSevenCharacterPassword() {
        assertFalse(PasswordPolicy.isValid("1234567"));
    }

    @Test
    void acceptsEightCharacterPassword() {
        assertTrue(PasswordPolicy.isValid("12345678"));
    }

    @Test
    void acceptsAsciiWithoutRequiringCharacterTypeCombination() {
        assertTrue(PasswordPolicy.isValid("abcdefgh"));
        assertTrue(PasswordPolicy.isValid("12345678"));
        assertTrue(PasswordPolicy.isValid("!!!!!!!!"));
        assertTrue(PasswordPolicy.isValid("abc123!!"));
        assertTrue(PasswordPolicy.isValid("ABCdef12!"));
    }

    @Test
    void rejectsKoreanAndOtherNonAsciiCharacters() {
        assertFalse(PasswordPolicy.isValid("가나다라마바사아"));
        assertFalse(PasswordPolicy.isValid("password한글"));
        assertFalse(PasswordPolicy.isValid("abc123가!"));
        assertFalse(PasswordPolicy.isValid("password😀"));
    }
}
