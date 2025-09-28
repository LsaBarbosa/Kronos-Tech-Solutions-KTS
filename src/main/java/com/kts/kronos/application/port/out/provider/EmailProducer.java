package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.adapter.in.messaging.dto.PasswordResetMessage;

public interface EmailProducer {
    void sendPasswordResetEmail(PasswordResetMessage message);

}
