package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestEntity;
import com.kts.kronos.adapter.out.persistence.mapper.LgpdRequestMapper;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LgpdRequestProviderImplTest {

    @Mock
    private LgpdRequestRepository repository;

    @Mock
    private LgpdRequestMapper mapper;

    @InjectMocks
    private LgpdRequestProviderImpl provider;

    @Test
    void shouldSaveRequest() {
        LgpdRequest domain = mock(LgpdRequest.class);
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        when(mapper.toEntity(domain)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(domain);

        LgpdRequest result = provider.save(domain);

        assertSame(domain, result);
        verify(repository).save(entity);
    }

    @Test
    void shouldFindByIdWhenPresent() {
        UUID id = UUID.randomUUID();
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<LgpdRequest> result = provider.findById(id);

        assertTrue(result.isPresent());
        assertSame(domain, result.get());
    }

    @Test
    void shouldReturnEmptyWhenIdNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<LgpdRequest> result = provider.findById(id);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindByEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        when(repository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<LgpdRequest> result = provider.findByEmployeeId(employeeId);

        assertEquals(1, result.size());
        assertSame(domain, result.get(0));
    }

    @Test
    void shouldFindByCompanyId() {
        UUID companyId = UUID.randomUUID();
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        when(repository.findByCompanyIdOrderByCreatedAtDesc(companyId)).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<LgpdRequest> result = provider.findByCompanyId(companyId);

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindAll() {
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<LgpdRequest> result = provider.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindByCompanyIdWithPageable() {
        UUID companyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        Page<LgpdRequestEntity> entityPage = new PageImpl<>(List.of(entity));
        when(repository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)).thenReturn(entityPage);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Page<LgpdRequest> result = provider.findByCompanyId(companyId, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindAllWithPageable() {
        Pageable pageable = PageRequest.of(0, 10);
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        Page<LgpdRequestEntity> entityPage = new PageImpl<>(List.of(entity));
        when(repository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(entityPage);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Page<LgpdRequest> result = provider.findAll(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void shouldFindAdminRequestsWithNullEmployeeName() {
        UUID companyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<LgpdRequestEntity> entityPage = new PageImpl<>(List.of());
        when(repository.findAdminRequests(eq(companyId), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(entityPage);

        Page<LgpdRequest> result = provider.findAdminRequests(companyId, null, null, null, pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldFindAdminRequestsWithBlankEmployeeName() {
        UUID companyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<LgpdRequestEntity> entityPage = new PageImpl<>(List.of());
        when(repository.findAdminRequests(eq(companyId), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(entityPage);

        // Blank employee name → treated as null
        Page<LgpdRequest> result = provider.findAdminRequests(companyId, null, null, "   ", pageable);

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void shouldFindAdminRequestsWithEmployeeNameFilter() {
        UUID companyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        LgpdRequestEntity entity = mock(LgpdRequestEntity.class);
        LgpdRequest domain = mock(LgpdRequest.class);
        Page<LgpdRequestEntity> entityPage = new PageImpl<>(List.of(entity));
        // trimmed lowercase with % wrapping
        when(repository.findAdminRequests(eq(companyId), eq(LgpdRequestType.ACCESS),
                eq(LgpdRequestStatus.OPEN), eq("%john%"), eq(pageable)))
                .thenReturn(entityPage);
        when(mapper.toDomain(entity)).thenReturn(domain);

        Page<LgpdRequest> result = provider.findAdminRequests(
                companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN, "John", pageable);

        assertEquals(1, result.getTotalElements());
    }
}
