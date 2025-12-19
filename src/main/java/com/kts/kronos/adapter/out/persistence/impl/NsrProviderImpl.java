package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.CompanyNsrRepository;
import com.kts.kronos.application.port.out.provider.NsrProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NsrProviderImpl implements NsrProvider {

    private final CompanyNsrRepository repository;

    @Override
    // Propagation.MANDATORY exige que já exista uma transação aberta (do TimeRecordService)
    // para garantir que o NSR só seja consumido se o ponto for salvo.
    @Transactional(propagation = Propagation.MANDATORY) 
    public Long generateNextNsr(UUID companyId) {
        return repository.incrementAndGetNsr(companyId);
    }
}