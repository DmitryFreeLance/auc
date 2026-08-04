package ru.autoauction.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="bids", indexes={@Index(name="idx_bid_lot_time", columnList="lot_id,createdAt"),@Index(name="idx_bid_user", columnList="user_id")})
public class Bid {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) public Lot lot;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) public AppUser user;
  @Column(nullable=false) public long amount;
  @Column(nullable=false) public Instant createdAt = Instant.now();
  protected Bid() {}
  public Bid(Lot lot, AppUser user, long amount) { this.lot=lot; this.user=user; this.amount=amount; }
}
