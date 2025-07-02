package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "tb_user")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class UserEntity {
    @Id
    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "employee_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID employeeId;

    public User toDomain() {
        return new User(
                userId,
                username,
                password,
                role,
                active,
                employeeId
        );
    }

    public static UserEntity fromDomain(User user) {
        return UserEntity.builder()
                .userId(user.userId())
                .username(user.username())
                .password(user.password())
                .role(user.role())
                .active(user.active())
                .employeeId(user.employeeId())
                .build();
    }
}
