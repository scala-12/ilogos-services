package ru.ilogos.auth_service.entity;

import java.time.Instant;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import ru.ilogos.auth_service.model.RoleType;
import ru.ilogos.auth_service.validation.annotation.ValidTimezone;
import ru.ilogos.auth_service.validation.annotation.ValidUsername;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "app_user")
public class User {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue
    private Long id;

    @ValidUsername
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(unique = true, nullable = false)
    @Email
    private String email;

    @Column(nullable = false)
    @NotBlank
    @Setter(AccessLevel.NONE)
    private String password;

    @NotEmpty
    @ElementCollection(targetClass = RoleType.class)
    @Enumerated(EnumType.STRING)
    @Singular
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<RoleType> roles;

    @Column(name = "active")
    private boolean isActive;

    @Column(name = "emailVerified")
    private boolean isEmailVerified;

    private int failedAttempts;

    @NotBlank
    @ValidTimezone
    private String timezone;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        failedAttempts = 0;
        isEmailVerified = false;
    }

    public static class UserBuilder {
        @Getter
        private String password;

        public UserBuilder hashPassword(String rawPassword) {
            this.password = passwordEncoder.encode(rawPassword);

            return this;
        }
    }

    public boolean equalsPassword(String rawPassword) {
        return passwordEncoder.matches(rawPassword, password);
    }

}
