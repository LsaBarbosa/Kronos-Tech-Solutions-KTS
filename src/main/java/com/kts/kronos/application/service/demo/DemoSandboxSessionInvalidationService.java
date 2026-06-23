package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxSessionInvalidationService {

    private final UserRepository userRepository;

    /**
     * Bumps session_version for the sandbox user, causing the JWT filter to
     * reject all existing tokens on the next request (server-side revocation).
     *
     * @return number of sessions invalidated (0 or 1)
     */
    @Transactional
    public int invalidateSandboxUserSession(String username) {
        Optional<UserEntity> userOpt = userRepository.findByUsernameIgnoreCase(username);
        if (userOpt.isEmpty()) {
            log.debug("[DemoSandbox] No user found for session invalidation: username={}", username);
            return 0;
        }
        UserEntity user = userOpt.get();
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.save(user);
        log.info("[DemoSandbox] Session invalidated for sandbox user: username={}", username);
        return 1;
    }
}
