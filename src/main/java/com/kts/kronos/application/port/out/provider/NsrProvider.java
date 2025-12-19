package com.kts.kronos.application.port.out.provider;
import java.util.UUID;

public interface NsrProvider {
    Long generateNextNsr(UUID companyId);
}