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
    public static final String TIME_RECORD_APPROVAL_SUBSCRIPTION = "time-record-approval-subscription";
     private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME);

    @ServiceActivator(inputChannel = TIME_RECORD_APPROVAL_SUBSCRIPTION + ".input")
    public void handleTimeRecordChangeRequest(
            TimeRecordChangeRequestMessage message,
            @Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage originalMessage) {

        log.info("Recebida solicitação de alteração de ponto para o registro ID: {}", message.timeRecordId());

        try {
            // 1. Lógica de busca de dados e montagem de notificação (mantida do Listener anterior)
            var partnerEmployee = employeeProvider.findById(message.partnerEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException(PARTNER_NOT_FOUND + message.partnerEmployeeId()));

            var managerUser = userProvider.findById(message.managerId())
                    .orElseThrow(() -> new IllegalArgumentException(USER_MANAGER_NOT_FOUND + message.managerId()));

            var managerEmployee = employeeProvider.findById(managerUser.employeeId())
                    .orElseThrow(() -> new IllegalArgumentException(MANAGER_NOT_FOUND + managerUser.employeeId()));

            var timeRecord = timeRecordProvider.findById(message.timeRecordId())
                    .orElseThrow(() -> new IllegalArgumentException(RECORD_NOT_FOUND + message.timeRecordId()));

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
                    message.timeRecordId(),
                    timeRecord.startWork().format(formatter),
                    timeRecord.endWork() != null ? timeRecord.endWork().format(formatter) : "N/A",
                    message.newStartWork().format(formatter),
                    message.newEndWork().format(formatter)
            );

            log.info(notificationMessage);

            // 2. Confirmação (ACK)
            originalMessage.ack();
            log.info("Mensagem de solicitação ID {} confirmada com sucesso (ACK).", message.timeRecordId());

        } catch (Exception e) {
            log.error("Erro ao processar a mensagem da fila para o registro de ponto ID {}: {}", message.timeRecordId(), e.getMessage());
            // 3. Rejeição (NACK) para re-entrega pelo Pub/Sub
            originalMessage.nack();
        }
    }
}