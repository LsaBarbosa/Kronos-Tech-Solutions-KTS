package testsupport;

import com.kts.kronos.KronosApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = {
        MailSenderAutoConfiguration.class
})
public class LgpdComplianceTestApplication extends KronosApplication {
}
