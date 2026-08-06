package ru.autoauction.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="app_users", indexes=@Index(name="idx_user_max", columnList="maxUserId", unique=true))
public class AppUser {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false, unique=true) public Long maxUserId;
  @Column(nullable=false) public String name;
  public String phone;
  public Boolean phoneVerified = false;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role = Role.USER;
  public Boolean banned = false;
  public Instant bannedAt;
  @Column(nullable=false) public boolean registered;
  @Column(nullable=false) public Instant createdAt = Instant.now();
  public Instant lastSeenAt = Instant.now();
  protected AppUser() {}
  public AppUser(Long maxUserId, String name, Role role) { this.maxUserId=maxUserId; this.name=name; this.role=role; }
}
