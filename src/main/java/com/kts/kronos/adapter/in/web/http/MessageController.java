package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.in.web.dto.message.CreateMessageResponse;
import com.kts.kronos.adapter.in.web.dto.message.MessageResponse;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Message;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.ApiPaths.MESSAGES;
import static com.kts.kronos.constants.ApiPaths.MESSAGE_ID;
import static com.kts.kronos.constants.Messages.ADMINISTRATOR;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;

@RestController
@RequestMapping(MESSAGES)
@RequiredArgsConstructor
public class MessageController {

    private final MessageUseCase useCase;
    private final EmployeeProvider employeeProvider;

    @PreAuthorize(ADMINISTRATOR)
    @PostMapping
    public ResponseEntity<CreateMessageResponse> postMessage(@Valid @RequestBody CreateMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.postMessage(request));
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        var safePage = (page == null || page < 0) ? 0 : page;
        var safeSize = (size == null || size < 1) ? 10 : Math.min(size, 100);
        var messages = useCase.listMessagesForMyCompany(safePage, safeSize);
        var senderNames = employeeProvider.findAllByIds(messages.stream()
                        .map(Message::employeeId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        employee -> employee.employeeId(),
                        employee -> employee.fullName(),
                        (left, right) -> left
                ));
        var responseList = messages.stream()
                .map(message -> MessageResponse.fromDomain(
                        message,
                        resolveSenderName(message.employeeId(), senderNames)
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PreAuthorize(ADMINISTRATOR)
    @DeleteMapping(MESSAGE_ID)
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        useCase.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }

    private String resolveSenderName(UUID senderEmployeeId, Map<UUID, String> senderNames) {
        if (senderNames.containsKey(senderEmployeeId)) {
            return senderNames.get(senderEmployeeId);
        }

        return employeeProvider.findById(senderEmployeeId)
                .map(employee -> employee.fullName())
                .orElse(null);
    }
}
