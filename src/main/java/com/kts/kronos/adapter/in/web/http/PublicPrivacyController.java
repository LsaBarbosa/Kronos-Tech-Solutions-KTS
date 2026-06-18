package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.public_privacy.PublicBiometricTermResponse;
import com.kts.kronos.adapter.in.web.dto.public_privacy.PublicPrivacyPolicyResponse;
import com.kts.kronos.adapter.in.web.dto.public_privacy.PublicProcessingCatalogResponse;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.service.PublicPrivacyService;
import com.kts.kronos.infrastructure.redis.RedisCacheNames;
import com.kts.kronos.infrastructure.redis.RedisScopeKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/public/privacy")
@RequiredArgsConstructor
public class PublicPrivacyController {
    private final PublicPrivacyService publicPrivacyService;

    @Autowired(required = false)
    private CacheProvider cacheProvider;

    @GetMapping("/processing-catalog")
    public ResponseEntity<PublicProcessingCatalogResponse> getProcessingCatalog() {
        return ResponseEntity.ok(cache(
                RedisCacheNames.PUBLIC_PROCESSING_CATALOG,
                RedisScopeKeyResolver.publicScope("processing-catalog"),
                PublicProcessingCatalogResponse.class,
                publicPrivacyService::getPublicProcessingCatalog
        ));
    }

    @GetMapping("/policy")
    public ResponseEntity<PublicPrivacyPolicyResponse> getPrivacyPolicy() {
        return ResponseEntity.ok(cache(
                RedisCacheNames.PUBLIC_PRIVACY_POLICY,
                RedisScopeKeyResolver.publicScope("policy"),
                PublicPrivacyPolicyResponse.class,
                publicPrivacyService::getPublicPrivacyPolicy
        ));
    }

    @GetMapping("/biometric-term")
    public ResponseEntity<PublicBiometricTermResponse> getBiometricTerm() {
        return ResponseEntity.ok(cache(
                RedisCacheNames.PUBLIC_BIOMETRIC_TERM,
                RedisScopeKeyResolver.publicScope("biometric-term"),
                PublicBiometricTermResponse.class,
                publicPrivacyService::getPublicBiometricTerm
        ));
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
