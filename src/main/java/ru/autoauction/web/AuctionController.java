package ru.autoauction.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.*;
import ru.autoauction.repo.*;
import ru.autoauction.service.*;
import java.time.Instant;
import java.util.*;
import static ru.autoauction.web.ApiDtos.*;

@RestController @RequestMapping("/api")
public class AuctionController {
  private final CurrentUser current;private final LotRepository lots;private final BidRepository bids;private final AuctionService auction;private final AuctionEvents events;private final MaxBotService bot;
  public AuctionController(CurrentUser current,LotRepository lots,BidRepository bids,AuctionService auction,AuctionEvents events,MaxBotService bot){this.current=current;this.lots=lots;this.bids=bids;this.auction=auction;this.events=events;this.bot=bot;}
  @GetMapping("/lots/current") public LotDto currentLot(HttpSession s){AppUser u=current.requireRegistered(s);Instant now=Instant.now();Lot l=lots.findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByEndsAtAsc(LotStatus.LIVE,now,now).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Активных торгов сейчас нет"));return dto(l,u);}
  @GetMapping("/lots/active") public List<LotDto> activeLots(HttpSession s){AppUser u=current.requireRegistered(s);Instant now=Instant.now();return lots.findByStatusAndStartsAtLessThanEqualAndEndsAtGreaterThanOrderByEndsAtAsc(LotStatus.LIVE,now,now).stream().map(l->dto(l,u)).toList();}
  @GetMapping("/lots/{id}") public LotDto lot(@PathVariable long id,HttpSession s){AppUser u=current.requireRegistered(s);Lot l=lots.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Лот не найден"));Instant now=Instant.now();if(u.role==Role.USER&&(l.status!=LotStatus.LIVE||now.isBefore(l.startsAt)||!now.isBefore(l.endsAt)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Торги не активны");return dto(l,u);}
  @GetMapping("/lots/history") public List<AuctionHistoryDto> history(HttpSession s){current.requireRegistered(s);Instant now=Instant.now();return lots.findAllByOrderByCreatedAtDesc().stream().filter(l->l.status==LotStatus.FINISHED||(l.status==LotStatus.LIVE&&!now.isBefore(l.endsAt))).sorted(Comparator.comparing((Lot l)->l.endsAt).reversed()).map(this::historyDto).toList();}
  @GetMapping("/lots") public List<LotDto> all(HttpSession s){AppUser u=current.requireAdmin(s);return lots.findAll().stream().sorted(Comparator.comparing((Lot l)->l.createdAt).reversed()).map(l->dto(l,u)).toList();}
  public record BidRequest(Long amount){}
  @PostMapping("/lots/{id}/bids") public BidDto bid(@PathVariable long id,@RequestBody(required=false) BidRequest req,HttpSession s){AppUser bidder=current.requireRegistered(s);AuctionService.BidResult result=auction.placeBid(id,bidder,req==null?null:req.amount);events.bidUpdated(id,result.bid().amount,bidder.id);if(result.previousLeader()!=null&&!result.previousLeader().id.equals(bidder.id))bot.sendOutbid(result.previousLeader().maxUserId,result.lot().title,result.bid().amount);return BidDto.of(result.bid());}
  @GetMapping(value="/lots/{id}/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public ResponseEntity<SseEmitter> events(@PathVariable long id,HttpSession s){AppUser u=current.requireRegistered(s);if(!lots.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Лот не найден");return ResponseEntity.ok().cacheControl(CacheControl.noCache()).header("X-Accel-Buffering","no").body(events.subscribe(id,u.id));}
  @GetMapping(value="/events",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public ResponseEntity<SseEmitter> globalEvents(HttpSession s){AppUser u=current.requireRegistered(s);return ResponseEntity.ok().cacheControl(CacheControl.noCache()).header("X-Accel-Buffering","no").body(events.subscribeGlobal(u.id));}
  @GetMapping("/lots/{id}/bids") public List<BidDto> lotBids(@PathVariable long id,HttpSession s){current.requireAdmin(s);return bids.findByLotIdOrderByCreatedAtDesc(id).stream().map(BidDto::of).toList();}
  private LotDto dto(Lot l,AppUser viewer){List<Bid> all=bids.findByLotIdOrderByCreatedAtDesc(l.id);Bid top=all.stream().max(Comparator.comparingLong(b->b.amount)).orElse(null);Long my=all.stream().filter(b->b.user.id.equals(viewer.id)).map(b->b.amount).max(Long::compare).orElse(null);LotStatus status=l.status;if(status==LotStatus.LIVE&&!Instant.now().isBefore(l.endsAt))status=LotStatus.FINISHED;return new LotDto(l.id,l.title,l.description,l.vin,l.mileage,l.ownersCount,l.engineNumber,l.fuelType,l.engineVolume,l.horsepower,l.transmission,l.driveType,l.autotecaUrl,l.startingPrice,l.currentPrice,l.bidStep,l.startsAt,l.endsAt,status,l.media.stream().map(m->new MediaDto(m.id,m.url,m.type)).toList(),all.size(),bids.countParticipantsByLotId(l.id),events.onlineUsers(l.id),top==null?null:top.user.id,top==null?null:top.user.name,top==null?null:top.user.phone,my);}
  private AuctionHistoryDto historyDto(Lot l){List<Bid> all=bids.findByLotIdOrderByCreatedAtDesc(l.id);Bid top=all.stream().max(Comparator.comparingLong(b->b.amount)).orElse(null);String image=l.media.stream().filter(m->"image".equals(m.type)).findFirst().map(m->m.url).orElse(null);return new AuctionHistoryDto(l.id,l.title,l.mileage,l.endsAt,top==null?l.currentPrice:top.amount,top==null?null:top.user.name,image,all.size());}
}
