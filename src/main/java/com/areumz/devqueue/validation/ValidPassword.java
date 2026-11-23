package com.areumz.devqueue.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** At least 8 characters and at most 72 UTF-8 bytes (BCrypt only reads the first 72 bytes). */
@Documented
@Constraint(validatedBy = PasswordRuleValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "비밀번호는 8자 이상, 72바이트 이하로 입력해 주세요(한글은 약 24자까지).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
