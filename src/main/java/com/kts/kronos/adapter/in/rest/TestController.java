package com.kts.kronos.adapter.in.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    // Exige só autenticação (qualquer role)
    @GetMapping("/any")
    public ResponseEntity<String> any() {
        return ResponseEntity.ok("OK autenticação válida");
    }

    // Exige papel específico (por exemplo, PARTNER)
    @PreAuthorize("hasRole('PARTNER')")
    @GetMapping("/partner")
    public ResponseEntity<String> partner() {
        return ResponseEntity.ok("OK PARTNER");
    }
}
