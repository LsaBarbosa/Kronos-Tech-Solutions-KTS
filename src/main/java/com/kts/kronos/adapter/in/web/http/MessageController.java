package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.in.web.dto.message.MessageResponse;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.ApiPaths.MESSAGES;
import static com.kts.kronos.constants.ApiPaths.MESSAGE_ID;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;
import static com.kts.kronos.constants.Messages.MANAGER;

@RestController
@RequestMapping(MESSAGES)
@RequiredArgsConstructor
public class MessageController {

    private final MessageUseCase useCase;
    private final EmployeeProvider employeeProvider;

    @PreAuthorize(MANAGER)
    @PostMapping
    public ResponseEntity<Void> postMessage(@Valid @RequestBody CreateMessageRequest request) {
        useCase.postMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
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
        var responseList = messages.stream()
                .map(message -> MessageResponse.fromDomain(
                        message,
                        employeeProvider.findById(message.employeeId())
                                .map(employee -> employee.fullName())
                                .orElse(null)
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(MESSAGE_ID)
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        useCase.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
