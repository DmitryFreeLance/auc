package ru.autoauction.web;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.AppUser;
import ru.autoauction.repo.UserRepository;
import ru.autoauction.service.*;
import static ru.autoauction.web.ApiDtos.*;
import ru.autoauction.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@RestController @RequestMapping("/api/auth")
public class AuthController {
  private static final String ADMIN_ATTEMPTS="adminPasswordAttempts",ADMIN_LOCK_UNTIL="adminPasswordLockUntil";
  private final MaxAuthService auth; private final CurrentUser current; private final UserRepository users;private final AppProperties props;private final AdminAccessTokens adminTokens;
  public AuthController(MaxAuthService auth,CurrentUser current,UserRepository users,AppProperties props,AdminAccessTokens adminTokens){this.auth=auth;this.current=current;this.users=users;this.props=props;this.adminTokens=adminTokens;}
  public record MaxLogin(String initData,String startParam){}
  public record DemoLogin(long maxUserId,String name){}
  public record Registration(@NotBlank @Size(min=2,max=80) String name,@NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String phone){}
  public record AdminAccess(@NotBlank String password){} public record AdminAccessResult(String token){}
  @PostMapping("/max") public UserDto max(@RequestBody MaxLogin req,HttpSession s){return login(auth.authenticate(req.initData),s);}
  @PostMapping("/demo") public UserDto demo(@RequestBody DemoLogin req,HttpSession s){return login(auth.demo(req.maxUserId,req.name==null?"Demo":req.name),s);}
  @GetMapping("/me") public UserDto me(HttpSession s){return UserDto.of(current.require(s));}
  @PostMapping("/register") public UserDto register(@Valid @RequestBody Registration req,HttpSession s){AppUser u=current.require(s);u.name=req.name.trim();u.phone=req.phone.replaceAll("[ ()-]","");u.registered=true;return UserDto.of(users.save(u));}
  @PostMapping("/admin-access") public AdminAccessResult adminAccess(@Valid @RequestBody AdminAccess req,HttpSession s){AppUser actor=current.requireRegistered(s);s.removeAttribute(CurrentUser.ADMIN_TOKEN_KEY);Instant locked=(Instant)s.getAttribute(ADMIN_LOCK_UNTIL);if(locked!=null&&locked.isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Слишком много попыток. Повторите через несколько минут");String expected=props.adminAccessPassword();if(expected.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Пароль админ-панели не настроен");if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),req.password.getBytes(StandardCharsets.UTF_8))){int attempts=s.getAttribute(ADMIN_ATTEMPTS)instanceof Integer n?n+1:1;s.setAttribute(ADMIN_ATTEMPTS,attempts);if(attempts>=5){s.setAttribute(ADMIN_LOCK_UNTIL,Instant.now().plusSeconds(300));s.removeAttribute(ADMIN_ATTEMPTS);}throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Неверный пароль");}String token=adminTokens.issue(actor.id);s.setAttribute(CurrentUser.ADMIN_TOKEN_KEY,token);s.removeAttribute(ADMIN_ATTEMPTS);s.removeAttribute(ADMIN_LOCK_UNTIL);return new AdminAccessResult(token);}
  @PostMapping("/admin-lock") public void adminLock(HttpSession s,HttpServletRequest request){current.require(s);adminTokens.revoke(request.getHeader("X-Admin-Session"));s.removeAttribute(CurrentUser.ADMIN_TOKEN_KEY);}
  @PostMapping("/logout") public void logout(HttpSession s){s.invalidate();}
  private UserDto login(AppUser u,HttpSession s){if(Boolean.TRUE.equals(u.banned))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Доступ к приложению ограничен администратором");s.setAttribute(CurrentUser.SESSION_KEY,u.id);return UserDto.of(u);}
}
