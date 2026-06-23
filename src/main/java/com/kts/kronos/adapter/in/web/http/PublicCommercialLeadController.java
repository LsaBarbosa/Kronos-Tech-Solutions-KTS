package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.public_commercial.CommercialLeadRequest;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.CommercialLeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/commercial-leads")
@RequiredArgsConstructor
public class PublicCommercialLeadController {

    private final CommercialLeadService commercialLeadService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    public ResponseEntity<Void> submitLead(
            @Valid @RequestBody CommercialLeadRequest request,
            HttpServletRequest httpServletRequest
    ) {
        commercialLeadService.submitLead(request, clientIpResolver.resolve(httpServletRequest));
        return ResponseEntity.noContent().build();
    }
}
