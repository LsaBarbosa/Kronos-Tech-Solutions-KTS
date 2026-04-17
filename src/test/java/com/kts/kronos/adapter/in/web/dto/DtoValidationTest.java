package com.kts.kronos.adapter.in.web.dto;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void tearDown() {
        FACTORY.close();
    }

    @Test
    @DisplayName("CreateUserRequest: deve invalidar role fora do padrão")
    void shouldInvalidateCreateUserRequestWhenRoleIsInvalid() {
        CreateUserRequest dto = new CreateUserRequest(
                "john",
                "ADMIN",
                UUID.randomUUID()
        );

        Set<String> fields = VALIDATOR.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("role"));
    }

    @Test
    @DisplayName("CreateEmployeeRequest: deve invalidar email/cpf/address inválidos")
    void shouldInvalidateCreateEmployeeRequestWhenFieldsAreInvalid() {
        CreateEmployeeRequest dto = new CreateEmployeeRequest(
                "",
                "123",
                null,
                "",
                "email-invalido",
                -10.0,
                "21999999999",
                new AddressRequest("123", ""),
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<String> fields = VALIDATOR.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("fullName"));
        assertTrue(fields.contains("cpf"));
        assertTrue(fields.contains("jobPosition"));
        assertTrue(fields.contains("email"));
        assertTrue(fields.contains("salary"));
        assertTrue(fields.contains("address.postalCode"));
        assertTrue(fields.contains("address.number"));
    }

    @Test
    @DisplayName("CreateCompanyRequest: deve invalidar name/cnpj/email")
    void shouldInvalidateCreateCompanyRequestWhenFieldsAreInvalid() {
        CreateCompanyRequest dto = new CreateCompanyRequest(
                "",
                "123",
                "email-invalido",
                new AddressRequest("12345678", "10"),
                new CreateEmployeeRequest(
                        "Gestor Inicial",
                        "12345678909",
                        "12345678901",
                        "Manager",
                        "gestor@kronos.com",
                        5000.0,
                        "21999999999",
                        new AddressRequest("12345678", "10"),
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                new Location(-22.90, -43.20)
        );

        Set<String> fields = VALIDATOR.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("name"));
        assertTrue(fields.contains("cnpj"));
        assertTrue(fields.contains("email"));
    }

    @Test
    @DisplayName("GeolocationRequest: deve invalidar faceImageBase64 em branco")
    void shouldInvalidateGeolocationRequestWhenFaceImageIsBlank() {
        GeolocationRequest dto = new GeolocationRequest(-22.90, -43.20, "");

        Set<String> fields = VALIDATOR.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("faceImageBase64"));
    }
}