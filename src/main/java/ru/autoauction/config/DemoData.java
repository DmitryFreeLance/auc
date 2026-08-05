package ru.autoauction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.autoauction.model.*;
import ru.autoauction.repo.*;
import ru.autoauction.config.AppProperties;

@Component
public class DemoData implements CommandLineRunner {
  private final LotRepository lots;private final UserRepository users;private final BidRepository bids;private final AppProperties props;
  public DemoData(LotRepository lots,UserRepository users,BidRepository bids,AppProperties props){this.lots=lots;this.users=users;this.bids=bids;this.props=props;}
  @Override @Transactional public void run(String... args){
    lots.findAll().stream().filter(this::isOldDemoLot).toList().forEach(lot->{bids.deleteAll(bids.findByLotIdOrderByCreatedAtDesc(lot.id));lots.delete(lot);});
    if(!props.demoAuth()){removeUnusedDemoUser(1000001L);removeUnusedDemoUser(1000002L);return;}
    users.findByMaxUserId(1000001L).orElseGet(()->{AppUser u=new AppUser(1000001L,"Алексей Воронцов",Role.SUPER_ADMIN);u.phone="+79990001122";u.registered=true;return users.save(u);});
    users.findByMaxUserId(1000002L).orElseGet(()->{AppUser u=new AppUser(1000002L,"Мария Соколова",Role.USER);u.phone="+79995557788";u.registered=true;return users.save(u);});
  }
  private void removeUnusedDemoUser(long maxUserId){if(props.adminMaxIds().contains(maxUserId)||props.superAdminMaxIds().contains(maxUserId))return;users.findByMaxUserId(maxUserId).filter(user->bids.countByUserId(user.id)==0).ifPresent(users::delete);}
  private boolean isOldDemoLot(Lot lot){return "Aurum E5 xDrive, 2021".equals(lot.title)&&"WBA••••••••74291".equals(lot.vin)&&lot.startingPrice==3_850_000;}
}
