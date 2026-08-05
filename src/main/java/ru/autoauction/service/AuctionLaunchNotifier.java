package ru.autoauction.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.autoauction.repo.UserRepository;
import ru.autoauction.config.AppProperties;
import ru.autoauction.model.AppUser;

@Component
public class AuctionLaunchNotifier {
  private final UserRepository users;
  private final MaxBotService bot;
  private final AuctionEvents events;
  private final AppProperties props;

  public AuctionLaunchNotifier(UserRepository users, MaxBotService bot, AuctionEvents events, AppProperties props) {
    this.users = users;
    this.bot = bot;
    this.events = events;
    this.props = props;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onLotStarted(LotStartedEvent event) {
    events.lotStarted(event.lotId());
    var recipients = users.findAll().stream()
        .filter(user -> user.registered && !isDemoUser(user))
        .map(user -> user.maxUserId)
        .toList();
    bot.sendAuctionStarted(recipients, event.title(), event.startingPrice());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onLotCancelled(LotCancelledEvent event) {
    events.lotCancelled(event.lotId());
  }

  private boolean isDemoUser(AppUser user) {
    if (props.demoAuth() || props.adminMaxIds().contains(user.maxUserId) || props.superAdminMaxIds().contains(user.maxUserId)) return false;
    return (user.maxUserId == 1000001L && "Алексей Воронцов".equals(user.name))
        || (user.maxUserId == 1000002L && "Мария Соколова".equals(user.name));
  }
}
