package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DataProcessingInventoryRepository;
import com.kts.kronos.adapter.out.persistence.mapper.DataProcessingInventoryMapper;
import com.kts.kronos.application.port.out.provider.DataProcessingInventoryProvider;
import com.kts.kronos.domain.model.DataProcessingInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataProcessingInventoryProviderImpl implements DataProcessingInventoryProvider {
    private final DataProcessingInventoryRepository repository;
    private final DataProcessingInventoryMapper mapper;

    @Override
    public DataProcessingInventory save(DataProcessingInventory inventory) {
        var entity = mapper.toEntity(inventory);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<DataProcessingInventory> findById(UUID inventoryId) {
        return repository.findById(inventoryId).map(mapper::toDomain);
    }

    @Override
    public Optional<DataProcessingInventory> findByProcessCode(String processCode) {
        return repository.findByProcessCode(processCode).map(mapper::toDomain);
    }

    @Override
    public Page<DataProcessingInventory> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<DataProcessingInventory> findAllActive(Pageable pageable) {
        return repository.findByActiveTrueOrderByCreatedAtDesc(pageable).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID inventoryId) {
        repository.deleteById(inventoryId);
    }
}
