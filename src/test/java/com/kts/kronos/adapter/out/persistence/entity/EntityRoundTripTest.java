package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntityRoundTripTest {

    @Test
    void deveFazerRoundTripDeAddressEmbeddable() {
        var address = new Address("Rua A", "10", "65000000", "São Luís", "MA");

        var entity = AddressEmbeddable.fromDomain(address);
        var back = entity.toDomain();

        assertEquals(address, back);
    }

    @Test
    void deveFazerRoundTripDeCompanyEntity() {
        var company = new Company(
                UUID.randomUUID(),
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                new Location(-2.53, -44.30),
                7,
                2
        );

        var back = CompanyEntity.fromDomain(company).toDomain();

        assertEquals(company.companyId(), back.companyId());
        assertEquals(company.name(), back.name());
        assertEquals(company.cnpj(), back.cnpj());
        assertEquals(company.email(), back.email());
        assertEquals(company.address().postalCode(), back.address().postalCode());
    }

    @Test
    void deveFazerRoundTripDeDocumentEntity() {
        var document = new Document(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.TIME_OFF,
                "arquivo.pdf",
                "application/pdf",
                "docs/arquivo.pdf",
                LocalDateTime.now(),
                10L,
                true,
                false,
                "checksum"
        );

        var back = DocumentEntity.fromDomain(document).toDomain();

        assertEquals(document.documentId(), back.documentId());
        assertEquals(document.employeeId(), back.employeeId());
        assertEquals(document.type(), back.type());
        assertEquals(document.deletedByEmployee(), back.deletedByEmployee());
        assertEquals(document.deletedByManager(), back.deletedByManager());
        assertEquals("checksum", back.checksumSha256());
    }

    @Test
    void deveFazerRoundTripDeEmployeeEntityComCamposOpcionais() {
        var employee = new Employee(
                UUID.randomUUID(),
                "Lucas",
                "12345678901",
                null,
                "Dev",
                "lucas@kts.com",
                1000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                UUID.randomUUID(),
                LocalDateTime.now(),
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 4, 1),
                DayOfWeek.SUNDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );

        var back = EmployeeEntity.fromDomain(employee).toDomain();

        assertEquals(employee.employeeId(), back.employeeId());
        assertEquals(employee.fullName(), back.fullName());
        assertEquals(employee.scheduleType(), back.scheduleType());
        assertEquals(employee.fixedWorkDays(), back.fixedWorkDays());
        assertNull(back.faceS3ObjectKey());
    }

    @Test
    void deveFazerRoundTripDeMessageTimeRecordApprovalTimeRecordEUser() {
        var message = new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Titulo",
                "Texto",
                MessagePriority.ALERT,
                LocalDateTime.now(),
                UUID.randomUUID()
        );
        var messageBack = MessageEntity.fromDomain(message).toDomain();
        assertEquals(message.title(), messageBack.title());

        var approval = new TimeRecordApprovalRequest(
                10L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.now().minusHours(8),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now()
        );
        var approvalBack = TimeRecordApprovalEntity.fromDomain(approval).toDomain();
        assertEquals(approval.timeRecordId(), approvalBack.timeRecordId());

        var timeRecord = new TimeRecord(
                20L,
                LocalDateTime.now().minusHours(8),
                LocalDateTime.now().minusHours(1),
                StatusRecord.CREATED,
                true,
                true,
                UUID.randomUUID(),
                -2.53,
                -44.30,
                -2.54,
                -44.31,
                100L,
                200L,
                LocalDateTime.now().minusHours(8),
                LocalDateTime.now().minusHours(1)
        );
        var timeRecordBack = TimeRecordEntity.fromDomain(timeRecord).toDomain();
        assertEquals(timeRecord.timeRecordId(), timeRecordBack.timeRecordId());
        assertEquals(timeRecord.nsrCheckin(), timeRecordBack.nsrCheckin());
        assertEquals(timeRecord.nsrCheckout(), timeRecordBack.nsrCheckout());

        var user = new User(
                UUID.randomUUID(),
                "lucas",
                "encoded",
                Role.MANAGER,
                true,
                UUID.randomUUID()
        );
        var userBack = UserEntity.fromDomain(user).toDomain();
        assertEquals(user.username(), userBack.username());
        assertEquals(user.role(), userBack.role());
    }
}
