package ru.autoauction.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.autoauction.repo.UserRepository;

@Component
public class AuctionLaunchNotifier {
  private final UserRepository users;
  private final MaxBotService bot;
  private final AuctionEvents events;

  public AuctionLaunchNotifier(UserRepository users, MaxBotService bot, AuctionEvents events) {
    this.users = users;
    this.bot = bot;
    this.events = events;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onLotStarted(LotStartedEvent event) {
    events.lotStarted(event.lotId());
    var recipients = users.findAll().stream()
        .filter(user -> user.registered)
        .map(user -> user.maxUserId)
        .toList();
    bot.sendAuctionStarted(recipients, event.title(), event.startingPrice());
  }
}
