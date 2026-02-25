package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

import static com.kts.kronos.constants.Messages.INVALID_PASSWORD_POLICY;

@Component
public class PasswordPolicyValidator {

    private final Pattern passwordPattern;

    public PasswordPolicyValidator(
            @Value("${security.password.policy-regex:${SECURITY_PASSWORD_POLICY_REGEX:^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$}}") String policyRegex
    ) {
        this.passwordPattern = Pattern.compile(policyRegex);
    }

    public void validate(String rawPassword) {
        if (rawPassword == null || !passwordPattern.matcher(rawPassword).matches()) {
            throw new BadRequestException(INVALID_PASSWORD_POLICY);
        }
    }
}