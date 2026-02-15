package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.adapter.in.web.dto.message.MessageResponse;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.ApiPaths.MESSAGES;
import static com.kts.kronos.constants.ApiPaths.MESSAGE_ID;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(MESSAGES)
@RequiredArgsConstructor
@Tag(name = SWAGGER_MSG_TAG, description = SWAGGER_MSG_DESC)public class MessageController {

    private final MessageUseCase useCase;

    @PreAuthorize(MANAGER)
    @PostMapping
    @Operation(summary = POST_MSG_SUMMARY, description = POST_MSG_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = POST_MSG_SUCCESS),
            @ApiResponse(responseCode = "400", description = POST_MSG_400),
            @ApiResponse(responseCode = "404", description = POST_MSG_404),
            @ApiResponse(responseCode = "403", description = POST_MSG_403)
    })
    public void postMessage(@Valid @RequestBody CreateMessageRequest request) {
        useCase.postMessage(request);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping
    @Operation(summary = LIST_MSG_SUMMARY, description = LIST_MSG_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = LIST_MSG_SUCCESS),
            @ApiResponse(responseCode = "404", description = EMPLOYEE_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public ResponseEntity<List<MessageResponse>> getMessages() {
        var messages = useCase.listMessagesForMyCompany();
        var responseList = messages.stream()
                .map(MessageResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(MESSAGE_ID)
    @Operation(summary = DEL_MSG_SUMMARY, description = DEL_MSG_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = DEL_MSG_SUCCESS),
            @ApiResponse(responseCode = "400", description = DEL_MSG_400),
            @ApiResponse(responseCode = "404", description = MESSAGE_NOT_FOUND),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
     public void deleteMessage(@PathVariable UUID messageId) {
        useCase.deleteMessage(messageId);
    }
}
