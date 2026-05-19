package testsupport;

import com.kts.kronos.RekognitionSetup;
import com.kts.kronos.adapter.in.web.exceptions.JsonAccessDeniedHandler;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.persistence.impl.EmailSenderProviderImpl;
import com.kts.kronos.config.SecurityConfig;
import com.kts.kronos.observability.adapter.in.web.CorrelationIdFilter;
import com.kts.kronos.observability.adapter.in.web.ObservabilityController;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.application.impl.ObservabilityStatusUseCaseImpl;
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
	        JsonAccessDeniedHandler.class,
	        AuthCookieService.class,
	        EmailSenderProviderImpl.class,
        RekognitionSetup.class,
        CorrelationIdFilter.class,
        ObservabilityController.class,
        ObservabilityStatusUseCaseImpl.class,
        KronosMetrics.class,
        KronosTracing.class
})
public class ContextSmokeTestApplication {
}
