package com.kts.kronos.adapter.in.web.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;

class SensitiveDataLoggingFilterTest {

    @Test
    void doFilter_withNonNullQueryString_coversNonNullBranch() throws ServletException, IOException {
        SensitiveDataLoggingFilter filter = new SensitiveDataLoggingFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setQueryString("param=value&other=123"); // non-null → covers BR L28 TRUE
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_withNullQueryString_coversNullBranch() throws ServletException, IOException {
        SensitiveDataLoggingFilter filter = new SensitiveDataLoggingFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        // queryString is null by default → covers BR L28 FALSE
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
