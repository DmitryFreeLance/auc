package ru.autoauction.web;

import ru.autoauction.model.*;
import java.time.Instant;
import java.util.*;

public final class ApiDtos {
  private ApiDtos(){}
  public record UserDto(Long id,Long maxUserId,String name,String phone,Role role,boolean registered) { public static UserDto of(AppUser u){return new UserDto(u.id,u.maxUserId,u.name,u.phone,u.role,u.registered);} }
  public record MediaDto(Long id,String url,String type){}
  public record LotDto(Long id,String title,String description,String vin,String mileage,String autotecaUrl,long startingPrice,long currentPrice,long bidStep,Instant startsAt,Instant endsAt,LotStatus status,List<MediaDto> media,long bids,Long leaderId,String leaderName,Long myBid){}
  public record AuctionHistoryDto(Long id,String title,String mileage,Instant endedAt,long finalPrice,String winnerName,String imageUrl,long bids){}
  public record BidDto(Long id,long amount,Instant createdAt,Long userId,String userName,String maskedPhone){
    public static BidDto of(Bid b){String p=b.user.phone;String masked=p==null?"—":p.replaceAll("(\\+?\\d{2})\\d+(\\d{2})","$1•••••••$2");return new BidDto(b.id,b.amount,b.createdAt,b.user.id,b.user.name,masked);}
  }
}
