package ru.autoauction.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.AppUser;
import ru.autoauction.service.*;
import static ru.autoauction.web.ApiDtos.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final MaxAuthService auth; private final CurrentUser current;
  public AuthController(MaxAuthService auth,CurrentUser current){this.auth=auth;this.current=current;}
  public record MaxLogin(String initData,String startParam){}
  public record DemoLogin(long maxUserId,String name){}
  @PostMapping("/max") public UserDto max(@RequestBody MaxLogin req,HttpSession s){return login(auth.authenticate(req.initData),s);}
  @PostMapping("/demo") public UserDto demo(@RequestBody DemoLogin req,HttpSession s){return login(auth.demo(req.maxUserId,req.name==null?"Demo":req.name),s);}
  @GetMapping("/me") public UserDto me(HttpSession s){return UserDto.of(current.require(s));}
  @PostMapping("/logout") public void logout(HttpSession s){s.invalidate();}
  private UserDto login(AppUser u,HttpSession s){if(Boolean.TRUE.equals(u.banned))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Доступ к приложению ограничен администратором");s.setAttribute(CurrentUser.SESSION_KEY,u.id);return UserDto.of(u);}
}
