package ru.autoauction.service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.*;
import ru.autoauction.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class CurrentUser {
  public static final String SESSION_KEY="userId";
  public static final String ADMIN_TOKEN_KEY="adminAccessToken";
  private final UserRepository users;
  private final HttpServletRequest request;
  private final AdminAccessTokens adminTokens;
  public CurrentUser(UserRepository users,HttpServletRequest request,AdminAccessTokens adminTokens) { this.users=users;this.request=request;this.adminTokens=adminTokens; }
  public AppUser require(HttpSession session) {
    Object id=session.getAttribute(SESSION_KEY);
    if (!(id instanceof Long)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Откройте приложение из MAX");
    AppUser user=users.findById((Long)id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if(Boolean.TRUE.equals(user.banned))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Доступ к приложению ограничен администратором");
    return user;
  }
  public AppUser requireRegistered(HttpSession session) {
    AppUser u=require(session);
    if(!u.registered) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Сначала завершите регистрацию");
    return u;
  }
  public AppUser requireAdmin(HttpSession session) {
    AppUser u=requireRegistered(session);
    Object expected=session.getAttribute(ADMIN_TOKEN_KEY);String supplied=request.getHeader("X-Admin-Session");
    boolean sessionMatch=expected instanceof String token&&supplied!=null&&MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8));
    if(!sessionMatch&&!adminTokens.valid(supplied,u.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Введите пароль администратора");
    return u;
  }
  public AppUser requireSuperAdmin(HttpSession session) {
    return requireAdmin(session);
  }
}
