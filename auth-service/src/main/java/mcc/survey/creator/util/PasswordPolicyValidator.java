package mcc.survey.creator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    private static final String LOWERCASE_PATTERN = ".*[a-z].*";
    private static final String DIGIT_PATTERN = ".*[0-9].*";
    // Special characters: !@#$%^&*()_+-=[]{};':",./<>?
    // Need to escape regex special characters like [], \, / etc.
    // The example regex had an unescaped hyphen in the character class which might not behave as intended for a literal hyphen.
    // It's better to list them explicitly or ensure correct escaping.
    // Corrected regex for special characters:
    private static final String SPECIAL_CHAR_PATTERN = ".*[!@#\\$%\\^&\\*()_\\+\\-=\\[\\]{};':\",./<>?].*";


    /**
     * Validates the given password against the defined password policy.
     *
     * @param password The password to validate.
     * @throws IllegalArgumentException If the password violates one or more policy rules.
     *                                  The exception message will detail all violations.
     */
    public static void validate(String password) throws IllegalArgumentException {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }

        List<String> violations = new ArrayList<>();

        if (password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long.");
        }

        if (!Pattern.matches(UPPERCASE_PATTERN, password)) {
            violations.add("Password must contain at least one uppercase letter.");
        }

        if (!Pattern.matches(LOWERCASE_PATTERN, password)) {
            violations.add("Password must contain at least one lowercase letter.");
        }

        if (!Pattern.matches(DIGIT_PATTERN, password)) {
            violations.add("Password must contain at least one number.");
        }

        if (!Pattern.matches(SPECIAL_CHAR_PATTERN, password)) {
            violations.add("Password must contain at least one special character (e.g., !@#$%^&*()_+-=[]{};':\",./<>?).");
        }

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Password policy violated: " + String.join(" ", violations));
        }
    }

    // Optional: Main method for quick testing
    public static void main(String[] args) {
        System.out.println("Testing PasswordPolicyValidator...");
        String[] testPasswords = {
            "short",
            "nouppercase",
            "NOLOWERCASE123",
            "NoDigit!@",
            "NoSpecial123aA",
            "ValidPass1!",
            "AnotherValidP@ssw0rd",
            null,
            ""
        };

        for (String p : testPasswords) {
            try {
                validate(p);
                System.out.println("\"" + p + "\"" + " is VALID.");
            } catch (IllegalArgumentException e) {
                System.out.println("\"" + p + "\"" + " is INVALID: " + e.getMessage());
            }
        }
    }
}
