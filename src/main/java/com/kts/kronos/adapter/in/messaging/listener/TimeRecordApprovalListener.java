package com.kts.kronos.adapter.in.messaging.listener;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeRecordApprovalListener {

    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider timeRecordProvider;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME);

    // NOVO: Usa @SqsListener e a propriedade do application.yml com o nome da fila

    @ServiceActivator(inputChannel = "timeRecordApprovalInputChannel")
    public void handleTimeRecordChangeRequest(
            TimeRecordChangeRequestMessage payload,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) { // Assinatura simplificada

        log.info("Recebida solicitação de alteração de ponto para o registro ID: {}", payload.timeRecordId());

        try {
            // 1. Lógica de busca de dados e montagem de notificação (mantida)
            var partnerEmployee = employeeProvider.findById(payload.partnerEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException(PARTNER_NOT_FOUND + payload.partnerEmployeeId()));

            var managerUser = userProvider.findById(payload.managerId())
                    .orElseThrow(() -> new IllegalArgumentException(USER_MANAGER_NOT_FOUND + payload.managerId()));

            var managerEmployee = employeeProvider.findById(managerUser.employeeId())
                    .orElseThrow(() -> new IllegalArgumentException(MANAGER_NOT_FOUND + managerUser.employeeId()));

            var timeRecord = timeRecordProvider.findById(payload.timeRecordId())
                    .orElseThrow(() -> new IllegalArgumentException(RECORD_NOT_FOUND + payload.timeRecordId()));

            String notificationMessage = String.format(
                    // ... (Mensagem de notificação idêntica à anterior) ...
                    "\n\n" +
                            "--- NOTIFICAÇÃO PARA O MANAGER ---\n" +
                            "De: %s\n" +
                            "Para: %s (Email: %s)\n" +
                            "Assunto: Aprovação de Alteração de Ponto\n\n" +
                            "O colaborador '%s' solicitou uma alteração no registro de ponto ID %d.\n\n" +
                            "Valores Atuais:\n" +
                            " - Entrada: %s\n" +
                            " - Saída:   %s\n\n" +
                            "Valores Solicitados:\n" +
                            " - Nova Entrada: %s\n" +
                            " - Nova Saída:   %s\n\n" +
                            "Para aprovar ou reprovar, acesse o sistema.\n" +
                            "----------------------------------\n",
                    partnerEmployee.fullName(),
                    managerEmployee.fullName(),
                    managerEmployee.email(),
                    partnerEmployee.fullName(),
                    payload.timeRecordId(),
                    timeRecord.startWork().format(formatter),
                    timeRecord.endWork() != null ? timeRecord.endWork().format(formatter) : "N/A",
                    payload.newStartWork().format(formatter),
                    payload.newEndWork().format(formatter)
            );

            log.info(notificationMessage);

            // 2. Confirmação (ACK) - O @SqsListener faz o ACK automático em caso de sucesso.
            log.info("Mensagem de solicitação ID {} confirmada com sucesso (ACK).", payload.timeRecordId());
            message.ack();
        } catch (Exception e) {
            log.error("Erro ao processar a mensagem da fila para o registro de ponto ID {}: {}", payload.timeRecordId(), e.getMessage());
            message.nack();
            throw e;
        }
    }
}