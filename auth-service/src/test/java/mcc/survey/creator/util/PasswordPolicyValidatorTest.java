package mcc.survey.creator.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PasswordPolicyValidatorTest {

    @Test
    void validate_validPassword_shouldNotThrow() {
        assertDoesNotThrow(() -> PasswordPolicyValidator.validate("ValidP@ss1"));
        assertDoesNotThrow(() -> PasswordPolicyValidator.validate("Str0ngP@$$wOrd"));
        assertDoesNotThrow(() -> PasswordPolicyValidator.validate("An0ther!Good_one"));
    }

    @Test
    void validate_nullPassword_shouldThrowIllegalArgumentException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate(null);
        });
        assertEquals("Password cannot be null.", exception.getMessage());
    }

    @Test
    void validate_emptyPassword_shouldThrowAndContainAllViolations() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("");
        });
        String message = exception.getMessage();
        assertTrue(message.contains("Password must be at least 8 characters long."));
        assertTrue(message.contains("Password must contain at least one uppercase letter."));
        assertTrue(message.contains("Password must contain at least one lowercase letter."));
        assertTrue(message.contains("Password must contain at least one number."));
        assertTrue(message.contains("Password must contain at least one special character"));
    }

    @Test
    void validate_tooShort_shouldThrowAndContainLengthViolation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("Sh@1"); // Length 4, has upper, lower, special, number
        });
        String message = exception.getMessage();
        assertTrue(message.startsWith("Password policy violated:"));
        assertTrue(message.contains("Password must be at least 8 characters long."));
        // Depending on how PasswordPolicyValidator collects errors, other rules might pass for "Sh@1"
        // but the primary check here is the length.
    }

    @Test
    void validate_missingUppercase_shouldThrowAndContainUppercaseViolation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("invalidp@ss1");
        });
        String message = exception.getMessage();
        assertTrue(message.startsWith("Password policy violated:"));
        assertTrue(message.contains("Password must contain at least one uppercase letter."));
    }

    @Test
    void validate_missingLowercase_shouldThrowAndContainLowercaseViolation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("INVALIDP@SS1");
        });
        String message = exception.getMessage();
        assertTrue(message.startsWith("Password policy violated:"));
        assertTrue(message.contains("Password must contain at least one lowercase letter."));
    }

    @Test
    void validate_missingNumber_shouldThrowAndContainNumberViolation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("InvalidP@ss");
        });
        String message = exception.getMessage();
        assertTrue(message.startsWith("Password policy violated:"));
        assertTrue(message.contains("Password must contain at least one number."));
    }

    @Test
    void validate_missingSpecialChar_shouldThrowAndContainSpecialCharViolation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("InvalidPass1");
        });
        String message = exception.getMessage();
        assertTrue(message.startsWith("Password policy violated:"));
        assertTrue(message.contains("Password must contain at least one special character"));
    }

    @Test
    void validate_multipleViolations_shortAndNoSpecial_shouldThrowAndContainMultipleMessages() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            PasswordPolicyValidator.validate("short"); // No uppercase, no number, no special, length < 8
        });
        String message = exception.getMessage();
        assertTrue(message.startsWith("Password policy violated:"));
        assertTrue(message.contains("Password must be at least 8 characters long."));
        assertTrue(message.contains("Password must contain at least one uppercase letter."));
        // "short" has lowercase
        assertTrue(message.contains("Password must contain at least one number."));
        assertTrue(message.contains("Password must contain at least one special character"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "NoUpper1@",      // No uppercase
        "noupper1@",      // No uppercase (covered by specific test, but good for parameterized too)
        "NOLOWER1@",      // No lowercase
        "NoLowerCaSe@",   // No number
        "NoSpecial1aA",   // No special character
        "short",          // Too short, and other violations
        "1234567",        // Too short, no letters, no special
        "abcdefg",        // Too short, no upper, no num, no special
        "ABCDEFG",        // Too short, no lower, no num, no special
        "1234ABCD",       // Has length, num, upper, but no lower, no special
        "abcdEFGH",       // Has length, lower, upper, but no num, no special
        "1234abcd!!!!",   // Has length, num, lower, special, but no upper
        "ABCD!!!!1234",   // Has length, upper, special, num, but no lower
        "UPPERlower123",  // No special
        "UPPERlower!!!",  // No number
        "UPPER123!!!",    // No lower
        "lower123!!!"     // No upper
    })
    void validate_variousInvalidPasswords_shouldThrow(String invalidPassword) {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicyValidator.validate(invalidPassword));
    }
}
