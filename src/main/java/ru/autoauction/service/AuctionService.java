package ru.autoauction.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.*;
import ru.autoauction.repo.*;
import java.time.Instant;
import java.time.Duration;

@Service
public class AuctionService {
  private final LotRepository lots; private final BidRepository bids;
  public AuctionService(LotRepository lots,BidRepository bids){this.lots=lots;this.bids=bids;}
  public record BidResult(Bid bid, AppUser previousLeader, Lot lot) {}
  @Transactional public BidResult placeBid(long lotId,AppUser user){
    Lot lot=lots.findById(lotId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Лот не найден"));
    Instant now=Instant.now();
    if(lot.status!=LotStatus.LIVE || now.isBefore(lot.startsAt) || !now.isBefore(lot.endsAt)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Торги не активны");
    Bid top=bids.findFirstByLotIdOrderByAmountDescCreatedAtAsc(lotId).orElse(null);
    if(top!=null && top.user.id.equals(user.id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ваша ставка уже лидирует");
    long amount=Math.addExact(lot.currentPrice,lot.bidStep);Bid bid=bids.save(new Bid(lot,user,amount));lot.currentPrice=amount;if(Duration.between(now,lot.endsAt).compareTo(Duration.ofMinutes(5))<0)lot.endsAt=lot.endsAt.plus(Duration.ofMinutes(2));lots.save(lot);return new BidResult(bid,top==null?null:top.user,lot);
  }
  @Transactional public Lot updateStep(long id,long step){if(step<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Шаг должен быть положительным");Lot l=lots.findById(id).orElseThrow();l.bidStep=step;return lots.save(l);}
}
