package ru.autoauction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.autoauction.model.*;
import ru.autoauction.repo.*;
import java.time.*;

@Component
public class DemoData implements CommandLineRunner {
  private final LotRepository lots;private final UserRepository users;
  public DemoData(LotRepository lots,UserRepository users){this.lots=lots;this.users=users;}
  @Override public void run(String... args){
    users.findByMaxUserId(1000001L).orElseGet(()->{AppUser u=new AppUser(1000001L,"Алексей Воронцов",Role.SUPER_ADMIN);u.phone="+79990001122";u.registered=true;return users.save(u);});
    users.findByMaxUserId(1000002L).orElseGet(()->{AppUser u=new AppUser(1000002L,"Мария Соколова",Role.USER);u.phone="+79995557788";u.registered=true;return users.save(u);});
    if(lots.count()>0)return;Lot l=new Lot();l.title="Aurum E5 xDrive, 2021";l.description="Полноприводный бизнес-седан в отличном состоянии. Один владелец, обслуживание у официального дилера, оригинальный ПТС. Комплектация: адаптивная оптика, кожаный салон, круговой обзор, проекция, подогрев и вентиляция сидений.";l.vin="WBA••••••••74291";l.mileage="38 400 км";l.autotecaUrl="https://autoteka.ru/";l.startingPrice=3_850_000;l.currentPrice=4_120_000;l.bidStep=50_000;l.startsAt=Instant.now().minus(Duration.ofMinutes(12));l.endsAt=Instant.now().plus(Duration.ofHours(2)).plus(Duration.ofMinutes(18));l.status=LotStatus.LIVE;l.media.add(new LotMedia(l,"/assets/car-main.jpg","image",0));l.media.add(new LotMedia(l,"/assets/car-detail.jpg","image",1));l.media.add(new LotMedia(l,"/assets/car-wide.jpg","image",2));lots.save(l);
  }
}
