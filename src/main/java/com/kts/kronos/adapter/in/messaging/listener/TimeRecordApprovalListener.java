package com.kts.kronos.adapter.in.messaging.listener;

import com.kts.kronos.adapter.in.messaging.dto.TimeRecordChangeRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.time.format.DateTimeFormatter;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeRecordApprovalListener {

    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider timeRecordProvider;

    // Formatter para exibir data e hora de forma legível no log
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME);

    /**
     * Este método escuta a fila de solicitações de alteração de ponto.
     * @param message A mensagem recebida do RabbitMQ.
     */
    @RabbitListener(queues = TIME_RECORD_CHANGE_QUEUE)
    public void handleTimeRecordChangeRequest(TimeRecordChangeRequestMessage message) {
        log.info("Recebida solicitação de alteração de ponto para o registro ID: {}", message.timeRecordId());

        try {
            // 1. Buscar informações relevantes do banco de dados
            var partnerEmployee = employeeProvider.findById(message.partnerEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException(PARTNER_NOT_FOUND + message.partnerEmployeeId()));

            var managerUser = userProvider.findById(message.managerId())
                    .orElseThrow(() -> new IllegalArgumentException(USER_MANAGER_NOT_FOUND + message.managerId()));

            var managerEmployee = employeeProvider.findById(managerUser.employeeId())
                    .orElseThrow(() -> new IllegalArgumentException(MANAGER_NOT_FOUND + managerUser.employeeId()));

            var timeRecord = timeRecordProvider.findById(message.timeRecordId())
                    .orElseThrow(() -> new IllegalArgumentException(RECORD_NOT_FOUND + message.timeRecordId()));

            // 2. Montar a notificação (simulação via log)
            String notificationMessage = String.format(
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
                    managerEmployee.email(), // O e-mail para onde a notificação seria enviada
                    partnerEmployee.fullName(),
                    message.timeRecordId(),
                    timeRecord.startWork().format(formatter),
                    timeRecord.endWork() != null ? timeRecord.endWork().format(formatter) : "N/A",
                    message.newStartWork().format(formatter),
                    message.newEndWork().format(formatter)
            );

            // 3. Simular o envio da notificação
            // Em um projeto real, aqui você chamaria seu serviço de e-mail:
            // emailService.send(managerEmployee.email(), "Aprovação de Alteração de Ponto", notificationMessage);
            log.info(notificationMessage);


        } catch (Exception e) {
            log.error("Erro ao processar a mensagem da fila para o registro de ponto ID {}: {}", message.timeRecordId(), e.getMessage());
            // Aqui você poderia implementar uma lógica de "dead-letter queue" para tratar a falha.
        }
    }
}
