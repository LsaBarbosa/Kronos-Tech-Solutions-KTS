package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.DataProcessingInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DataProcessingInventoryProvider {
    DataProcessingInventory save(DataProcessingInventory inventory);
    Optional<DataProcessingInventory> findById(UUID inventoryId);
    Optional<DataProcessingInventory> findByProcessCode(String processCode);
    Page<DataProcessingInventory> findAll(Pageable pageable);
    Page<DataProcessingInventory> findAllActive(Pageable pageable);
    void deleteById(UUID inventoryId);
}
