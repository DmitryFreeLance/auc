package ru.autoauction.service;

import com.fasterxml.jackson.databind.*;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.autoauction.config.AppProperties;
import java.util.*;

@Service
public class MaxBotService {
  private static final Logger log=LoggerFactory.getLogger(MaxBotService.class);
  private final AppProperties props; private final RestClient client;
  public MaxBotService(AppProperties props,RestClient.Builder builder){this.props=props;this.client=builder.baseUrl("https://platform-api2.max.ru").build();}
  public boolean configured(){return props.botToken()!=null&&!props.botToken().isBlank();}
  public void sendOpen(long userId){
    Map<String,Object> button=Map.of("type","open_app","text","Открыть аукцион","web_app",webAppName());
    send(userId,"Добро пожаловать! Нажмите кнопку, чтобы открыть текущий аукцион.",List.of(Map.of("type","inline_keyboard","payload",Map.of("buttons",List.of(List.of(button))))));
  }
  public boolean sendText(long userId,String text){if(!configured())return false;send(userId,text,List.of());return true;}
  @Async public void sendOutbid(long userId,String lotTitle,long currentPrice){
    if(!configured())return;
    try{
      Map<String,Object> button=Map.of("type","open_app","text","Вернуть лидерство","web_app",webAppName());
      String price=String.format(Locale.forLanguageTag("ru-RU"),"%,d ₽",currentPrice);
      send(userId,"Вашу ставку на «"+lotTitle+"» перебили. Новая цена: "+price+".",List.of(Map.of("type","inline_keyboard","payload",Map.of("buttons",List.of(List.of(button))))));
    }catch(Exception e){log.warn("Не удалось отправить уведомление о перебитой ставке пользователю {}: {}",userId,e.getMessage());}
  }
  @Async public void sendAuctionStarted(List<Long> userIds,String lotTitle,long startingPrice){
    if(!configured()||userIds.isEmpty())return;
    String price=String.format(Locale.forLanguageTag("ru-RU"),"%,d ₽",startingPrice);
    Map<String,Object> button=Map.of("type","open_app","text","Открыть аукцион","web_app",webAppName());
    List<?> keyboard=List.of(Map.of("type","inline_keyboard","payload",Map.of("buttons",List.of(List.of(button)))));
    int delivered=0;
    for(long userId:userIds){
      try{send(userId,"🚘 Новый лот уже в эфире!\n«"+lotTitle+"»\nСтартовая цена: "+price+". Успейте сделать ставку.",keyboard);delivered++;}
      catch(Exception e){log.debug("Уведомление о старте не доставлено пользователю {}: {}",userId,e.getMessage());}
      try{Thread.sleep(40);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
    }
    log.info("Уведомление о старте лота отправлено: {} из {}",delivered,userIds.size());
  }
  private void send(long userId,String text,List<?> attachments){
    if(!configured()) return;
    client.post().uri(u->u.path("/messages").queryParam("user_id",userId).build()).header("Authorization",props.botToken()).contentType(MediaType.APPLICATION_JSON).body(Map.of("text",text,"attachments",attachments)).retrieve().toBodilessEntity();
  }
  public long extractUserId(JsonNode update){
    long id=update.path("user").path("user_id").asLong(); if(id==0)id=update.path("user").path("id").asLong();
    if(id==0)id=update.path("message").path("sender").path("user_id").asLong(); return id;
  }
  private String webAppName(){
    String value=Objects.requireNonNullElse(props.botUsername(),"").trim();
    if(value.startsWith("@"))value=value.substring(1);
    if(value.isBlank())throw new IllegalStateException("MAX_BOT_USERNAME не задан");
    return value;
  }
}
