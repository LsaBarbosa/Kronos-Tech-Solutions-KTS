package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AfdEntryEntity;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

public interface AfdEntryRepository extends JpaRepository<AfdEntryEntity, Long> {

    boolean existsByCompanyIdAndNsr(UUID companyId, Long nsr);

    @Query(
            value = """
                    SELECT current_hash
                    FROM tb_afd_entry
                    WHERE company_id = :companyId
                    ORDER BY nsr DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<String> findLastHashByCompanyId(@Param("companyId") UUID companyId);

    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "1000"))
    @Query("SELECT a FROM AfdEntryEntity a WHERE a.companyId = :companyId ORDER BY a.nsr ASC")
    Stream<AfdEntryEntity> streamAllByCompanyIdOrderByNsrAsc(@Param("companyId") UUID companyId);
}