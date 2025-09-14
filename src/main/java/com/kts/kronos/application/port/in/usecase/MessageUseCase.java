package com.kts.kronos.application.port.in.usecase;
import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.domain.model.Message;

import java.util.List;
import java.util.UUID;
public interface MessageUseCase {
    void postMessage(CreateMessageRequest request);
    List<Message> listMessagesForMyCompany();
    void deleteMessage(UUID messageId);
}
