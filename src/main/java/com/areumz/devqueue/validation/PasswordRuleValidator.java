package com.areumz.devqueue.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class PasswordRuleValidator implements ConstraintValidator<ValidPassword, String> {
    private static final int MIN_LENGTH = 8;
    private static final int MAX_BYTES = 72;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // 빈 값은 @NotBlank가 따로 잡는다
        }
        return value.length() >= MIN_LENGTH
                && value.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
