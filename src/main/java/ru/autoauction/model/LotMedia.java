package ru.autoauction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity @Table(name="lot_media")
public class LotMedia {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @JsonIgnore @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) public Lot lot;
  @Column(nullable=false) public String url;
  @Column(nullable=false) public String type;
  @Column(nullable=false) public int sortOrder;
  protected LotMedia() {}
  public LotMedia(Lot lot, String url, String type, int sortOrder) { this.lot=lot; this.url=url; this.type=type; this.sortOrder=sortOrder; }
}
