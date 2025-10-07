package com.kts.kronos.adapter.in.web.dto.message.publisher;

import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;
import com.google.cloud.spring.pubsub.core.PubSubTemplate; // Import do GCP
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.kts.kronos.constants.Messages.TIME_RECORD_APPROVAL_TOPIC;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeRecordChangePublisher {
    private final PubSubTemplate pubSubTemplate; // Injeta o PubSubTemplate


    public void publishApprovalRequest(TimeRecordChangeRequestMessage message) {
        try {
            // Publica a mensagem. O Spring Cloud GCP a serializa para JSON/Bytes.
            pubSubTemplate.publish(TIME_RECORD_APPROVAL_TOPIC, message);
            log.info("Solicitação de alteração de ponto ID {} publicada no tópico {}",
                    message.timeRecordId(), TIME_RECORD_APPROVAL_TOPIC);
        } catch (Exception e) {
            log.error("Falha ao publicar a mensagem no Pub/Sub para o registro ID {}: {}",
                    message.timeRecordId(), e.getMessage());
            throw new RuntimeException("Falha na comunicação com o serviço de mensageria.", e);
        }
    }
}