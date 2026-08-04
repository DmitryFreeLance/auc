package ru.autoauction.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.config.AppProperties;
import ru.autoauction.model.*;
import ru.autoauction.repo.*;
import ru.autoauction.service.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static ru.autoauction.web.ApiDtos.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
  private final CurrentUser current;private final LotRepository lots;private final BidRepository bids;private final UserRepository users;private final BroadcastRepository broadcasts;private final AuctionService auction;private final MaxBotService bot;private final AppProperties props;
  public AdminController(CurrentUser current,LotRepository lots,BidRepository bids,UserRepository users,BroadcastRepository broadcasts,AuctionService auction,MaxBotService bot,AppProperties props){this.current=current;this.lots=lots;this.bids=bids;this.users=users;this.broadcasts=broadcasts;this.auction=auction;this.bot=bot;this.props=props;}
  public record StepRequest(@Positive long step){} public record BroadcastRequest(@NotBlank @Size(max=4000) String text){}
  public record AdminUserDto(Long id,Long maxUserId,String name,String phone,Role role,boolean registered,Instant createdAt,Instant lastSeenAt){
    static AdminUserDto of(AppUser u){return new AdminUserDto(u.id,u.maxUserId,u.name,u.phone,u.role,u.registered,u.createdAt,u.lastSeenAt);}
  }
  public record Stats(long users,long lots,long liveLots,long bids,long bidVolume,long broadcasts){}
  @GetMapping("/stats") public Stats stats(HttpSession s){current.requireAdmin(s);return new Stats(users.countByRegisteredTrue(),lots.count(),lots.countByStatus(LotStatus.LIVE),bids.count(),bids.totalBidVolume(),broadcasts.count());}
  @GetMapping("/users") public List<AdminUserDto> users(HttpSession s){
    current.requireAdmin(s);
    return users.findAll().stream().sorted(Comparator.comparing((AppUser u)->u.registered).reversed().thenComparing(u->u.createdAt,Comparator.reverseOrder())).map(AdminUserDto::of).toList();
  }
  @PatchMapping("/users/{id}/make-admin") @Transactional public AdminUserDto makeAdmin(@PathVariable long id,HttpSession s){
    current.requireSuperAdmin(s);
    AppUser target=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Пользователь не найден"));
    if(target.role==Role.SUPER_ADMIN)return AdminUserDto.of(target);
    target.role=Role.ADMIN;
    return AdminUserDto.of(users.save(target));
  }
  @PatchMapping("/lots/{id}/step") public Map<String,Object> step(@PathVariable long id,@RequestBody StepRequest req,HttpSession s){current.requireAdmin(s);Lot l=auction.updateStep(id,req.step);return Map.of("id",l.id,"bidStep",l.bidStep);}
  @PatchMapping("/lots/{id}/status") public void status(@PathVariable long id,@RequestParam LotStatus value,HttpSession s){current.requireAdmin(s);Lot l=lots.findById(id).orElseThrow();l.status=value;lots.save(l);}
  @PostMapping("/broadcast") public BroadcastLog broadcast(@RequestBody BroadcastRequest req,HttpSession s){current.requireAdmin(s);List<AppUser> audience=users.findAll().stream().filter(u->u.registered).toList();int delivered=0;for(AppUser u:audience){try{if(bot.sendText(u.maxUserId,req.text))delivered++;}catch(Exception ignored){}}return broadcasts.save(new BroadcastLog(req.text,audience.size(),delivered));}
  @PostMapping(value="/lots",consumes="multipart/form-data") @Transactional
  public Map<String,Long> create(@RequestParam String title,@RequestParam String description,@RequestParam(required=false) String vin,@RequestParam(required=false) String mileage,@RequestParam(required=false) String autotecaUrl,@RequestParam long startingPrice,@RequestParam long bidStep,@RequestParam long durationMinutes,@RequestParam(defaultValue="true") boolean publish,@RequestPart(required=false) List<MultipartFile> media,HttpSession s) throws IOException {
    current.requireSuperAdmin(s);if(startingPrice<0||bidStep<=0||durationMinutes<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Проверьте цены и длительность");Lot l=new Lot();l.title=title;l.description=description;l.vin=vin;l.mileage=mileage;l.autotecaUrl=autotecaUrl;l.startingPrice=startingPrice;l.currentPrice=startingPrice;l.bidStep=bidStep;l.startsAt=Instant.now();l.endsAt=l.startsAt.plus(Duration.ofMinutes(durationMinutes));l.status=publish?LotStatus.LIVE:LotStatus.DRAFT;lots.save(l);
    if(media!=null){Path dir=Files.createDirectories(Path.of(props.uploadDir()).resolve(String.valueOf(l.id)));int i=0;for(MultipartFile f:media){if(f.isEmpty())continue;String type=Objects.requireNonNullElse(f.getContentType(),"");if(!type.startsWith("image/")&&!type.startsWith("video/"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Разрешены изображения и видео");String ext=type.startsWith("video/")?".mp4":".jpg";String name=UUID.randomUUID()+ext;Files.copy(f.getInputStream(),dir.resolve(name),StandardCopyOption.REPLACE_EXISTING);l.media.add(new LotMedia(l,"/uploads/"+l.id+"/"+name,type.startsWith("video/")?"video":"image",i++));}}lots.save(l);return Map.of("id",l.id);
  }
}
