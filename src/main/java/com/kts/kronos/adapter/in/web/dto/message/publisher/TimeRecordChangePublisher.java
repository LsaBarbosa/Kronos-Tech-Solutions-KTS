package com.kts.kronos.adapter.in.web.dto.message.publisher;

import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;


import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeRecordChangePublisher {
    private final SnsTemplate snsTemplate;

    // Injeta o ARN do Tópico da configuração
    @Value("${time-record.approval-topic-arn}")
    private String timeRecordApprovalTopicArn;

    public void publishApprovalRequest(TimeRecordChangeRequestMessage message) {
        try {
            // Publica a mensagem no ARN do tópico. O Spring Cloud AWS faz a serialização JSON.
            snsTemplate.convertAndSend(timeRecordApprovalTopicArn, message);

            log.info("Solicitação de alteração de ponto ID {} publicada no tópico SNS {}",
                    message.timeRecordId(), timeRecordApprovalTopicArn);
        } catch (Exception e) {
            log.error("Falha ao publicar a mensagem no AWS SNS para o registro ID {}: {}",
                    message.timeRecordId(), e.getMessage());
            throw new RuntimeException("Falha na comunicação com o serviço de mensageria.", e);
        }
    }
}