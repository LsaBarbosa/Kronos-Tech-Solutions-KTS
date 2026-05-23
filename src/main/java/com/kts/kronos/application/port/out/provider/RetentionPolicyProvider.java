package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.RetentionPolicy;

import java.util.List;

public interface RetentionPolicyProvider {
    RetentionPolicy save(RetentionPolicy policy);

    List<RetentionPolicy> findEnabledPolicies();

    List<RetentionPolicy> findAll();

    RetentionPolicy findByCode(String policyCode);
}
