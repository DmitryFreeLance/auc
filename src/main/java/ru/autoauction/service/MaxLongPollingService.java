package ru.autoauction.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ru.autoauction.config.AppProperties;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class MaxLongPollingService {
  private static final Logger log=LoggerFactory.getLogger(MaxLongPollingService.class);
  private final AppProperties props;
  private final MaxBotService bot;
  private final BotRegistrationService registration;
  private final RestClient client;
  private final AtomicBoolean running=new AtomicBoolean();
  private volatile Thread worker;
  private volatile Long marker;

  public MaxLongPollingService(AppProperties props,MaxBotService bot,BotRegistrationService registration,RestClient.Builder builder){
    this.props=props;
    this.bot=bot;
    this.registration=registration;
    this.client=builder.baseUrl("https://platform-api2.max.ru").build();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start(){
    if(!bot.configured()){
      log.warn("MAX Long Polling не запущен: MAX_BOT_TOKEN не задан");
      return;
    }
    if(!running.compareAndSet(false,true))return;
    worker=Thread.ofVirtual().name("max-long-polling").start(this::pollLoop);
    log.info("MAX Long Polling запущен");
  }

  private void pollLoop(){
    long retryDelay=1000;
    while(running.get()){
      try{
        JsonNode response=client.get().uri(uri->{
          uri.path("/updates").queryParam("timeout",30).queryParam("limit",100).queryParam("types","bot_started,message_created");
          if(marker!=null)uri.queryParam("marker",marker);
          return uri.build();
        }).header("Authorization",props.botToken()).retrieve().body(JsonNode.class);
        if(response==null)continue;
        JsonNode updates=response.path("updates");
        if(updates.isArray())updates.forEach(this::handle);
        if(response.hasNonNull("marker"))marker=response.path("marker").asLong();
        retryDelay=1000;
      }catch(RestClientResponseException e){
        log.error("MAX Long Polling: HTTP {}: {}",e.getStatusCode().value(),shortText(e.getResponseBodyAsString()));
        pause(retryDelay);
        retryDelay=Math.min(retryDelay*2,30000);
      }catch(Exception e){
        if(running.get())log.error("MAX Long Polling: {}",e.getMessage(),e);
        pause(retryDelay);
        retryDelay=Math.min(retryDelay*2,30000);
      }
    }
  }

  private void handle(JsonNode update){
    String type=update.path("update_type").asText();
    String text=update.path("message").path("body").path("text").asText("").trim();
    boolean start="bot_started".equals(type)||("message_created".equals(type)&&(text.equalsIgnoreCase("начать")||text.toLowerCase(Locale.ROOT).startsWith("/start")));
    JsonNode contact=findContact(update);
    if(!start&&contact==null)return;
    long userId=bot.extractUserId(update);
    if(userId<=0){
      log.warn("MAX {} получен без user_id",type);
      return;
    }
    try{
      String name=bot.extractUserName(update);
      if(contact!=null){
        try{registration.registerVerifiedContact(userId,name,contact.path("payload"));bot.sendOpenAfterRegistration(userId);log.info("MAX: пользователь {} подтвердил номер телефона",userId);}
        catch(IllegalArgumentException e){log.warn("MAX: отклонён неподтверждённый контакт пользователя {}: {}",userId,e.getMessage());bot.sendContactRejected(userId);}
        return;
      }
      var user=registration.observe(userId,name);
      if(user.registered&&Boolean.TRUE.equals(user.phoneVerified)){bot.sendOpen(userId);log.info("MAX: кнопка аукциона отправлена пользователю {}",userId);}
      else{bot.sendContactRequest(userId);log.info("MAX: пользователю {} запрошен подтверждённый номер телефона",userId);}
    }catch(Exception e){
      log.error("MAX: не удалось ответить пользователю {}: {}",userId,e.getMessage(),e);
    }
  }

  private JsonNode findContact(JsonNode update){JsonNode attachments=update.path("message").path("body").path("attachments");if(!attachments.isArray())attachments=update.path("message").path("attachments");if(attachments.isArray())for(JsonNode attachment:attachments)if("contact".equals(attachment.path("type").asText()))return attachment;return null;}

  private void pause(long millis){
    try{Thread.sleep(millis);}catch(InterruptedException e){Thread.currentThread().interrupt();}
  }

  private String shortText(String value){
    if(value==null)return "";
    return value.length()>500?value.substring(0,500):value;
  }

  @PreDestroy
  public void stop(){
    running.set(false);
    Thread thread=worker;
    if(thread!=null)thread.interrupt();
  }
}
