package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.SpringDataUserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.application.port.out.repository.UserRepository;
import com.kts.kronos.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository jpa;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        UserEntity saved  = jpa.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<UserEntity> opt = jpa.findByUsername(username);
        return opt.map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID userId) {
        Optional<UserEntity> opt = jpa.findById(userId);
        return opt.map(UserEntity::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll()
                .stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findByActive(boolean active) {
        List<UserEntity> entities = active
                ? jpa.findByActiveTrue()
                : jpa.findByActiveFalse();
        return entities.stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findByEmployeeId(UUID employeeId) {
        Optional<UserEntity> opt = jpa.findByEmployeeId(employeeId);
        return opt.map(UserEntity::toDomain);
    }

    @Override
    public void deleteById(UUID userId) {
        jpa.deleteById(userId);
    }
}
