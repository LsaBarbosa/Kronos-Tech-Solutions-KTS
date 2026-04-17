package com.kts.kronos.adapter.in.web.dto;

import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.document.DocumentResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeResponse;
import com.kts.kronos.adapter.in.web.dto.message.MessageResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserSearchItemResponse;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResponseMapperTest {

    @Test
    @DisplayName("AddressResponse.fromDomain: deve mapear endereço")
    void shouldMapAddressResponseFromDomain() {
        Address address = new Address("Rua A", "10", "12345678", "Rio", "RJ");

        AddressResponse response = AddressResponse.fromDomain(address);

        assertEquals("Rua A", response.street());
        assertEquals("10", response.number());
        assertEquals("12345678", response.postalCode());
        assertEquals("Rio", response.city());
        assertEquals("RJ", response.state());
    }

    @Test
    @DisplayName("CompanyResponse.fromDomain: deve mapear empresa")
    void shouldMapCompanyResponseFromDomain() {
        Company company = new Company(
                UUID.randomUUID(),
                "Kronos Tech",
                "12345678000199",
                "contato@kronos.com",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                new Location(-22.90, -43.20),
                5,
                2
        );

        CompanyResponse response = CompanyResponse.fromDomain(company);

        assertEquals(company.companyId(), response.id());
        assertEquals(company.name(), response.name());
        assertEquals(company.cnpj(), response.cnpj());
        assertEquals(company.email(), response.email());
        assertEquals(company.activeEmployees(), response.activeEmployees());
        assertEquals(company.inactiveEmployees(), response.inactiveEmployees());
        assertEquals("12345678", response.address().postalCode());
    }

    @Test
    @DisplayName("UserResponse/UserSearchItemResponse: devem mapear role e flags")
    void shouldMapUserResponsesFromDomain() {
        User user = new User(
                UUID.randomUUID(),
                "john",
                "encoded-password",
                Role.MANAGER,
                true,
                UUID.randomUUID()
        );

        UserResponse full = UserResponse.fromDomain(user);
        UserSearchItemResponse search = UserSearchItemResponse.fromDomain(user);

        assertEquals(user.userId(), full.userId());
        assertEquals("MANAGER", full.role());
        assertEquals(user.employeeId(), full.employeeId());

        assertEquals(user.userId(), search.userId());
        assertEquals("MANAGER", search.role());
        assertEquals(user.active(), search.active());
    }

    @Test
    @DisplayName("MessageResponse.fromDomain: deve mapear mensagem")
    void shouldMapMessageResponseFromDomain() {
        Message message = new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Comunicado",
                "Aviso importante",
                MessagePriority.CRITICAL,
                LocalDateTime.of(2026, 4, 17, 10, 30),
                UUID.randomUUID()
        );

        MessageResponse response = MessageResponse.fromDomain(message);

        assertEquals(message.messageId(), response.messageId());
        assertEquals(message.title(), response.title());
        assertEquals(message.messageText(), response.messageText());
        assertEquals(message.priority(), response.priority());
        assertEquals(message.employeeId(), response.senderEmployeeId());
        assertEquals(message.recipientEmployeeId(), response.recipientEmployeeId());
    }

    @Test
    @DisplayName("DocumentResponse.fromDomain: deve mapear documento")
    void shouldMapDocumentResponseFromDomain() {
        Document document = new Document(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "docs/holerite.pdf",
                LocalDateTime.of(2026, 4, 17, 9, 0),
                10L,
                false,
                false
        );

        DocumentResponse response = DocumentResponse.fromDomain(document);

        assertEquals(document.documentId(), response.id());
        assertEquals(document.fileName(), response.fileName());
        assertEquals(document.contentType(), response.contentType());
        assertEquals(document.uploadeAt(), response.uploadedAt());
    }

    @Test
    @DisplayName("EmployeeResponse.fromDomain: deve mapear employee com role e escala")
    void shouldMapEmployeeResponseFromDomain() {
        Employee employee = new Employee(
                UUID.randomUUID(),
                "Maria Silva",
                "12345678909",
                "12345678901",
                "Developer",
                "maria@kronos.com",
                5000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 4, 17, 8, 0),
                true,
                "faces/maria.jpg",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 4, 1),
                DayOfWeek.MONDAY,
                2,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );

        EmployeeResponse response = EmployeeResponse.fromDomain(employee, "Kronos Tech", "MANAGER");

        assertEquals(employee.employeeId(), response.employeeId());
        assertEquals(employee.fullName(), response.fullName());
        assertEquals(employee.cpf(), response.maskedCpf());
        assertEquals("Kronos Tech", response.companyName());
        assertEquals("MANAGER", response.role());
        assertEquals(employee.scheduleType(), response.scheduleType());
        assertEquals(employee.scaleStartDate(), response.scaleStartDate());
        assertEquals(employee.fixedWorkDays(), response.fixedWorkDays());
    }
}