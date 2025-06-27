package com.kts.kronos.domain.port.out;

import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.dto.AuthResponse;
import com.kts.kronos.domain.model.dto.LoginRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserRepositoryPort {
    User findByUsername(String username) ;
}
