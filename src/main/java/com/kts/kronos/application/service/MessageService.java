package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService implements MessageUseCase {

    private final MessageProvider messageProvider;
    private final EmployeeProvider employeeProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @Override
    public void postMessage(CreateMessageRequest request) {
        var senderEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var sender = employeeProvider.findById(senderEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));


        var recipientIds = normalizeRecipients(request.recipientEmployeeIds(), senderEmployeeId);

        var recipients = employeeProvider.findByIdIn(recipientIds);
        var validRecipientIds = recipients.stream()
                .filter(recipient -> sender.companyId().equals(recipient.companyId()))
                .map(Employee::employeeId)
                .toList();

        if (validRecipientIds.isEmpty()) {
            throw new BadRequestException(INVALID_EMPLOYEE);
        }

        var messages = validRecipientIds.stream()
                .map(recipientId -> new Message(
                        senderEmployeeId,
                        sender.companyId(),
                        request.title(),
                        request.messageText(),
                        request.priority(),
                        recipientId
                ))
                .toList();

        messageProvider.saveAll(messages);
    }

    @Override
    public List<Message> listMessagesForMyCompany() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var companyId = employeeProvider.findCompanyIdByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        return messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId);
    }

    @Override
    public void deleteMessage(UUID messageId) {
        var senderEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var message = messageProvider.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(MESSAGE_NOT_FOUND));

        if (!message.employeeId().equals(senderEmployeeId)) {
            throw new BadRequestException(ONLY_MANAGER_CAN_DELETE_MESSAGE);
        }

        messageProvider.deleteByMessageIdAndEmployeeId(messageId, senderEmployeeId);
    }

    private List<UUID> normalizeRecipients(List<UUID> recipients, UUID senderEmployeeId) {
        if (recipients == null || recipients.isEmpty()) {
            throw new BadRequestException(CHOOSE_EMPLOYEE);
        }

        Set<UUID> uniqueRecipients = recipients.stream()
                .filter(recipientId -> recipientId != null && !recipientId.equals(senderEmployeeId))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueRecipients.isEmpty()) {
            throw new BadRequestException(INVALID_EMPLOYEE);
        }

        return List.copyOf(uniqueRecipients);
    }
}
