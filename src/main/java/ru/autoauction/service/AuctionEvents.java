package ru.autoauction.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuctionEvents {
  private final Map<Long, CopyOnWriteArrayList<SseEmitter>> listeners = new ConcurrentHashMap<>();

  public SseEmitter subscribe(long lotId) {
    SseEmitter emitter = new SseEmitter(0L);
    listeners.computeIfAbsent(lotId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
    Runnable cleanup = () -> remove(lotId, emitter);
    emitter.onCompletion(cleanup);
    emitter.onTimeout(cleanup);
    emitter.onError(ignored -> cleanup.run());
    try { emitter.send(SseEmitter.event().name("connected").data(Map.of("lotId", lotId))); }
    catch (IOException e) { cleanup.run(); }
    return emitter;
  }

  public void bidUpdated(long lotId, long currentPrice, long leaderId) {
    send(lotId, SseEmitter.event().name("bid-updated")
        .data(Map.of("lotId", lotId, "currentPrice", currentPrice, "leaderId", leaderId)));
  }

  @Scheduled(fixedRate = 15_000)
  public void keepAlive() {
    listeners.forEach((lotId, emitters) -> emitters.forEach(emitter -> {
      try { emitter.send(SseEmitter.event().comment("keepalive")); }
      catch (IOException e) { remove(lotId, emitter); }
    }));
  }

  private void send(long lotId, SseEmitter.SseEventBuilder event) {
    listeners.getOrDefault(lotId, new CopyOnWriteArrayList<>()).forEach(emitter -> {
      try { emitter.send(event); }
      catch (IOException e) { remove(lotId, emitter); }
    });
  }

  private void remove(long lotId, SseEmitter emitter) {
    var emitters = listeners.get(lotId);
    if (emitters == null) return;
    emitters.remove(emitter);
    if (emitters.isEmpty()) listeners.remove(lotId, emitters);
  }
}
