package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRecordResponseTest {

    @Test
    @DisplayName("fromDomain: deve usar saldo diário informado")
    void shouldUseProvidedDailyBalance() {
        UUID employeeId = UUID.randomUUID();
        TimeRecord record = record(
                employeeId,
                StatusRecord.CREATED,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 17, 30)
        );
        EmployeeData employeeData = new EmployeeData("Ana", "KTS");

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                employeeData,
                "/documents/1",
                "+01:30"
        );

        assertEquals("08:00", response.startHour());
        assertEquals("17:30", response.endHour());
        assertEquals("09:30", response.hoursWork());
        assertEquals("+01:30", response.balance());
        assertEquals(employeeData, response.employeeData());
        assertEquals("/documents/1", response.documentDownloadPath());
        assertEquals(employeeId, response.employeeId());
        assertEquals(100L, response.nsrCheckin());
        assertEquals(101L, response.nsrCheckout());
    }

    @Test
    @DisplayName("fromDomain: deve deixar campos de saída vazios quando não há checkout")
    void shouldLeaveCheckoutFieldsEmptyWhenRecordIsOpen() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.PENDING,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                null
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertNull(response.endWork());
        assertEquals("", response.endHour());
        assertEquals("", response.hoursWork());
        assertEquals("", response.balance());
    }

    @Test
    @DisplayName("fromDomain: deve calcular ausência como saldo negativo")
    void shouldCalculateAbsenceAsNegativeBalance() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.ABSENCE,
                LocalDateTime.of(2026, 4, 21, 0, 0),
                LocalDateTime.of(2026, 4, 21, 0, 0)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertEquals("-08:00", response.balance());
    }

    @Test
    @DisplayName("fromDomain: deve zerar saldo para status abonados")
    void shouldUseZeroBalanceForZeroBalanceStatuses() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.DAY_OFF,
                LocalDateTime.of(2026, 4, 21, 0, 0),
                LocalDateTime.of(2026, 4, 21, 0, 0)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertEquals("+00:00", response.balance());
    }

    @Test
    @DisplayName("fromDomain: registro sem edição deve retornar hasTreatment=false")
    void shouldReturnNoTreatmentWhenRecordHasNoEdition() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.CREATED,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 17, 0),
                false,
                null,
                null
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertFalse(response.hasTreatment());
        assertNull(response.treatmentLabel());
        assertNull(response.originalStartWork());
        assertNull(response.originalStartHour());
        assertNull(response.originalEndWork());
        assertNull(response.originalEndHour());
    }

    @Test
    @DisplayName("fromDomain: registro editado deve retornar hasTreatment=true")
    void shouldReturnTreatmentWhenRecordIsEdited() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.UPDATED,
                LocalDateTime.of(2026, 4, 21, 8, 30),
                LocalDateTime.of(2026, 4, 21, 17, 30),
                true,
                null,
                null
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertTrue(response.hasTreatment());
        assertEquals("Registro tratado", response.treatmentLabel());
    }

    @Test
    @DisplayName("fromDomain: original diferente do atual deve retornar comparação original/tratada")
    void shouldReturnTreatmentWhenOriginalDiffersFromCurrentRecord() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.CREATED,
                LocalDateTime.of(2026, 4, 21, 8, 30),
                LocalDateTime.of(2026, 4, 21, 17, 30),
                false,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 17, 0)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertTrue(response.hasTreatment());
        assertEquals("Possui ajuste", response.treatmentLabel());
        assertEquals(LocalDateTime.of(2026, 4, 21, 8, 0), response.originalStartWork());
        assertEquals("08:00", response.originalStartHour());
        assertEquals(LocalDateTime.of(2026, 4, 21, 17, 0), response.originalEndWork());
        assertEquals("17:00", response.originalEndHour());
    }

    @Test
    @DisplayName("fromDomain: deve preencher label de aprovação pendente e rejeição")
    void shouldReturnTreatmentLabelsForApprovalStatuses() {
        TimeRecord pending = record(
                UUID.randomUUID(),
                StatusRecord.PENDING_APPROVAL,
                LocalDateTime.of(2026, 4, 21, 8, 30),
                LocalDateTime.of(2026, 4, 21, 17, 30),
                true,
                null,
                null
        );
        TimeRecord rejected = record(
                UUID.randomUUID(),
                StatusRecord.UPDATE_REJECTED,
                LocalDateTime.of(2026, 4, 21, 8, 30),
                LocalDateTime.of(2026, 4, 21, 17, 30),
                true,
                null,
                null
        );

        assertEquals(
                "Aguardando aprovação",
                TimeRecordResponse.fromDomain(pending, Duration.ofHours(8), null, null, null).treatmentLabel()
        );
        assertEquals(
                "Alteração rejeitada",
                TimeRecordResponse.fromDomain(rejected, Duration.ofHours(8), null, null, null).treatmentLabel()
        );
    }

    @Test
    @DisplayName("JSON: deve serializar campos novos do contrato de relatório")
    void shouldSerializeNewReportContractFields() throws Exception {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.UPDATED,
                LocalDateTime.of(2026, 4, 21, 8, 30),
                LocalDateTime.of(2026, 4, 21, 17, 30),
                true,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 17, 0)
        );
        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                new EmployeeData("Ana", "KTS"),
                null,
                null
        );
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = mapper.writeValueAsString(response);
        JsonNode root = mapper.readTree(json);

        assertEquals("21-04-2026", root.get("originalStartWork").asText());
        assertEquals("08:00", root.get("originalStartHour").asText());
        assertEquals("21-04-2026", root.get("originalEndWork").asText());
        assertEquals("17:00", root.get("originalEndHour").asText());
        assertTrue(root.get("hasTreatment").asBoolean());
        assertEquals("Registro tratado", root.get("treatmentLabel").asText());
        assertEquals(100L, root.get("nsrCheckin").asLong());
        assertEquals(101L, root.get("nsrCheckout").asLong());
    }

    @Test
    @DisplayName("fromDomain: deve calcular saldo individual quando dailyBalance não é informado")
    void shouldCalculateIndividualBalanceWhenDailyBalanceIsMissing() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.CREATED,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 15, 30)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertEquals("-00:30", response.balance());
    }

    private static TimeRecord record(
            UUID employeeId,
            StatusRecord status,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return record(employeeId, status, start, end, true, null, null);
    }

    private static TimeRecord record(
            UUID employeeId,
            StatusRecord status,
            LocalDateTime start,
            LocalDateTime end,
            boolean edited,
            LocalDateTime originalStart,
            LocalDateTime originalEnd
    ) {
        return new TimeRecord(
                10L,
                start,
                end,
                status,
                edited,
                true,
                employeeId,
                -2.53,
                -44.30,
                -2.54,
                -44.31,
                100L,
                101L,
                originalStart,
                originalEnd
        );
    }
}
