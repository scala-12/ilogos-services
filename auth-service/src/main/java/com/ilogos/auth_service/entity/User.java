package com.ilogos.auth_service.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ilogos.auth_service.model.RoleType;
import com.ilogos.auth_service.model.TokenInfo;
import com.ilogos.auth_service.model.common.UserView;
import com.ilogos.auth_service.validation.annotation.ValidTimezone;
import com.ilogos.auth_service.validation.annotation.ValidUsername;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "app_user")
public class User implements UserView {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private Set<Field> changedFields = new HashSet<>();

    public enum Field {
        USERNAME,
        PASSWORD,
        ATTEMPTS_RESET,
        ATTEMPTS_INCREMENT,
        LOGGED_TIME,
        EMAIL
    }

    @Setter(AccessLevel.NONE)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ValidUsername
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotEmpty
    @ElementCollection(targetClass = RoleType.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Singular
    @Column(name = "role")
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<RoleType> roles;

    @Column(name = "active")
    private boolean isActive;

    @Column(name = "email_verified")
    private boolean isEmailVerified;

    @Setter(AccessLevel.NONE)
    private int failedAttempts;

    @NotBlank
    @ValidTimezone
    private String timezone;

    @Setter(AccessLevel.NONE)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "last_token_issued_at", nullable = false)
    private Instant lastTokenIssuedAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Setter(AccessLevel.NONE)
    @Column(name = "prev_login_at")
    private Instant prevLoginAt;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<UsernameHistory> usernameHistory = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmailHistory> emailHistory = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        failedAttempts = 0;
        isEmailVerified = false;
        lastTokenIssuedAt = Instant.now();
    }

    public void preUpdate() {
        if (changedFields.isEmpty()) {
            return;
        }
        var time = Instant.now();
        if (changedFields.contains(Field.ATTEMPTS_INCREMENT)) {
            failedAttempts += 1;
        } else if (changedFields.contains(Field.ATTEMPTS_RESET) || changedFields.contains(Field.LOGGED_TIME)) {
            if (changedFields.contains(Field.LOGGED_TIME)) {
                prevLoginAt = lastLoginAt;
                lastLoginAt = time;
            }
            failedAttempts = 0;
        }

        if (changedFields.contains(Field.PASSWORD)
                || changedFields.contains(Field.USERNAME)
                || changedFields.contains(Field.EMAIL)) {
            updatedAt = time;
            if (changedFields.contains(Field.PASSWORD)) {
                passwordChangedAt = time;
            }
        }
    }

    public static class UserBuilder {

        public UserBuilder password(String rawPassword) {
            if (!rawPassword.isBlank()) {
                password = passwordEncoder.encode(rawPassword);
            }

            return this;
        }

        public UserBuilder username(String username) {
            this.username = username != null ? username.toLowerCase() : null;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email != null ? email.toLowerCase() : null;
            return this;
        }

        @SuppressWarnings("unused")
        private UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        @SuppressWarnings("unused")
        private UserBuilder lastTokenIssuedAt(Instant lastTokenIssuedAt) {
            this.lastTokenIssuedAt = lastTokenIssuedAt;
            return this;
        }

        @SuppressWarnings("unused")
        private UserBuilder updatedAt(Instant v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder lastLoginAt(Instant v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder prevLoginAt(Instant v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder usernameHistory(List<UsernameHistory> v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder emailHistory(List<EmailHistory> v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder failedAttempts(int v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder createdAt(Instant v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder passwordChangedAt(Instant v) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unused")
        private UserBuilder changedFields(Set<Field> v) {
            throw new UnsupportedOperationException();
        }
    }

    public boolean equalsPassword(String rawPassword) {
        return passwordEncoder.matches(rawPassword, password);
    }

    public boolean setPassword(String rawPassword) {
        if (!rawPassword.isBlank()) {
            if (id != null && !equalsPassword(rawPassword)) {
                changedFields.add(Field.PASSWORD);
            }
            password = passwordEncoder.encode(rawPassword);

            return true;
        }

        return false;
    }

    public boolean setUsername(String username) {
        var newUsername = username != null ? username.toLowerCase() : "";
        if (!newUsername.isBlank()) {
            if (id != null && !newUsername.equals(this.username)) {
                changedFields.add(Field.USERNAME);
            }
            this.username = newUsername;

            return true;
        }

        return false;
    }

    public boolean setEmail(String email) {
        var newEmail = email.toLowerCase();
        if (!newEmail.isBlank()) {
            if (id != null && !newEmail.equals(this.email)) {
                changedFields.add(Field.EMAIL);
            }
            this.email = newEmail;

            return true;
        }

        return false;
    }

    public void resetAttempts() {
        changedFields.add(Field.ATTEMPTS_RESET);
    }

    public void incrementAttempts() {
        changedFields.add(Field.ATTEMPTS_INCREMENT);
    }

    public void setLastTokenIssuedAt(TokenInfo info, boolean isLogin) {
        if (isLogin) {
            changedFields.add(Field.LOGGED_TIME);
        }
        lastTokenIssuedAt = info.getIssuedAt().toInstant();
    }

    public boolean hasChangedUsername() {
        return changedFields.contains(Field.USERNAME);
    }

    public boolean hasChangedEmail() {
        return changedFields.contains(Field.EMAIL);
    }

    public Set<String> getRolesNames() {
        return roles.stream().map(RoleType::name).collect(Collectors.toSet());
    }

}
