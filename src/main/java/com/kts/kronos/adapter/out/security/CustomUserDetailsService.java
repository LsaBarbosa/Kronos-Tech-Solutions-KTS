package com.kts.kronos.adapter.out.security;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    public static final String INVALID_ACCESS = "Credenciais inválidas";
    public static final String INACTIVED_ACCOUNT = "Conta foi desativada. Entre em contato com o seu Gestor para mais informações.";
    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        var entity = repo.findByUsernameIgnoreCase(normalizedUsername)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_ACCESS));
        var domain = entity.toDomain();

        if (!domain.active()) {
            throw new DisabledException(INACTIVED_ACCOUNT);
        }
        return SecurityUserMapper.toSpringUser(domain);
    }
}
