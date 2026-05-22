package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.adapter.in.web.dto.company.Location;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityConversionTest {

    @Test
    @DisplayName("AddressEmbeddable: deve converter domain <-> entity")
    void shouldConvertAddressEmbeddable() {
        Address address = new Address("Rua A", "10", "12345678", "Rio", "RJ");

        AddressEmbeddable embeddable = AddressEmbeddable.fromDomain(address);
        Address converted = embeddable.toDomain();

        assertEquals(address.street(), converted.street());
        assertEquals(address.number(), converted.number());
        assertEquals(address.postalCode(), converted.postalCode());
        assertEquals(address.city(), converted.city());
        assertEquals(address.state(), converted.state());
    }

    @Test
    @DisplayName("CompanyEntity: deve converter domain <-> entity")
    void shouldConvertCompanyEntity() {
        Company company = new Company(
                UUID.randomUUID(),
                "Kronos Tech",
                "12345678000199",
                "contato@kronos.com",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                new Location(-22.90, -43.20),
                7,
                3
        );

        CompanyEntity entity = CompanyEntity.fromDomain(company);
        Company converted = entity.toDomain();

        assertEquals(company.companyId(), entity.getId());
        assertEquals(company.companyId(), converted.companyId());
        assertEquals(company.name(), converted.name());
        assertEquals(company.cnpj(), converted.cnpj());
        assertEquals(company.email(), converted.email());
    }

    @Test
    @DisplayName("UserEntity: deve converter domain <-> entity")
    void shouldConvertUserEntity() {
        User user = new User(
                UUID.randomUUID(),
                "john",
                "encoded-password",
                Role.MANAGER,
                true,
                UUID.randomUUID()
        );

        UserEntity entity = UserEntity.fromDomain(user);
        User converted = entity.toDomain();

        assertEquals(user.userId(), entity.getUserId());
        assertEquals(user.userId(), converted.userId());
        assertEquals(user.username(), converted.username());
        assertEquals(user.role(), converted.role());
        assertEquals(user.employeeId(), converted.employeeId());
    }

    @Test
    @DisplayName("EmployeeEntity: deve converter domain <-> entity preservando fixedWorkDays")
    void shouldConvertEmployeeEntityPreservingFixedWorkDays() {
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
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        );

        EmployeeEntity entity = EmployeeEntity.fromDomain(employee);
        Employee converted = entity.toDomain();

        assertEquals(employee.employeeId(), converted.employeeId());
        assertEquals(employee.scheduleType(), converted.scheduleType());
        assertEquals(employee.scaleStartDate(), converted.scaleStartDate());
        assertEquals(employee.preferredDayOff(), converted.preferredDayOff());
        assertEquals(employee.weekendOffIndex(), converted.weekendOffIndex());
        assertEquals(employee.fixedWorkDays(), converted.fixedWorkDays());
        assertTrue(entity.getFixedWorkDays().contains("MONDAY"));
        assertTrue(entity.getFixedWorkDays().contains("WEDNESDAY"));
    }

    @Test
    @DisplayName("DocumentEntity: deve converter domain <-> entity")
    void shouldConvertDocumentEntity() {
        Document document = new Document(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "docs/holerite.pdf",
                LocalDateTime.of(2026, 4, 17, 9, 0),
                10L,
                true,
                false,
                "checksum"
        );

        DocumentEntity entity = DocumentEntity.fromDomain(document);
        Document converted = entity.toDomain();

        assertEquals(document.documentId(), converted.documentId());
        assertEquals(document.employeeId(), converted.employeeId());
        assertEquals(document.type(), converted.type());
        assertEquals(document.deletedByEmployee(), converted.deletedByEmployee());
        assertEquals(document.deletedByManager(), converted.deletedByManager());
        assertEquals("checksum", converted.checksumSha256());
    }

    @Test
    @DisplayName("MessageEntity: deve converter domain <-> entity")
    void shouldConvertMessageEntity() {
        Message message = new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Comunicado",
                "Aviso importante",
                MessagePriority.NORMAL,
                LocalDateTime.of(2026, 4, 17, 10, 30),
                UUID.randomUUID()
        );

        MessageEntity entity = MessageEntity.fromDomain(message);
        Message converted = entity.toDomain();

        assertEquals(message.messageId(), converted.messageId());
        assertEquals(message.employeeId(), converted.employeeId());
        assertEquals(message.companyId(), converted.companyId());
        assertEquals(message.title(), converted.title());
        assertEquals(message.messageText(), converted.messageText());
        assertEquals(message.priority(), converted.priority());
        assertEquals(message.recipientEmployeeId(), converted.recipientEmployeeId());
    }
}
