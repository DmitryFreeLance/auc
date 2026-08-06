package ru.autoauction.service;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminAccessTokens {
  private record Access(Long userId,Instant expiresAt){}
  private final ConcurrentHashMap<String,Access> tokens=new ConcurrentHashMap<>();
  public String issue(Long userId){cleanup();String token=UUID.randomUUID().toString()+UUID.randomUUID();tokens.put(token,new Access(userId,Instant.now().plus(Duration.ofHours(12))));return token;}
  public boolean valid(String token,Long userId){if(token==null)return false;Access access=tokens.get(token);if(access==null)return false;if(!access.expiresAt.isAfter(Instant.now())){tokens.remove(token);return false;}return access.userId.equals(userId);}
  public void revoke(String token){if(token!=null)tokens.remove(token);}
  private void cleanup(){Instant now=Instant.now();tokens.entrySet().removeIf(entry->!entry.getValue().expiresAt.isAfter(now));}
}
