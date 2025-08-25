package com.kts.kronos.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.kts.kronos.constants.Messages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceTest {

    @Mock
    private TimeRecordProvider recordRepository;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private UserProvider userProvider;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TimeRecordService timeRecordService;

    // Dados de teste
    private UUID employeeId;
    private UUID companyId;
    private Employee employee;
    private TimeRecord openTimeRecord;
    private Long timeRecordId;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        timeRecordId = 1L;
        employee = new Employee(employeeId, "John Doe", "12345678900", "Developer", "john.doe@test.com", 5000.0, "11987654321", true, new Address("Street A", "123", "12345678", "City A", "State A"), companyId);
        openTimeRecord = new TimeRecord(timeRecordId, LocalDateTime.now().minusHours(1), null, StatusRecord.PENDING, false, true, employeeId);

        // Mocks comuns para todos os testes. Marcando-os como 'lenient'.
        lenient().when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        lenient().when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // Testes para checkin()
    @Test
    void checkin_Success() {
        when(recordRepository.findOpenByEmployeeId(any(UUID.class))).thenReturn(Optional.empty());

        timeRecordService.checkin();

        verify(recordRepository, times(1)).save(any(TimeRecord.class));
    }

    @Test
    void checkin_ThrowsException_WhenTimeRecordIsOpen() {
        when(recordRepository.findOpenByEmployeeId(any(UUID.class))).thenReturn(Optional.of(openTimeRecord));

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> timeRecordService.checkin());
        assertEquals(CHECKIN_EXCEPTION, thrown.getMessage());
        verify(recordRepository, never()).save(any(TimeRecord.class));
    }

    // Testes para checkout()
    @Test
    void checkout_Success() {
        when(recordRepository.findOpenByEmployeeId(any(UUID.class))).thenReturn(Optional.of(openTimeRecord));

        timeRecordService.checkout();

        verify(recordRepository, times(1)).save(argThat(tr -> tr.endWork() != null && tr.statusRecord() == StatusRecord.CREATED));
    }

    @Test
    void checkout_ThrowsException_WhenNoOpenTimeRecordExists() {
        when(recordRepository.findOpenByEmployeeId(any(UUID.class))).thenReturn(Optional.empty());

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> timeRecordService.checkout());
        assertEquals(CHECKOUT_EXCEPTION, thrown.getMessage());
        verify(recordRepository, never()).save(any(TimeRecord.class));
    }

    // Testes para updateTimeRecord()
    @Test
    void updateTimeRecord_ByManager_Success() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(recordRepository.findById(anyLong())).thenReturn(Optional.of(openTimeRecord));

        UpdateTimeRecordRequest request = new UpdateTimeRecordRequest(
                LocalDate.now(),
                LocalDate.now(),
                "09:00",
                "18:00",
                null
        );
        timeRecordService.updateTimeRecord(timeRecordId, request);

        verify(recordRepository, times(1)).save(argThat(tr -> tr.startWork() != null && tr.endWork() != null && tr.statusRecord() == StatusRecord.UPDATED && tr.edited()));
    }

    @Test
    void updateTimeRecord_ByPartner_SavesToRedisAndSendsToRabbit() {
        UUID managerId = UUID.randomUUID();
        UUID managerEmployeeId = UUID.randomUUID();
        User managerUser = new User(managerId, "manager_user", "password", Role.MANAGER, true, managerEmployeeId);
        Employee managerEmployee = new Employee(managerEmployeeId, "Jane Manager", "00000000000", "Manager", "jane.manager@test.com", 7000.0, "11998877665", true, new Address("Street B", "456", "87654321", "City B", "State B"), companyId);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(recordRepository.findById(anyLong())).thenReturn(Optional.of(openTimeRecord));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(managerUser));
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));

        UpdateTimeRecordRequest request = new UpdateTimeRecordRequest(
                LocalDate.now(),
                LocalDate.now(),
                "09:00",
                "18:00",
                managerId
        );
        timeRecordService.updateTimeRecord(timeRecordId, request);

        verify(recordRepository, times(1)).save(argThat(tr -> tr.statusRecord() == StatusRecord.PENDING_APPROVAL));
        verify(valueOperations, times(1)).set(eq("timerecord:approval:" + timeRecordId), any(TimeRecordChangeRequestMessage.class), eq(7L), eq(TimeUnit.DAYS));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("time-record-exchange"), eq("change.request"), any(TimeRecordChangeRequestMessage.class));
    }

    @Test
    void updateTimeRecord_ThrowsException_WhenStartAfterEnd() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(recordRepository.findById(anyLong())).thenReturn(Optional.of(openTimeRecord));

        UpdateTimeRecordRequest request = new UpdateTimeRecordRequest(
                LocalDate.now(),
                LocalDate.now(),
                "18:00",
                "09:00",
                null
        );

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> timeRecordService.updateTimeRecord(timeRecordId, request));
        assertEquals(HOURS_EXCEPTIONS, thrown.getMessage());
        verify(recordRepository, never()).save(any(TimeRecord.class));
    }

    // Testes para approveTimeRecordChange()
    @Test
    void approveTimeRecordChange_Success() {
        TimeRecord pendingRecord = new TimeRecord(timeRecordId, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), StatusRecord.PENDING_APPROVAL, true, true, employeeId);
        TimeRecordChangeRequestMessage approvalData = new TimeRecordChangeRequestMessage(timeRecordId, employeeId, UUID.randomUUID(), LocalDateTime.now().minusHours(8), LocalDateTime.now());

        when(recordRepository.findById(timeRecordId)).thenReturn(Optional.of(pendingRecord));
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(approvalData);
        when(objectMapper.convertValue(any(), eq(TimeRecordChangeRequestMessage.class))).thenReturn(approvalData);

        timeRecordService.approveTimeRecordChange(timeRecordId);

        verify(recordRepository, times(1)).save(argThat(tr -> tr.statusRecord() == StatusRecord.UPDATED));
        verify(redisTemplate, times(1)).delete("timerecord:approval:" + timeRecordId);
    }

    @Test
    void approveTimeRecordChange_ThrowsException_WhenRedisDataNotFound() {
        TimeRecord pendingRecord = new TimeRecord(timeRecordId, LocalDateTime.now(), null, StatusRecord.PENDING_APPROVAL, true, true, employeeId);
        when(recordRepository.findById(timeRecordId)).thenReturn(Optional.of(pendingRecord));
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);

        ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> timeRecordService.approveTimeRecordChange(timeRecordId));
        assertTrue(thrown.getMessage().contains("Solicitação de aprovação não encontrada ou expirada"));
    }

    // Testes para rejectTimeRecordChange()
    @Test
    void rejectTimeRecordChange_Success() {
        TimeRecord pendingRecord = new TimeRecord(timeRecordId, LocalDateTime.now(), null, StatusRecord.PENDING_APPROVAL, true, true, employeeId);
        when(recordRepository.findById(timeRecordId)).thenReturn(Optional.of(pendingRecord));

        timeRecordService.rejectTimeRecordChange(timeRecordId);

        verify(recordRepository, times(1)).save(argThat(tr -> tr.statusRecord() == StatusRecord.UPDATE_REJECTED));
        verify(redisTemplate, times(1)).delete("timerecord:approval:" + timeRecordId);
    }

    @Test
    void rejectTimeRecordChange_ThrowsException_WhenStatusIsNotPendingApproval() {
        TimeRecord record = new TimeRecord(timeRecordId, LocalDateTime.now(), null, StatusRecord.CREATED, false, true, employeeId);
        when(recordRepository.findById(timeRecordId)).thenReturn(Optional.of(record));

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> timeRecordService.rejectTimeRecordChange(timeRecordId));
        assertEquals("O registro não está aguardando aprovação.", thrown.getMessage());
    }

    // Teste para deleteTimeRecord()
    @Test
    void deleteTimeRecord_Success() {
        TimeRecord recordToDelete = new TimeRecord(timeRecordId, LocalDateTime.now(), null, StatusRecord.CREATED, false, true, employeeId);
        when(recordRepository.findById(timeRecordId)).thenReturn(Optional.of(recordToDelete));

        timeRecordService.deleteTimeRecord(employeeId, timeRecordId);

        verify(recordRepository, times(1)).deleteTimeRecord(recordToDelete);
    }

    // Teste para toggleActivate()
    @Test
    void toggleActivate_Success() {
        TimeRecord recordToToggle = new TimeRecord(timeRecordId, LocalDateTime.now(), null, StatusRecord.CREATED, false, true, employeeId);
        when(recordRepository.findById(timeRecordId)).thenReturn(Optional.of(recordToToggle));

        timeRecordService.toggleActivate(employeeId, timeRecordId);

        verify(recordRepository, times(1)).save(argThat(tr -> !tr.active()));
    }
}