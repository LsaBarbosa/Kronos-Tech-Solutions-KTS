package com.kts.kronos.adapter.in.web.exceptions;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DelegatedAuthenticationEntryPointCoverageTest {

    @Test
    void commence_resolverReturnsNull_responseNotCommitted_sendError() throws Exception {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(null);

        DelegatedAuthenticationEntryPoint entryPoint = new DelegatedAuthenticationEntryPoint();
        ReflectionTestUtils.setField(entryPoint, "resolver", resolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void commence_resolverReturnsModelAndView_doesNotSendError() throws Exception {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        when(resolver.resolveException(any(), any(), isNull(), any()))
                .thenReturn(new ModelAndView());

        DelegatedAuthenticationEntryPoint entryPoint = new DelegatedAuthenticationEntryPoint();
        ReflectionTestUtils.setField(entryPoint, "resolver", resolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));

        assertEquals(200, response.getStatus()); // default, sendError not called
    }

    @Test
    void commence_resolverReturnsNull_responseAlreadyCommitted_noSendError() throws Exception {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(null);

        DelegatedAuthenticationEntryPoint entryPoint = new DelegatedAuthenticationEntryPoint();
        ReflectionTestUtils.setField(entryPoint, "resolver", resolver);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.flushBuffer(); // marks response as committed

        entryPoint.commence(request, response, new BadCredentialsException("Unauthorized"));

        // response was already committed — sendError must NOT have been called
        assertEquals(200, response.getStatus());
    }

}