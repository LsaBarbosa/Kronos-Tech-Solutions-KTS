package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinResponse;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.application.port.in.usecase.TerminalCheckinUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kts.kronos.constants.ApiPaths.AUTH;
import static com.kts.kronos.constants.ApiPaths.TERMINAL_CHECKIN;

@RestController
@RequestMapping(AUTH)
@RequiredArgsConstructor
public class TerminalCheckinController {

    private final TerminalCheckinUseCase terminalCheckinUseCase;
    private final AuthCookieService authCookieService;

    @PostMapping(TERMINAL_CHECKIN)
    public ResponseEntity<TerminalCheckinResponse> checkinByFace(
            @Valid @RequestBody TerminalCheckinRequest request
    ) {
        var result = terminalCheckinUseCase.checkinByFace(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(result.jwtToken()).toString())
                .body(result.checkinResponse());
    }
}
