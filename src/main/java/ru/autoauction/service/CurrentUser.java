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
    AppUser user=users.findById((Long)id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if(Boolean.TRUE.equals(user.banned))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Доступ к приложению ограничен администратором");
    return user;
  }
  public AppUser requireRegistered(HttpSession session) {
    AppUser u=require(session);
    if(!u.registered||!Boolean.TRUE.equals(u.phoneVerified)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Сначала поделитесь номером телефона с ботом MAX");
    return u;
  }
  public AppUser requireAdmin(HttpSession session) {
    AppUser u=requireRegistered(session);
    if(u.role!=Role.ADMIN&&u.role!=Role.SUPER_ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Недостаточно прав");
    return u;
  }
  public AppUser requireSuperAdmin(HttpSession session) {
    AppUser u=requireRegistered(session);
    if(u.role!=Role.SUPER_ADMIN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Действие доступно только супер-администратору");
    return u;
  }
}
