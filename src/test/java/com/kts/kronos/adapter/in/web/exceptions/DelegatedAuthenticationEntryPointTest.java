package com.kts.kronos.adapter.in.web.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.*;

class DelegatedAuthenticationEntryPointTest {

    @Test
    void shouldDelegateToHandlerExceptionResolver() throws Exception {
        var entryPoint = new DelegatedAuthenticationEntryPoint();
        var resolver = mock(HandlerExceptionResolver.class);
        ReflectionTestUtils.setField(entryPoint, "resolver", resolver);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException ex = new BadCredentialsException("bad");

        entryPoint.commence(request, response, ex);

        verify(resolver).resolveException(request, response, null, ex);
    }
}
