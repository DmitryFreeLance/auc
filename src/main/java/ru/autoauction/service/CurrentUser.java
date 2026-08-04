package ru.autoauction.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.*;
import ru.autoauction.repo.UserRepository;

@Component
public class CurrentUser {
  public static final String SESSION_KEY="userId";
  private final UserRepository users;
  public CurrentUser(UserRepository users) { this.users=users; }
  public AppUser require(HttpSession session) {
    Object id=session.getAttribute(SESSION_KEY);
    if (!(id instanceof Long)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Откройте приложение из MAX");
    return users.findById((Long)id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }
  public AppUser requireRegistered(HttpSession session) {
    AppUser u=require(session);
    if(!u.registered) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Сначала завершите регистрацию");
    return u;
  }
  public AppUser requireAdmin(HttpSession session) {
    AppUser u=requireRegistered(session);
    if(u.role==Role.USER) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Недостаточно прав");
    return u;
  }
  public AppUser requireSuperAdmin(HttpSession session) {
    AppUser u=requireAdmin(session);
    if(u.role!=Role.SUPER_ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Нужны права супер-администратора");
    return u;
  }
}
