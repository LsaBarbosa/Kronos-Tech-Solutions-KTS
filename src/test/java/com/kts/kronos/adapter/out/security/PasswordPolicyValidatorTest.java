package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyValidatorTest {

    @Test
    void shouldAcceptValidPassword() {
        var validator = new PasswordPolicyValidator("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

        assertThatCode(() -> validator.validate("StrongPass1")).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullAndInvalidPassword() {
        var validator = new PasswordPolicyValidator("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> validator.validate("weak")).isInstanceOf(BadRequestException.class);
    }
}
