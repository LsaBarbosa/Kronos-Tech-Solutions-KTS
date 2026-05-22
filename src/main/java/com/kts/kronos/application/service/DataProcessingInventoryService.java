package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.inventory.CreateInventoryRequest;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.DataProcessingInventoryProvider;
import com.kts.kronos.domain.model.DataProcessingInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DataProcessingInventoryService {
    private final DataProcessingInventoryProvider provider;

    public DataProcessingInventory createInventory(CreateInventoryRequest request) {
        Instant now = Instant.now();
        DataProcessingInventory inventory = new DataProcessingInventory(
                null,
                request.processCode(),
                request.processName(),
                request.description(),
                request.dataCategory(),
                request.dataFields(),
                request.dataSubjectCategory(),
                request.purpose(),
                request.legalBasis(),
                request.sensitiveData(),
                request.sourceSystem(),
                request.storageLocation(),
                request.retentionPolicyCode(),
                request.externalSharing(),
                request.internationalTransfer(),
                request.securityMeasures(),
                request.active(),
                request.riskLevel(),
                request.ripdRequired(),
                request.version(),
                request.operators(),
                now,
                now
        );
        return provider.save(inventory);
    }

    public DataProcessingInventory updateInventory(UUID inventoryId, CreateInventoryRequest request) {
        DataProcessingInventory existing = provider.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventário não encontrado: " + inventoryId));

        DataProcessingInventory updated = new DataProcessingInventory(
                existing.inventoryId(),
                request.processCode(),
                request.processName(),
                request.description(),
                request.dataCategory(),
                request.dataFields(),
                request.dataSubjectCategory(),
                request.purpose(),
                request.legalBasis(),
                request.sensitiveData(),
                request.sourceSystem(),
                request.storageLocation(),
                request.retentionPolicyCode(),
                request.externalSharing(),
                request.internationalTransfer(),
                request.securityMeasures(),
                request.active(),
                request.riskLevel(),
                request.ripdRequired(),
                request.version(),
                request.operators(),
                existing.createdAt(),
                Instant.now()
        );

        return provider.save(updated);
    }

    @Transactional(readOnly = true)
    public DataProcessingInventory getInventoryById(UUID inventoryId) {
        return provider.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventário não encontrado: " + inventoryId));
    }

    @Transactional(readOnly = true)
    public DataProcessingInventory getInventoryByProcessCode(String processCode) {
        return provider.findByProcessCode(processCode)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado: " + processCode));
    }

    @Transactional(readOnly = true)
    public Page<DataProcessingInventory> listAllInventories(Pageable pageable) {
        return provider.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<DataProcessingInventory> listActiveInventories(Pageable pageable) {
        return provider.findAllActive(pageable);
    }

    public void deleteInventory(UUID inventoryId) {
        provider.deleteById(inventoryId);
    }
}
