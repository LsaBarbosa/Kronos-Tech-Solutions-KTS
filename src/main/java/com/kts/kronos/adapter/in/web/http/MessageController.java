package com.kts.kronos.adapter.in.web.http;
import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.in.web.dto.message.MessageResponse;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.ApiPaths.MESSAGE_ID;
import static com.kts.kronos.constants.ApiPaths.MESSAGES;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;
import static com.kts.kronos.constants.Messages.MANAGER;

@RestController
@RequestMapping(MESSAGES)
@RequiredArgsConstructor
public class MessageController {

    private final MessageUseCase useCase;

    @PreAuthorize(MANAGER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void postMessage(@Valid @RequestBody CreateMessageRequest request) {
        useCase.postMessage(request);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages() {
        var messages = useCase.listMessagesForMyCompany();
        var responseList = messages.stream()
                .map(MessageResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(MESSAGE_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId) {
        useCase.deleteMessage(messageId);
    }
}
