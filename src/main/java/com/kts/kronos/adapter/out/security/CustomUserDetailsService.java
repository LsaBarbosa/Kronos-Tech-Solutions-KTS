package com.kts.kronos.adapter.out.security;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var entity = repo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos"));
        var domain = entity.toDomain();

        if (!domain.active()) {
            throw new DisabledException("A sua conta foi desativada. Entre em contato com o seu Gestor para mais informações.");
        }
        return SecurityUserMapper.toSpringUser(domain);
    }
}
