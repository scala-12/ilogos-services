package com.ilogos.auth.user.usernameHistory;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import com.ilogos.auth.user.User;
import com.ilogos.auth.user.validation.annotation.ValidUsername;
import com.ilogos.auth.utils.hibernate.CitextType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "username_history")
public class UsernameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ValidUsername
    @Type(value = CitextType.class)
    @Column(nullable = false, columnDefinition = "citext")
    private String username;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "start_at", nullable = false, updatable = false)
    private Instant startAt;

    @Setter
    @Column(name = "end_at")
    private Instant endAt;

    public UsernameHistory(User user) {
        this.user = user;
        this.username = user.getUsername();
    }
}
