package ru.autoauction.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuctionEvents {
  private static final long GLOBAL = -1L;
  private record Listener(long userId, SseEmitter emitter) {}
  private final Map<Long, CopyOnWriteArrayList<Listener>> listeners = new ConcurrentHashMap<>();

  public SseEmitter subscribeGlobal(long userId) {
    return subscribe(GLOBAL, userId);
  }

  public SseEmitter subscribe(long lotId, long userId) {
    SseEmitter emitter = new SseEmitter(0L);
    Listener listener = new Listener(userId, emitter);
    listeners.computeIfAbsent(lotId, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    Runnable cleanup = () -> remove(lotId, listener);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(cleanup);
    emitter.onError(ignored -> cleanup.run());
    try { emitter.send(SseEmitter.event().name("connected").data(lotId == GLOBAL ? Map.of("scope", "auction") : Map.of("lotId", lotId))); }
    catch (Exception e) { cleanup.run(); }
    return emitter;
  }

  public void bidUpdated(long lotId, long currentPrice, long leaderId) {
    Map<String,Long> update=Map.of("lotId",lotId,"currentPrice",currentPrice,"leaderId",leaderId);
    send(lotId, SseEmitter.event().name("bid-updated").data(update));
    send(GLOBAL, SseEmitter.event().name("bid-updated").data(update));
  }

  public void lotStarted(long lotId) {
    send(GLOBAL, SseEmitter.event().name("lot-started").data(Map.of("lotId", lotId)));
  }

  public void lotCancelled(long lotId) {
    send(GLOBAL, SseEmitter.event().name("lot-cancelled").data(Map.of("lotId", lotId)));
  }

  public long onlineUsers() {
    return listeners.values().stream().flatMap(java.util.Collection::stream).map(Listener::userId).distinct().count();
  }

  public long onlineUsers(long lotId) {
    return listeners.getOrDefault(lotId,new CopyOnWriteArrayList<>()).stream().map(Listener::userId).distinct().count();
  }

  public Map<Long,Long> onlineByLot() {
    Map<Long,Long> result=new java.util.HashMap<>();
    listeners.forEach((lotId,connections)->{if(lotId!=GLOBAL)result.put(lotId,connections.stream().map(Listener::userId).distinct().count());});
    return result;
  }

  @Scheduled(fixedRate = 15_000)
  public void keepAlive() {
    listeners.forEach((lotId, connections) -> connections.forEach(listener -> {
      try { listener.emitter().send(SseEmitter.event().comment("keepalive")); }
      catch (Exception e) { remove(lotId, listener); }
    }));
  }

  private void send(long lotId, SseEmitter.SseEventBuilder event) {
    listeners.getOrDefault(lotId, new CopyOnWriteArrayList<>()).forEach(listener -> {
      try { listener.emitter().send(event); }
      catch (Exception e) { remove(lotId, listener); }
    });
  }

  private void remove(long lotId, Listener listener) {
    var connections = listeners.get(lotId);
    if (connections == null) return;
    connections.remove(listener);
    if (connections.isEmpty()) listeners.remove(lotId, connections);
  }
}
