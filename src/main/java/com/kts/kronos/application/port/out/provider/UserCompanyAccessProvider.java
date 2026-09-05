package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.UserCompanyAccess;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCompanyAccessProvider {

    UserCompanyAccess save(UserCompanyAccess access);

    List<UserCompanyAccess> findActiveByUserId(UUID userId);

    Optional<UserCompanyAccess> findActiveByUserIdAndCompanyId(UUID userId, UUID companyId);

    Optional<UserCompanyAccess> findDefaultActiveByUserId(UUID userId);

    boolean existsActiveByUserIdAndCompanyId(UUID userId, UUID companyId);

    List<UserCompanyAccess> findActiveByCompanyId(UUID companyId);
}
