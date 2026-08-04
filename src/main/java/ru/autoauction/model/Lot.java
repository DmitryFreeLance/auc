package ru.autoauction.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="lots")
public class Lot {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public long version;
  @Column(nullable=false) public String title;
  @Column(nullable=false, length=5000) public String description;
  public String vin;
  public String mileage;
  public String autotecaUrl;
  @Column(nullable=false) public long startingPrice;
  @Column(nullable=false) public long currentPrice;
  @Column(nullable=false) public long bidStep;
  @Column(nullable=false) public Instant startsAt;
  @Column(nullable=false) public Instant endsAt;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public LotStatus status = LotStatus.DRAFT;
  @Column(nullable=false) public Instant createdAt = Instant.now();
  @OneToMany(mappedBy="lot", cascade=CascadeType.ALL, orphanRemoval=true, fetch=FetchType.EAGER) @OrderBy("sortOrder asc") public List<LotMedia> media = new ArrayList<>();
  public Lot() {}
}
