package br.ifsp.guesthouse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {
    }

    public PasswordResetToken(
            String tokenHash,
            UserAccount user,
            LocalDateTime expiresAt
    ) {
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash is required");
        this.user = Objects.requireNonNull(user, "User is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration date is required");
        this.used = false;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = Objects.requireNonNull(tokenHash, "Token hash is required");
    }

    public UserAccount getUser() {
        return user;
    }

    public void setUser(UserAccount user) {
        this.user = Objects.requireNonNull(user, "User is required");
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration date is required");
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public void markAsUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return expiresAt == null || !LocalDateTime.now().isBefore(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}