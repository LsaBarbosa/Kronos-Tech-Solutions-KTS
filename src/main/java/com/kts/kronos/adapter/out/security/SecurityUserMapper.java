package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class SecurityUserMapper {
    private SecurityUserMapper() {}

    public static UserDetails toSpringUser(User domain) {
        return org.springframework.security.core.userdetails.User
                .withUsername(domain.username())
                .password(domain.password())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + domain.role().name())))
                .disabled(!domain.active())
                .build();
    }
}
