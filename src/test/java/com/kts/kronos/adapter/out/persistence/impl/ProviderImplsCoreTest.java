package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.web.dto.security.RecoverPasswordCredentials;
import com.kts.kronos.adapter.in.web.dto.security.RecoverPasswordProjection;
import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderImplsCoreTest {

    @Mock AfdEntryRepository afdEntryRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock CompanyRepository companyRepository;
    @Mock DocumentRepository documentRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock MessageRepository messageRepository;
    @Mock CompanyNsrRepository companyNsrRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock TimeRecordApprovalRepository timeRecordApprovalRepository;
    @Mock UserRepository userRepository;

    @InjectMocks AfdEntryProviderImpl afdEntryProvider;
    @InjectMocks AuditLogProviderImpl auditLogProvider;
    @InjectMocks CompanyProviderImpl companyProvider;
    @InjectMocks DocumentProviderImpl documentProvider;
    @InjectMocks EmployeeProviderImpl employeeProvider;
    @InjectMocks MessageProviderImpl messageProvider;
    @InjectMocks NsrProviderImpl nsrProvider;
    @InjectMocks PasswordResetTokenProviderImpl passwordResetTokenProvider;
    @InjectMocks TimeRecordApprovalProviderImpl timeRecordApprovalProvider;
    @InjectMocks UserProviderImpl userProvider;

    @Test
    void afdProvider_shouldCoverMethods() {
        AfdEntry domain = mock(AfdEntry.class);
        AfdEntryEntity saved = mock(AfdEntryEntity.class);
        when(afdEntryRepository.save(any())).thenReturn(saved);
        when(saved.getId()).thenReturn(1L);
        when(saved.getNsr()).thenReturn(1L);

        assertThat(afdEntryProvider.save(domain).nsr()).isEqualTo(1L);

        UUID companyId = UUID.randomUUID();
        when(afdEntryRepository.findLastHashByCompanyId(companyId)).thenReturn(Optional.of("hash"));
        assertThat(afdEntryProvider.findLastHashByCompanyId(companyId)).contains("hash");

        AfdEntryEntity streamEntity = mock(AfdEntryEntity.class);
        when(streamEntity.getId()).thenReturn(2L);
        when(streamEntity.getNsr()).thenReturn(2L);
        when(afdEntryRepository.streamAllByCompanyIdOrderByNsrAsc(companyId)).thenReturn(Stream.of(streamEntity));
        assertThat(afdEntryProvider.streamByCompanyIdOrderByNsr(companyId).toList()).hasSize(1);
    }

    @Test
    void auditLog_shouldIgnoreFailure() {
        AuditLog domain = new AuditLog(UUID.randomUUID(), UUID.randomUUID(), "A", "ip", "ua", "d", LocalDateTime.now());
        auditLogProvider.registerLog(domain);
        verify(auditLogRepository).save(any(AuditLogEntity.class));

        doThrow(new RuntimeException("x")).when(auditLogRepository).save(any());
        assertThatCode(() -> auditLogProvider.registerLog(domain)).doesNotThrowAnyException();
    }

    @Test
    void companyProvider_shouldCoverMethods() {
        Company c = new Company(UUID.randomUUID(), "Comp", "123", "c@c.com", true,
                new Address("street", "10", "01001000", "SP", "SP"),
                new Location(1.0, 2.0), 0, 0);
        CompanyEntity e = mock(CompanyEntity.class);
        Company domain = mock(Company.class);
        when(companyRepository.save(any())).thenReturn(e);
        when(e.toDomain()).thenReturn(domain);
        companyProvider.save(c);

        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.of(e));
        when(companyRepository.findByCnpj("1")).thenReturn(Optional.of(e));
        when(companyRepository.findAll()).thenReturn(List.of(e));
        when(companyRepository.findByActiveTrue()).thenReturn(List.of(e));
        when(companyRepository.findByActiveFalse()).thenReturn(List.of(e));
        when(companyRepository.existsByCnpj("1")).thenReturn(true);

        assertThat(companyProvider.findById(id)).contains(domain);
        assertThat(companyProvider.findByCnpj("1")).contains(domain);
        assertThat(companyProvider.findAll()).hasSize(1);
        assertThat(companyProvider.findByActive(true)).hasSize(1);
        assertThat(companyProvider.findByActive(false)).hasSize(1);
        assertThat(companyProvider.existsByCnpj("1")).isTrue();
        companyProvider.deleteByCnpj("1");
        verify(companyRepository).deleteByCnpj("1");
    }

    @Test
    void documentProvider_shouldCoverMethodsAndErrors() {
        Document doc = mock(Document.class);
        DocumentEntity entity = mock(DocumentEntity.class);
        when(documentRepository.save(any())).thenReturn(entity);
        when(entity.toDomain()).thenReturn(doc);

        assertThat(documentProvider.save(doc)).isSameAs(doc);

        UUID docId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.of(entity));
        assertThat(documentProvider.findById(docId)).isSameAs(doc);
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentProvider.findById(docId)).isInstanceOf(ResourceNotFoundException.class);

        when(documentRepository.findVisibleToManager(eq(empId), any())).thenReturn(List.of(entity));
        when(documentRepository.findVisibleToEmployee(eq(empId), any())).thenReturn(List.of(entity));
        assertThat(documentProvider.findByEmployeeAndType(empId, DocumentType.DOCUMENTS, true)).hasSize(1);
        assertThat(documentProvider.findByEmployeeAndType(empId, DocumentType.DOCUMENTS, false)).hasSize(1);

        when(documentRepository.findVisibleToManagerByDate(any(), any(), any(), any())).thenReturn(List.of(entity));
        when(documentRepository.findVisibleToEmployeeByDate(any(), any(), any(), any())).thenReturn(List.of(entity));
        assertThat(documentProvider.findByEmployeeAndDateAndType(empId, LocalDate.now(), DocumentType.DOCUMENTS, true)).hasSize(1);
        assertThat(documentProvider.findByEmployeeAndDateAndType(empId, LocalDate.now(), DocumentType.DOCUMENTS, false)).hasSize(1);

        when(doc.documentId()).thenReturn(docId);
        when(documentRepository.findByDocumentIdAndEmployeeId(docId, empId)).thenReturn(Optional.of(entity));
        documentProvider.delete(empId, docId);
        verify(documentRepository).deleteById(docId);

        documentProvider.deleteByEmployeeId(empId);
        verify(documentRepository).deleteByEmployeeId(empId);

        when(documentRepository.findByTimeRecordId(10L)).thenReturn(List.of(entity));
        assertThat(documentProvider.findByTimeRecordId(10L)).hasSize(1);
        assertThat(documentProvider.findByTimeRecordIdIn(Set.of())).isEmpty();
        when(documentRepository.findByTimeRecordIdIn(Set.of(10L))).thenReturn(List.of(entity));
        assertThat(documentProvider.findByTimeRecordIdIn(Set.of(10L))).hasSize(1);

        when(documentRepository.existsByEmployeeIdAndType(empId, DocumentType.DOCUMENTS)).thenReturn(true);
        assertThat(documentProvider.existsByEmployeeIdAndType(empId, DocumentType.DOCUMENTS)).isTrue();

        assertThat(documentProvider.saveAll(List.of())).isEmpty();
        when(documentRepository.saveAll(anyList())).thenReturn(List.of(entity));
        assertThat(documentProvider.saveAll(List.of(doc))).hasSize(1);

        when(documentRepository.findTopByEmployeeIdAndTypeOrderByUploadedAtDesc(empId, DocumentType.DOCUMENTS)).thenReturn(Optional.of(entity));
        assertThat(documentProvider.findLatestByEmployeeIdAndType(empId, DocumentType.DOCUMENTS)).isSameAs(doc);
        when(documentRepository.findTopByEmployeeIdAndTypeOrderByUploadedAtDesc(empId, DocumentType.DOCUMENTS)).thenReturn(Optional.empty());
        assertThat(documentProvider.findLatestByEmployeeIdAndType(empId, DocumentType.DOCUMENTS)).isNull();

        when(documentRepository.findByDocumentIdAndEmployeeId(docId, empId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> documentProvider.findByIdAndEmployeeId(docId, empId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void employeeProvider_shouldCoverMethods() {
        Employee employee = mock(Employee.class);
        EmployeeEntity entity = mock(EmployeeEntity.class);
        when(employeeRepository.save(any())).thenReturn(entity);
        when(entity.toDomain()).thenReturn(employee);
        assertThat(employeeProvider.save(employee)).isSameAs(employee);

        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(employeeRepository.findByCpf("1")).thenReturn(Optional.of(entity));
        when(employeeRepository.existsByCpf("1")).thenReturn(true);
        when(employeeRepository.findAll()).thenReturn(List.of(entity));
        when(employeeRepository.findByCompanyId(companyId)).thenReturn(List.of(entity));
        when(employeeRepository.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(entity));
        when(employeeRepository.countByCompanyIdAndActive(companyId, true)).thenReturn(2L);
        when(employeeRepository.findByEmployeeIdIn(List.of(id))).thenReturn(List.of(entity));
        when(employeeRepository.countByCompanyIdsAndActive(List.of(companyId), true)).thenReturn(java.util.Collections.singletonList(new Object[]{companyId, 3L}));
        when(employeeRepository.findByIdForUpdate(id)).thenReturn(Optional.of(entity));
        when(employeeRepository.findCompanyIdByEmployeeId(id)).thenReturn(Optional.of(companyId));
        when(employeeRepository.updateActiveByCompanyId(companyId, false)).thenReturn(3);

        assertThat(employeeProvider.findById(id)).contains(employee);
        assertThat(employeeProvider.findByCpf("1")).contains(employee);
        assertThat(employeeProvider.cpfExists("1")).isTrue();
        assertThat(employeeProvider.findAll()).hasSize(1);
        employeeProvider.deleteById(id);
        assertThat(employeeProvider.findByCompanyId(companyId)).hasSize(1);
        assertThat(employeeProvider.findByCompanyIdAndActive(companyId, true)).hasSize(1);
        assertThat(employeeProvider.countByCompanyIdAndActive(companyId, true)).isEqualTo(2L);
        assertThat(employeeProvider.findByIdIn(List.of())).isEmpty();
        assertThat(employeeProvider.findByIdIn(List.of(id))).hasSize(1);
        assertThat(employeeProvider.countByCompanyIdsAndActive(List.of(), true)).isEmpty();
        assertThat(employeeProvider.countByCompanyIdsAndActive(List.of(companyId), true)).containsEntry(companyId, 3L);
        assertThat(employeeProvider.findByIdForUpdate(id)).contains(employee);
        assertThat(employeeProvider.findCompanyIdByEmployeeId(id)).contains(companyId);
        assertThat(employeeProvider.updateActiveByCompanyId(companyId, false)).isEqualTo(3);
    }

    @Test
    void messageNsrPasswordTimeApprovalUser_shouldCoverMethods() {
        Message m = mock(Message.class);
        MessageEntity me = mock(MessageEntity.class);
        when(messageRepository.findById(any())).thenReturn(Optional.of(me));
        when(messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of(me));
        when(me.toDomain()).thenReturn(m);
        messageProvider.save(m);
        messageProvider.saveAll(List.of(m));
        messageProvider.saveAll(List.of());
        assertThat(messageProvider.findById(UUID.randomUUID())).contains(m);
        assertThat(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(UUID.randomUUID(), UUID.randomUUID())).hasSize(1);
        messageProvider.deleteByMessageIdAndEmployeeId(UUID.randomUUID(), UUID.randomUUID());
        messageProvider.deleteByCreationDateBefore(LocalDateTime.now());

        UUID companyId = UUID.randomUUID();
        when(companyNsrRepository.incrementAndGetNsr(companyId)).thenReturn(9L);
        assertThat(nsrProvider.generateNextNsr(companyId)).isEqualTo(9L);

        UUID userId = UUID.randomUUID();
        String tk = passwordResetTokenProvider.generateAndSaveToken(userId);
        assertThat(tk).isNotBlank();
        PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
        tokenEntity.setUserId(userId);
        when(passwordResetTokenRepository.findByTokenAndExpiryDateAfter(eq("t"), any())).thenReturn(Optional.of(tokenEntity));
        assertThat(passwordResetTokenProvider.validateToken("t")).contains(userId);
        when(passwordResetTokenRepository.findByTokenAndExpiryDateAfter(eq("x"), any())).thenReturn(Optional.empty());
        assertThat(passwordResetTokenProvider.validateToken("x")).isEmpty();
        when(passwordResetTokenRepository.findById("t")).thenReturn(Optional.of(tokenEntity));
        passwordResetTokenProvider.deleteToken("t");
        verify(passwordResetTokenRepository).delete(tokenEntity);

        TimeRecordApprovalEntity ta = mock(TimeRecordApprovalEntity.class);
        TimeRecordApprovalRequest req = mock(TimeRecordApprovalRequest.class);
        when(ta.toDomain()).thenReturn(req);
        when(timeRecordApprovalRepository.findById(1L)).thenReturn(Optional.of(ta));
        Page<TimeRecordApprovalEntity> page = new PageImpl<>(List.of(ta));
        when(timeRecordApprovalRepository.findAllByCompanyId(any(), any(), isNull())).thenReturn(page);
        when(timeRecordApprovalRepository.findAllByCompanyId(any(), any(), contains("%joao%"))).thenReturn(page);
        timeRecordApprovalProvider.save(req);
        assertThat(timeRecordApprovalProvider.findByTimeRecordId(1L)).contains(req);
        assertThat(timeRecordApprovalProvider.findAllByCompanyId(PageRequest.of(0, 10), null, companyId).getContent()).hasSize(1);
        assertThat(timeRecordApprovalProvider.findAllByCompanyId(PageRequest.of(0, 10), "Joao", companyId).getContent()).hasSize(1);
        timeRecordApprovalProvider.deleteByTimeRecordId(1L);

        User user = mock(User.class);
        UserEntity ue = mock(UserEntity.class);
        when(ue.toDomain()).thenReturn(user);
        when(userRepository.save(any())).thenReturn(ue);
        when(userRepository.findByUsernameIgnoreCase("u")).thenReturn(Optional.of(ue));
        UUID uid = UUID.randomUUID();
        UUID eid = UUID.randomUUID();
        when(userRepository.findById(uid)).thenReturn(Optional.of(ue));
        when(userRepository.findAll()).thenReturn(List.of(ue));
        when(userRepository.findByActiveTrue()).thenReturn(List.of(ue));
        when(userRepository.findByActiveFalse()).thenReturn(List.of(ue));
        when(userRepository.findByEmployeeId(eid)).thenReturn(Optional.of(ue));
        when(userRepository.findByUserIdIn(List.of(uid))).thenReturn(List.of(ue));
        when(userRepository.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(ue));
        RecoverPasswordProjection proj = new RecoverPasswordProjection(uid, "u", "e@e");
        when(userRepository.findRecoverPasswordByCpfAndEmail("cpf", "mail")).thenReturn(Optional.of(proj));
        when(userRepository.existsByEmployeeId(eid)).thenReturn(true);
        when(userRepository.findByEmployeeIdIn(List.of(eid))).thenReturn(List.of(ue));

        userProvider.save(user);
        assertThat(userProvider.findByUsername("u")).contains(user);
        assertThat(userProvider.findById(uid)).contains(user);
        assertThat(userProvider.findAll()).hasSize(1);
        assertThat(userProvider.findByActive(true)).hasSize(1);
        assertThat(userProvider.findByActive(false)).hasSize(1);
        assertThat(userProvider.findByEmployeeId(eid)).contains(user);
        userProvider.deleteById(uid);
        assertThat(userProvider.findByIdIn(List.of())).isEmpty();
        assertThat(userProvider.findByIdIn(List.of(uid))).hasSize(1);
        assertThat(userProvider.findByCompanyIdAndActive(companyId, true)).hasSize(1);
        assertThat(userProvider.findRecoverPasswordCredentialsByCpfAndEmail("cpf", "mail"))
                .contains(new RecoverPasswordCredentials(uid, "u", "e@e"));
        assertThat(userProvider.existsByEmployeeId(eid)).isTrue();
        assertThat(userProvider.findByEmployeeIdIn(List.of())).isEmpty();
        assertThat(userProvider.findByEmployeeIdIn(List.of(eid))).hasSize(1);
    }
}
