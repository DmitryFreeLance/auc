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
    ensureDemoUser(1000001L,"Алексей Воронцов","+79990001122",Role.SUPER_ADMIN);
    ensureDemoUser(1000002L,"Мария Соколова","+79995557788",Role.USER);
  }
  private void removeUnusedDemoUser(long maxUserId){if(props.adminMaxIds().contains(maxUserId)||props.superAdminMaxIds().contains(maxUserId))return;users.findByMaxUserId(maxUserId).filter(user->bids.countByUserId(user.id)==0).ifPresent(users::delete);}
  private void ensureDemoUser(long maxUserId,String name,String phone,Role role){AppUser user=users.findByMaxUserId(maxUserId).orElseGet(()->new AppUser(maxUserId,name,role));user.name=name;user.phone=phone;user.phoneVerified=true;user.registered=true;user.role=role;users.save(user);}
  private boolean isOldDemoLot(Lot lot){return "Aurum E5 xDrive, 2021".equals(lot.title)&&"WBA••••••••74291".equals(lot.vin)&&lot.startingPrice==3_850_000;}
}
