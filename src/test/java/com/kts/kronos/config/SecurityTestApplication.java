package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.in.web.exceptions.JsonAccessDeniedHandler;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.AuthController;
import com.kts.kronos.adapter.in.web.http.CompanyController;
import com.kts.kronos.observability.adapter.in.web.ObservabilityController;
import com.kts.kronos.observability.adapter.in.web.CorrelationIdFilter;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        MailSenderAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
	        DelegatedAuthenticationEntryPoint.class,
	        JsonAccessDeniedHandler.class,
	        RestExceptionHandler.class,
	        AuthCookieService.class,
	        AuthController.class,
        CompanyController.class,
        ObservabilityController.class,
        CorrelationIdFilter.class
})
class SecurityTestApplication {
}
