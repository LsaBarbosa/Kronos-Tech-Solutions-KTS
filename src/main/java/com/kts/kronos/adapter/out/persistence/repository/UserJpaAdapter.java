package com.kts.kronos.adapter.out.persistence.repository;

import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.roles.CtoRole;
import com.kts.kronos.domain.model.roles.ManagerRole;
import com.kts.kronos.domain.model.roles.PartnerRole;
import com.kts.kronos.domain.model.roles.Role;
import com.kts.kronos.domain.port.out.UserRepositoryPort;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Transactional
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepositoryPort {
    private final DataUserRepository userRepository;

    @Override
    public User findByUsername(String username)   {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException(username + "Não foi encontrado"));
        return new User(
                userEntity.getUserId(),
                userEntity.getUsername(),
                userEntity.getPassword(),
                List.of(parseRole(userEntity.getRole())),
                userEntity.isEnabled(),
                userEntity.getEmployee().getEmployeeId()
        );
    }
    private Role parseRole(String role){
        return switch (role){
            case "CTO" -> new CtoRole();
            case "MANAGER" -> new ManagerRole();
            default -> new PartnerRole();
        };
    }
}
