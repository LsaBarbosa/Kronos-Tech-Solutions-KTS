package com.kts.kronos.adapter.in.web.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlatformHealthController {

    @GetMapping({"/", "/healthz"})
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}
