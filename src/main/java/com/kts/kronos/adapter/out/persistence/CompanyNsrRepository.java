package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.CompanyNsrEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyNsrRepository extends JpaRepository<CompanyNsrEntity, UUID> {

    // Retorna o NOVO valor já incrementado. 
    // É atômico: o banco trava essa linha até o commit da transação.
    @Query(value = """
        INSERT INTO tb_company_nsr (company_id, last_nsr) 
        VALUES (:companyId, 1) 
        ON CONFLICT (company_id) 
        DO UPDATE SET last_nsr = tb_company_nsr.last_nsr + 1 
        RETURNING last_nsr
    """, nativeQuery = true)
    Long incrementAndGetNsr(@Param("companyId") UUID companyId);
}