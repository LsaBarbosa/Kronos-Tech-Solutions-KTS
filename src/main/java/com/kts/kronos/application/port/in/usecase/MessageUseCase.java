package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.message.CreateMessageResponse;
import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.domain.model.Message;

import java.util.List;
import java.util.UUID;

public interface MessageUseCase {
    CreateMessageResponse postMessage(CreateMessageRequest request);
    List<Message> listMessagesForMyCompany();
    List<Message> listMessagesForMyCompany(Integer page, Integer size);
    void deleteMessage(UUID messageId);
}
