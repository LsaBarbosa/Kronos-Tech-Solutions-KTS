package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.inventory.CreateInventoryRequest;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.DataProcessingInventoryProvider;
import com.kts.kronos.domain.model.DataProcessingInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataProcessingInventoryServiceTest {

    @Mock private DataProcessingInventoryProvider provider;

    private DataProcessingInventoryService service;

    @BeforeEach
    void setUp() {
        service = new DataProcessingInventoryService(provider);
    }

    private CreateInventoryRequest makeRequest() {
        return new CreateInventoryRequest(
                "PROC-001", "Processo Teste", "Descrição",
                "PESSOAL", "nome,cpf", "FUNCIONARIO",
                "RH", "LGPD_ART6_I", false,
                "ERP", "/storage", null, null,
                false, "TLS/AES256", true,
                "BAIXO", false, "1.0", null
        );
    }

    private DataProcessingInventory makeInventory(UUID id) {
        var now = Instant.now();
        return new DataProcessingInventory(
                id, "PROC-001", "Processo", "Desc",
                "PESSOAL", "nome", "FUNC", "RH",
                "LGPD", false, "ERP", null, null,
                null, false, null, true, null, false,
                "1.0", null, now, now
        );
    }

    @Test
    void deveCriarInventarioComSucesso() {
        var request = makeRequest();
        var saved = makeInventory(UUID.randomUUID());
        when(provider.save(any())).thenReturn(saved);

        var result = service.createInventory(request);

        assertNotNull(result);
        verify(provider).save(any(DataProcessingInventory.class));
    }

    @Test
    void deveCriarInventarioPreservandoCamposDaRequest() {
        var request = makeRequest();
        var saved = makeInventory(UUID.randomUUID());
        when(provider.save(any())).thenReturn(saved);

        service.createInventory(request);

        var captor = org.mockito.ArgumentCaptor.forClass(DataProcessingInventory.class);
        verify(provider).save(captor.capture());
        var inventory = captor.getValue();
        assertNull(inventory.inventoryId());
        assertEquals("PROC-001", inventory.processCode());
        assertNotNull(inventory.createdAt());
        assertNotNull(inventory.updatedAt());
    }

    @Test
    void deveAtualizarInventarioExistente() {
        var id = UUID.randomUUID();
        var existing = makeInventory(id);
        var request = makeRequest();
        var updated = makeInventory(id);
        when(provider.findById(id)).thenReturn(Optional.of(existing));
        when(provider.save(any())).thenReturn(updated);

        var result = service.updateInventory(id, request);

        assertNotNull(result);
        verify(provider).findById(id);
        verify(provider).save(any());
    }

    @Test
    void deveAtualizarInventarioPreservandoCreatedAt() {
        var id = UUID.randomUUID();
        var existing = makeInventory(id);
        when(provider.findById(id)).thenReturn(Optional.of(existing));
        when(provider.save(any())).thenReturn(existing);

        service.updateInventory(id, makeRequest());

        var captor = org.mockito.ArgumentCaptor.forClass(DataProcessingInventory.class);
        verify(provider).save(captor.capture());
        assertEquals(existing.createdAt(), captor.getValue().createdAt());
        assertEquals(id, captor.getValue().inventoryId());
    }

    @Test
    void deveLancarExcecaoAoAtualizarInventarioInexistente() {
        var id = UUID.randomUUID();
        when(provider.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateInventory(id, makeRequest()));
    }

    @Test
    void deveRetornarInventarioPorIdComSucesso() {
        var id = UUID.randomUUID();
        var inventory = makeInventory(id);
        when(provider.findById(id)).thenReturn(Optional.of(inventory));

        var result = service.getInventoryById(id);

        assertNotNull(result);
        assertEquals(id, result.inventoryId());
    }

    @Test
    void deveLancarExcecaoAoBuscarInventarioInexistentePorId() {
        var id = UUID.randomUUID();
        when(provider.findById(id)).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getInventoryById(id));
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    @Test
    void deveRetornarInventarioPorProcessCodeComSucesso() {
        var inventory = makeInventory(UUID.randomUUID());
        when(provider.findByProcessCode("PROC-001")).thenReturn(Optional.of(inventory));

        var result = service.getInventoryByProcessCode("PROC-001");

        assertNotNull(result);
    }

    @Test
    void deveLancarExcecaoAoBuscarInventarioPorProcessCodeInexistente() {
        when(provider.findByProcessCode("INEXISTENTE")).thenReturn(Optional.empty());

        var ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getInventoryByProcessCode("INEXISTENTE"));
        assertTrue(ex.getMessage().contains("INEXISTENTE"));
    }

    @Test
    void deveListarTodosInventarios() {
        var pageable = Pageable.ofSize(10);
        Page<DataProcessingInventory> page = new PageImpl<>(List.of(makeInventory(UUID.randomUUID())));
        when(provider.findAll(pageable)).thenReturn(page);

        var result = service.listAllInventories(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void deveListarInventariosAtivos() {
        var pageable = Pageable.ofSize(10);
        Page<DataProcessingInventory> page = new PageImpl<>(List.of(makeInventory(UUID.randomUUID())));
        when(provider.findAllActive(pageable)).thenReturn(page);

        var result = service.listActiveInventories(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void deveDeletarInventarioPorId() {
        var id = UUID.randomUUID();
        doNothing().when(provider).deleteById(id);

        service.deleteInventory(id);

        verify(provider).deleteById(id);
    }
}
