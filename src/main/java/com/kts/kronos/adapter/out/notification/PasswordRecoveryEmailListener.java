package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.adapter.in.web.dto.event.PasswordRecoveryEmailRequestedEvent;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordRecoveryEmailListener {

    private final EmailSenderProvider emailSenderProvider;
    private final RetryTemplate emailRetryTemplate;
    private final PasswordRecoveryDeadLetterQueue deadLetterQueue;
    private final MeterRegistry meterRegistry;

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordRecoveryRequested(PasswordRecoveryEmailRequestedEvent event) {
        var sample = Timer.start(meterRegistry);

        try {
            emailRetryTemplate.execute(context -> {
                emailSenderProvider.sendResetEmail(
                        event.toEmail(),
                        event.resetToken(),
                        event.username(),
                        event.frontendUrl()
                );
                return null;
            });
            meterRegistry.counter("auth.password.recovery.email.total", "status", "sent").increment();
            sample.stop(meterRegistry.timer("auth.password.recovery.email.duration", "status", "sent"));
        } catch (Exception ex) {
            meterRegistry.counter("auth.password.recovery.email.total", "status", "dlq").increment();
            sample.stop(meterRegistry.timer("auth.password.recovery.email.duration", "status", "dlq"));
            deadLetterQueue.enqueue(event, ex.getMessage());
        }
    }
}
