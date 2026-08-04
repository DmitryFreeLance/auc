package ru.autoauction.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="broadcasts")
public class BroadcastLog {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false, length=4000) public String text;
  @Column(nullable=false) public int recipients;
  @Column(nullable=false) public int delivered;
  @Column(nullable=false) public Instant createdAt=Instant.now();
  protected BroadcastLog() {}
  public BroadcastLog(String text, int recipients, int delivered) { this.text=text; this.recipients=recipients; this.delivered=delivered; }
}
