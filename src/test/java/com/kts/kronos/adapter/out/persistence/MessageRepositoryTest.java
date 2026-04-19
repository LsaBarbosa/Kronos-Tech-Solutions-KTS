package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageRepositoryTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private MessageRepository repository;

    @Test
    void deveListarMensagensVisiveisPorEmpresaEEmployee() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID otherEmployeeId = UUID.randomUUID();

        repository.save(message(companyId, employeeId, employeeId, "recebida", LocalDateTime.now().minusHours(1)));
        repository.save(message(companyId, employeeId, otherEmployeeId, "enviada", LocalDateTime.now()));
        repository.save(message(UUID.randomUUID(), otherEmployeeId, employeeId, "outra-empresa", LocalDateTime.now()));

        var result = repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId);

        assertEquals(2, result.size());
        assertEquals("enviada", result.getFirst().getTitle());
    }

    @Test
    void devePaginarMensagensVisiveis() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        repository.save(message(companyId, employeeId, employeeId, "m1", LocalDateTime.now().minusMinutes(3)));
        repository.save(message(companyId, employeeId, employeeId, "m2", LocalDateTime.now().minusMinutes(2)));
        repository.save(message(companyId, employeeId, employeeId, "m3", LocalDateTime.now().minusMinutes(1)));

        var page = repository.findVisibleMessagesByCompanyIdAndEmployeeId(
                companyId,
                employeeId,
                PageRequest.of(0, 2)
        );

        assertEquals(2, page.getContent().size());
        assertEquals("m3", page.getContent().getFirst().getTitle());
    }

    private static MessageEntity message(
            UUID companyId,
            UUID employeeId,
            UUID recipientEmployeeId,
            String title,
            LocalDateTime createdAt
    ) {
        return MessageEntity.builder()
                .messageId(UUID.randomUUID())
                .companyId(companyId)
                .employeeId(employeeId)
                .recipientEmployeeId(recipientEmployeeId)
                .title(title)
                .messageText("texto-" + title)
                .priority(MessagePriority.NORMAL)
                .createdAt(createdAt)
                .build();
    }
}