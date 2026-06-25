package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.FaqCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FaqCategoryRepository extends JpaRepository<FaqCategoryEntity, UUID> {
}
