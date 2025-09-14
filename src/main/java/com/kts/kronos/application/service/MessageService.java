package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

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
        var employee = employeeProvider.findById(senderEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (jwtAuthenticatedUser.getRoleFromToken().equals(Role.MANAGER.name())) {
            var message = new Message(
                    senderEmployeeId,
                    employee.companyId(),
                    request.messageText(),
                    request.priority()
            );
            messageProvider.save(message);
        } else {
            throw new BadRequestException("Apenas Managers podem postar mensagens");
        }
    }

    @Override
    public List<Message> listMessagesForMyCompany() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        return messageProvider.findByCompanyId(employee.companyId());
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


}
