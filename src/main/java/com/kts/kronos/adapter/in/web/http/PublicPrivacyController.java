package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.public_privacy.PublicBiometricTermResponse;
import com.kts.kronos.adapter.in.web.dto.public_privacy.PublicPrivacyPolicyResponse;
import com.kts.kronos.adapter.in.web.dto.public_privacy.PublicProcessingCatalogResponse;
import com.kts.kronos.application.service.PublicPrivacyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/privacy")
@RequiredArgsConstructor
public class PublicPrivacyController {
    private final PublicPrivacyService publicPrivacyService;

    @GetMapping("/processing-catalog")
    public ResponseEntity<PublicProcessingCatalogResponse> getProcessingCatalog() {
        return ResponseEntity.ok(publicPrivacyService.getPublicProcessingCatalog());
    }

    @GetMapping("/policy")
    public ResponseEntity<PublicPrivacyPolicyResponse> getPrivacyPolicy() {
        return ResponseEntity.ok(publicPrivacyService.getPublicPrivacyPolicy());
    }

    @GetMapping("/biometric-term")
    public ResponseEntity<PublicBiometricTermResponse> getBiometricTerm() {
        return ResponseEntity.ok(publicPrivacyService.getPublicBiometricTerm());
    }
}
