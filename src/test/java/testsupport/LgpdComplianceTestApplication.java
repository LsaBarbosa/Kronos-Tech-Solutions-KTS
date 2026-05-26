package testsupport;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import static org.mockito.Mockito.mock;

@SpringBootApplication
@ComponentScan(
        basePackages = {"com.kts.kronos", "testsupport"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*\\$.*TestConfiguration"
        )
)
@EntityScan("com.kts.kronos")
@EnableJpaRepositories("com.kts.kronos.adapter.out.persistence")
@Import({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@ActiveProfiles("test")
public class LgpdComplianceTestApplication {

    @Bean
    public JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public com.kts.kronos.adapter.out.notification.EmailNotificationProviderImpl emailNotificationProvider(
            JavaMailSender mailSender) {
        return new com.kts.kronos.adapter.out.notification.EmailNotificationProviderImpl(mailSender);
    }
}
