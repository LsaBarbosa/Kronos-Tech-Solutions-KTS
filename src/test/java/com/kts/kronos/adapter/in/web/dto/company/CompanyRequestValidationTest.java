package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void deveInvalidarCreateCompanyQuandoCnpjEEmailForemInvalidos() {
        var request = new CreateCompanyRequest(
                "",
                "123",
                "email-invalido",
                new AddressRequest("12345678", "10"),
                null,
                new Location(-2.53, -44.30)
        );

        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);

        assertTrue(hasField(violations, "name"));
        assertTrue(hasField(violations, "cnpj"));
        assertTrue(hasField(violations, "email"));
    }

    @Test
    void deveInvalidarCreateCompanyQuandoAddressForInvalido() {
        var request = new CreateCompanyRequest(
                "KTS",
                "12345678000199",
                "contato@kts.com",
                new AddressRequest("", ""),
                null,
                new Location(-2.53, -44.30)
        );

        Set<ConstraintViolation<CreateCompanyRequest>> violations = validator.validate(request);

        assertTrue(hasField(violations, "address.postalCode"));
        assertTrue(hasField(violations, "address.number"));
    }

    @Test
    void deveInvalidarUpdateCompanyQuandoNamePassarDe50Caracteres() {
        var request = new UpdateCompanyRequest(
                "x".repeat(51),
                "contato@kts.com",
                true,
                new UpdateAddressRequest("12345678", "10"),
                new Location(-2.53, -44.30)
        );

        Set<ConstraintViolation<UpdateCompanyRequest>> violations = validator.validate(request);

        assertTrue(hasField(violations, "name"));
    }

    @Test
    void deveInvalidarUpdateCompanyQuandoEmailForInvalido() {
        var request = new UpdateCompanyRequest(
                "KTS",
                "email-invalido",
                true,
                new UpdateAddressRequest("12345678", "10"),
                new Location(-2.53, -44.30)
        );

        Set<ConstraintViolation<UpdateCompanyRequest>> violations = validator.validate(request);

        assertTrue(hasField(violations, "email"));
    }

    private static boolean hasField(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }
}