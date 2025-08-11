package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public class SecurityUserMapper {
    private SecurityUserMapper() {}

    public static UserDetails toSpringUser(User domain) {
        return new SecurityUser(
                domain.employeeId(),
                domain.username(),
                domain.password(),
                List.of(new SimpleGrantedAuthority("ROLE_" + domain.role().name())),
                domain.active(),
                true,
                true,
                true
        );
    }
}
