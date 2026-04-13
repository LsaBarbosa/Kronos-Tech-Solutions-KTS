package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class UserProviderImpl implements UserProvider {
    private final UserRepository jpa;

    @Override
    public void save(User user) {
        var entity = UserEntity.fromDomain(user);
        var saved  = jpa.save(entity);
         saved.toDomain();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        Optional<UserEntity> opt = jpa.findByUsernameIgnoreCase(username);
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
    public List<User> findByEmployeeIds(Collection<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        return jpa.findByEmployeeIdIn(employeeIds)
                .stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public List<User> findByEmployeeIdsAndActive(Collection<UUID> employeeIds, boolean active) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }

        return jpa.findByEmployeeIdInAndActive(employeeIds, active)
                .stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID userId) {
        jpa.deleteById(userId);
    }

    @Override
    public List<User> findAllByIds(Collection<UUID> ids) {
        List<User> result = new ArrayList<>();
        jpa.findAllById(ids).forEach(entity -> result.add(entity.toDomain()));
        return result;
    }
}
