package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tb_user")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class UserEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled;

    @OneToMany
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_employee")
    )
    private EmployeeEntity employee;
}
