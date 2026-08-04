package ru.autoauction.service;

import com.fasterxml.jackson.databind.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.config.AppProperties;
import ru.autoauction.model.*;
import ru.autoauction.repo.UserRepository;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class MaxAuthService {
  private final AppProperties props; private final UserRepository users; private final ObjectMapper json;
  public MaxAuthService(AppProperties props, UserRepository users, ObjectMapper json) { this.props=props; this.users=users; this.json=json; }
  public AppUser authenticate(String initData) {
    if(props.botToken()==null || props.botToken().isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"BOT_TOKEN не настроен");
    Map<String,String> data=parse(initData);
    String supplied=data.remove("hash");
    if(supplied==null || !MessageDigest.isEqual(hex(hmac(secret(), canonical(data))).getBytes(StandardCharsets.UTF_8), supplied.toLowerCase().getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Неверная подпись MAX");
    long authDate=Long.parseLong(data.getOrDefault("auth_date","0"));
    if(authDate<=0 || Instant.ofEpochSecond(authDate).isBefore(Instant.now().minus(props.maxAuthAge())))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Данные запуска устарели");
    try {
      JsonNode node=json.readTree(data.get("user")); long maxId=node.path("id").asLong();
      if(maxId<=0) throw new IllegalArgumentException();
      String name=(node.path("first_name").asText("")+" "+node.path("last_name").asText("")).trim();
      return upsert(maxId, name.isBlank()?"Пользователь MAX":name);
    } catch(Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Нет данных пользователя MAX"); }
  }
  public AppUser demo(long maxId, String name) {
    if(!props.demoAuth()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return upsert(maxId,name);
  }
  private AppUser upsert(long maxId,String name) {
    AppUser u=users.findByMaxUserId(maxId).orElseGet(()->new AppUser(maxId,name,role(maxId)));
    u.name=name; u.lastSeenAt=Instant.now();
    if(props.superAdminMaxIds().contains(maxId)) u.role=Role.SUPER_ADMIN;
    else if(props.adminMaxIds().contains(maxId) && u.role==Role.USER) u.role=Role.ADMIN;
    return users.save(u);
  }
  private Role role(long id) { return props.superAdminMaxIds().contains(id)?Role.SUPER_ADMIN:props.adminMaxIds().contains(id)?Role.ADMIN:Role.USER; }
  private Map<String,String> parse(String raw) {
    if(raw==null) return new HashMap<>(); Map<String,String> map=new HashMap<>();
    for(String p:raw.split("&")){int i=p.indexOf('='); if(i<1) continue; String k=decode(p.substring(0,i)); if(map.put(k,decode(p.substring(i+1)))!=null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Повторяющиеся параметры");}
    return map;
  }
  private String canonical(Map<String,String> d){return d.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+"="+e.getValue()).reduce((a,b)->a+"\n"+b).orElse("");}
  private byte[] secret(){return hmac("WebAppData".getBytes(StandardCharsets.UTF_8),props.botToken());}
  private byte[] hmac(byte[] key,String value){return hmac(key,value.getBytes(StandardCharsets.UTF_8));}
  private byte[] hmac(byte[] key,byte[] value){try{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key,"HmacSHA256"));return m.doFinal(value);}catch(Exception e){throw new IllegalStateException(e);}}
  private String hex(byte[] b){return HexFormat.of().formatHex(b);} private String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
}
