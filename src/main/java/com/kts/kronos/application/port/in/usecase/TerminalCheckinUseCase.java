package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinResponse;

public interface TerminalCheckinUseCase {

    /**
     * Autentica o colaborador via biometria facial e registra o ponto em uma única operação.
     * Retorna o resultado do checkin; o JWT é gerado internamente e deve ser definido em cookie pelo controller.
     */
    TerminalCheckinResult checkinByFace(TerminalCheckinRequest request);

    record TerminalCheckinResult(String jwtToken, TerminalCheckinResponse checkinResponse) {}
}
