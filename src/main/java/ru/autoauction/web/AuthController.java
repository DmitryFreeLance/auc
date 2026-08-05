package ru.autoauction.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.AppUser;
import ru.autoauction.repo.UserRepository;
import ru.autoauction.service.*;
import static ru.autoauction.web.ApiDtos.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final MaxAuthService auth; private final CurrentUser current; private final UserRepository users;
  public AuthController(MaxAuthService auth,CurrentUser current,UserRepository users){this.auth=auth;this.current=current;this.users=users;}
  public record MaxLogin(String initData,String startParam){}
  public record DemoLogin(long maxUserId,String name){}
  public record Registration(@NotBlank @Size(min=2,max=80) String name,@NotBlank @Pattern(regexp="^\\+?[0-9 ()-]{10,20}$") String phone){}
  @PostMapping("/max") public UserDto max(@RequestBody MaxLogin req,HttpSession s){return login(auth.authenticate(req.initData),s);}
  @PostMapping("/demo") public UserDto demo(@RequestBody DemoLogin req,HttpSession s){return login(auth.demo(req.maxUserId,req.name==null?"Demo":req.name),s);}
  @GetMapping("/me") public UserDto me(HttpSession s){return UserDto.of(current.require(s));}
  @PostMapping("/register") public UserDto register(@Valid @RequestBody Registration req,HttpSession s){AppUser u=current.require(s);u.name=req.name.trim();u.phone=req.phone.replaceAll("[ ()-]","");u.registered=true;return UserDto.of(users.save(u));}
  @PostMapping("/logout") public void logout(HttpSession s){s.invalidate();}
  private UserDto login(AppUser u,HttpSession s){if(Boolean.TRUE.equals(u.banned))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Доступ к приложению ограничен администратором");s.setAttribute(CurrentUser.SESSION_KEY,u.id);return UserDto.of(u);}
}
