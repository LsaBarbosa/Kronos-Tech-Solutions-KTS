package testsupport;

import com.kts.kronos.RekognitionSetup;
import com.kts.kronos.adapter.in.web.exceptions.JsonAccessDeniedHandler;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.persistence.impl.EmailSenderProviderImpl;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.config.SecurityConfig;
import com.kts.kronos.observability.web.CorrelationIdFilter;
import com.kts.kronos.observability.adapter.in.web.ObservabilityController;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.application.impl.ObservabilityStatusUseCaseImpl;
import com.kts.kronos.observability.support.ObservabilityTagSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import static org.mockito.Mockito.mock;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        MailSenderAutoConfiguration.class
})
	@Import({
	        SecurityConfig.class,
	        JsonAccessDeniedHandler.class,
	        AuthCookieService.class,
	        EmailSenderProviderImpl.class,
        RekognitionSetup.class,
        CorrelationIdFilter.class,
        ObservabilityController.class,
        ObservabilityStatusUseCaseImpl.class,
        ObservabilityTagSanitizer.class
})
public class ContextSmokeTestApplication {

	@Bean
	public UserProvider userProvider() {
		return mock(UserProvider.class);
	}

	@Bean
	public TokenBlacklistProvider tokenBlacklistProvider() {
		return mock(TokenBlacklistProvider.class);
	}

	@Bean
	public PrivacyLogReferenceService privacyLogReferenceService() {
		return new PrivacyLogReferenceService("test-lgpd-log-secret");
	}

    @Bean
    public KronosMetrics kronosMetrics(
            MeterRegistry meterRegistry,
            ObservabilityTagSanitizer observabilityTagSanitizer
    ) {
        return new KronosMetrics(meterRegistry, observabilityTagSanitizer);
    }

    @Bean
    public KronosTracing kronosTracing(
            ObservationRegistry observationRegistry,
            ObservabilityTagSanitizer observabilityTagSanitizer
    ) {
        return new KronosTracing(observationRegistry, observabilityTagSanitizer);
    }
}
