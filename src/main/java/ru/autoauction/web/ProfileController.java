package ru.autoauction.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.autoauction.model.*;
import ru.autoauction.repo.BidRepository;
import ru.autoauction.service.CurrentUser;

import java.time.Instant;
import java.util.*;

import static ru.autoauction.web.ApiDtos.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
  private final CurrentUser current;
  private final BidRepository bids;

  public ProfileController(CurrentUser current, BidRepository bids) {
    this.current = current;
    this.bids = bids;
  }

  @GetMapping
  public ProfileDto profile(HttpSession session) {
    AppUser user = current.requireRegistered(session);
    List<Bid> ownBids = bids.findByUserIdOrderByCreatedAtDesc(user.id);
    Map<Long, List<Bid>> byLot = new LinkedHashMap<>();
    ownBids.forEach(bid -> byLot.computeIfAbsent(bid.lot.id, ignored -> new ArrayList<>()).add(bid));

    List<ProfileAuctionDto> auctions = byLot.values().stream().map(group -> auction(group, user)).sorted(
        Comparator.comparing((ProfileAuctionDto item) -> item.status() == LotStatus.LIVE).reversed()
            .thenComparing(ProfileAuctionDto::lastBidAt, Comparator.reverseOrder())
    ).toList();
    long wins = auctions.stream().filter(ProfileAuctionDto::winner).count();
    long winningVolume = auctions.stream().filter(ProfileAuctionDto::winner).mapToLong(ProfileAuctionDto::myBestBid).sum();
    return new ProfileDto(UserDto.of(user), user.createdAt, ownBids.size(), auctions.size(), wins, winningVolume, auctions);
  }

  private ProfileAuctionDto auction(List<Bid> ownBids, AppUser user) {
    Lot lot = ownBids.getFirst().lot;
    Bid top = bids.findFirstByLotIdOrderByAmountDescCreatedAtAsc(lot.id).orElse(null);
    LotStatus status = lot.status;
    if (status == LotStatus.LIVE && !Instant.now().isBefore(lot.endsAt)) status = LotStatus.FINISHED;
    long best = ownBids.stream().mapToLong(bid -> bid.amount).max().orElse(0);
    boolean isTop = top != null && top.user.id.equals(user.id);
    boolean winner = status == LotStatus.FINISHED && isTop;
    boolean leading = status == LotStatus.LIVE && isTop;
    String image = lot.media.stream().filter(media -> "image".equals(media.type)).findFirst().map(media -> media.url).orElse(null);
    Instant lastBid = ownBids.stream().map(bid -> bid.createdAt).max(Instant::compareTo).orElse(lot.createdAt);
    long finalPrice = top == null ? lot.currentPrice : top.amount;
    return new ProfileAuctionDto(lot.id, lot.title, image, status, lastBid, lot.endsAt, best, finalPrice, ownBids.size(), winner, leading);
  }
}
