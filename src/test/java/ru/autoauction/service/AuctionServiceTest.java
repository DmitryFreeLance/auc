package ru.autoauction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.autoauction.model.*;
import ru.autoauction.repo.*;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {
  @Mock LotRepository lots;
  @Mock BidRepository bids;
  AuctionService service;
  Lot lot;
  AppUser user;

  @BeforeEach void setUp(){
    service=new AuctionService(lots,bids);
    lot=new Lot();lot.id=7L;lot.title="Автомобиль";lot.description="Описание";lot.currentPrice=4_120_000;lot.bidStep=50_000;lot.startsAt=Instant.now().minusSeconds(60);lot.endsAt=Instant.now().plusSeconds(3600);lot.status=LotStatus.LIVE;
    user=new AppUser(42L,"Участник",Role.USER);user.id=9L;
    when(lots.findById(7L)).thenReturn(Optional.of(lot));when(bids.findFirstByLotIdOrderByAmountDescCreatedAtAsc(7L)).thenReturn(Optional.empty());when(bids.save(any(Bid.class))).thenAnswer(invocation->invocation.getArgument(0));when(lots.save(any(Lot.class))).thenAnswer(invocation->invocation.getArgument(0));
  }

  @Test void usesMinimumBidWhenRequestedAmountIsTooLow(){assertThat(service.placeBid(7L,user,4_130_000L).bid().amount).isEqualTo(4_170_000);}
  @Test void roundsCustomBidUpByCurrentStep(){assertThat(service.placeBid(7L,user,4_234_000L).bid().amount).isEqualTo(4_270_000);}
}
