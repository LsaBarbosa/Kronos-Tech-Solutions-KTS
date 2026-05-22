package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataProcessingInventoryRepository extends JpaRepository<DataProcessingInventoryEntity, UUID> {
    Optional<DataProcessingInventoryEntity> findByProcessCode(String processCode);
    Page<DataProcessingInventoryEntity> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    Page<DataProcessingInventoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
