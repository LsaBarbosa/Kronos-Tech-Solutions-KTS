package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageResponse;
import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageDeliveryProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.MessageDelivery;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.MessageScope;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService implements MessageUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 200;

    private final MessageProvider messageProvider;
    private final MessageDeliveryProvider messageDeliveryProvider;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @Override
    public CreateMessageResponse postMessage(CreateMessageRequest request) {
        var role = jwtAuthenticatedUser.getCurrentRole();
        var sender = getAuthenticatedEmployee();
        var senderCompanyId = resolveSenderCompanyId(sender);

        return switch (role) {
            case CTO -> createGlobalMessage(request, sender, senderCompanyId);
            case MANAGER -> createDirectMessage(request, sender, senderCompanyId);
            case PARTNER -> throw new ForbiddenException(UNAUTHORIZED_ROLE_OPERATION);
        };
    }

    @Override
    public List<Message> listMessagesForMyCompany() {
        var employee = getAuthenticatedEmployee();
        return listVisibleMessages(employee, null, null);
    }

    @Override
    public List<Message> listMessagesForMyCompany(Integer page, Integer size) {
        var employee = getAuthenticatedEmployee();

        if (page == null && size == null) {
            return listVisibleMessages(employee, null, null);
        }

        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return listVisibleMessages(employee, safePage, safeSize);
    }

    @Override
    public void deleteMessage(UUID messageId) {
        var currentRole = jwtAuthenticatedUser.getCurrentRole();
        var senderEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var message = messageProvider.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(MESSAGE_NOT_FOUND));

        if (currentRole == Role.PARTNER) {
            throw new ForbiddenException(UNAUTHORIZED_ROLE_OPERATION);
        }

        if (currentRole == Role.CTO) {
            messageProvider.deleteByMessageId(messageId);
            return;
        }

        if (!message.employeeId().equals(senderEmployeeId) || message.scope() == MessageScope.GLOBAL) {
            throw new BadRequestException(ONLY_MANAGER_CAN_DELETE_MESSAGE);
        }

        messageProvider.deleteByMessageIdAndEmployeeId(messageId, senderEmployeeId);
    }

    private CreateMessageResponse createGlobalMessage(CreateMessageRequest request, Employee sender, UUID senderCompanyId) {
        var recipientEmployeeIds = userProvider.findByActive(true).stream()
                .map(User::employeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        var message = new Message(
                sender.employeeId(),
                senderCompanyId,
                request.title(),
                request.messageText(),
                request.priority(),
                MessageScope.GLOBAL,
                null
        );
        messageProvider.save(message);
        persistDeliveries(message, recipientEmployeeIds);

        return new CreateMessageResponse(message.messageId(), MessageScope.GLOBAL, recipientEmployeeIds.size());
    }

    private CreateMessageResponse createDirectMessage(CreateMessageRequest request, Employee sender, UUID senderCompanyId) {
        var recipientEmployeeIds = normalizeRecipients(request.recipientEmployeeIds());

        if (recipientEmployeeIds.isEmpty()) {
            throw new BadRequestException(CHOOSE_EMPLOYEE);
        }

        var validRecipientEmployeeIds = recipientEmployeeIds.stream()
                .filter(recipientEmployeeId -> isValidManagerRecipient(recipientEmployeeId, senderCompanyId))
                .toList();

        if (validRecipientEmployeeIds.isEmpty()) {
            throw new BadRequestException(INVALID_EMPLOYEE);
        }

        var message = new Message(
                sender.employeeId(),
                senderCompanyId,
                request.title(),
                request.messageText(),
                request.priority(),
                MessageScope.DIRECT,
                validRecipientEmployeeIds.size() == 1 ? validRecipientEmployeeIds.getFirst() : null
        );
        messageProvider.save(message);
        persistDeliveries(message, validRecipientEmployeeIds);

        return new CreateMessageResponse(message.messageId(), MessageScope.DIRECT, validRecipientEmployeeIds.size());
    }

    private List<Message> listVisibleMessages(Employee employee, Integer page, Integer size) {
        var role = jwtAuthenticatedUser.getCurrentRole();

        if (role == Role.CTO) {
            return page == null || size == null
                    ? messageProvider.findVisibleMessagesByEmployeeId(employee.employeeId())
                    : messageProvider.findVisibleMessagesByEmployeeId(employee.employeeId(), PageRequest.of(page, size));
        }

        var companyId = resolveSenderCompanyId(employee);
        return page == null || size == null
                ? messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employee.employeeId())
                : messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(
                companyId,
                employee.employeeId(),
                PageRequest.of(page, size)
        );
    }

    private void persistDeliveries(Message message, List<UUID> recipientEmployeeIds) {
        var createdAt = message.createdAt() == null ? LocalDateTime.now() : message.createdAt();
        messageDeliveryProvider.saveAll(recipientEmployeeIds.stream()
                .map(recipientEmployeeId -> new MessageDelivery(message.messageId(), recipientEmployeeId, createdAt))
                .toList());
    }

    private List<UUID> normalizeRecipients(List<UUID> recipients) {
        if (recipients == null) {
            return List.of();
        }

        return recipients.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private UUID resolveSenderCompanyId(Employee employee) {
        var activeCompanyId = jwtAuthenticatedUser.getActiveCompanyId();
        return activeCompanyId != null ? activeCompanyId : employee.companyId();
    }

    private boolean isValidManagerRecipient(UUID recipientEmployeeId, UUID senderCompanyId) {
        return employeeProvider.findById(recipientEmployeeId)
                .filter(Employee::active)
                .filter(employee -> employee.companyId().equals(senderCompanyId))
                .flatMap(employee -> userProvider.findByEmployeeId(employee.employeeId()))
                .map(User::active)
                .orElse(false);
    }

    private Employee getAuthenticatedEmployee() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        return employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }
}
