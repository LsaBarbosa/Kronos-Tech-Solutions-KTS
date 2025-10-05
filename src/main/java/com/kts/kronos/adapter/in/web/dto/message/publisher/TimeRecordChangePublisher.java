package com.kts.kronos.adapter.in.web.dto.message.publisher;

import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;
import com.google.cloud.spring.pubsub.core.PubSubTemplate; // Import do GCP
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeRecordChangePublisher {
    private final PubSubTemplate pubSubTemplate; // Injeta o PubSubTemplate

    @Value("${pubsub.topics.time-record-approval}")
    private String timeRecordApprovalTopic;

    public void publishApprovalRequest(TimeRecordChangeRequestMessage message) {
        try {
            pubSubTemplate.publish(timeRecordApprovalTopic, message); // Usa o PubSubTemplate

            log.info("Solicitação de alteração de ponto ID {} publicada no tópico Pub/Sub {}",
                    message.timeRecordId(), timeRecordApprovalTopic);
        } catch (Exception e) {
            log.error("Falha ao publicar a mensagem no Google Cloud Pub/Sub para o registro ID {}: {}",
                    message.timeRecordId(), e.getMessage());
            throw new RuntimeException("Falha na comunicação com o serviço de mensageria.", e);
        }
    }
}