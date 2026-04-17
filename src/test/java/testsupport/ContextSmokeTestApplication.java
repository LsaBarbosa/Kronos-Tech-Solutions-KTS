package testsupport;

import com.kts.kronos.RekognitionSetup;
import com.kts.kronos.adapter.out.persistence.impl.EmailSenderProviderImpl;
import com.kts.kronos.config.SecurityConfig;
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
        EmailSenderProviderImpl.class,
        RekognitionSetup.class
})
public class ContextSmokeTestApplication {
}