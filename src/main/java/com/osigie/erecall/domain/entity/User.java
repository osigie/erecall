package com.osigie.erecall.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Setter
  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  public void prePersist() {
    this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  @Builder
  public User(String email, String passwordHash, String username, Role role) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
    this.username = username;
  }

  public enum Role {
    USER,
    ADMIN
  }

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public @NonNull String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return emailVerified;
  }
}
