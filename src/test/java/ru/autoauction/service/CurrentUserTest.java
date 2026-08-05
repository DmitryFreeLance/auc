package ru.autoauction.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.model.AppUser;
import ru.autoauction.model.Role;
import ru.autoauction.repo.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserTest {
  @Test
  void adminAccessRequiresTokenEvenForRegularUser() {
    UserRepository users=mock(UserRepository.class);
    AppUser user=new AppUser(42L,"Участник", Role.USER);user.id=7L;user.registered=true;
    when(users.findById(7L)).thenReturn(Optional.of(user));
    MockHttpServletRequest request=new MockHttpServletRequest();
    MockHttpSession session=new MockHttpSession();session.setAttribute(CurrentUser.SESSION_KEY,7L);session.setAttribute(CurrentUser.ADMIN_TOKEN_KEY,"secret-session-token");
    CurrentUser current=new CurrentUser(users,request);

    assertThrows(ResponseStatusException.class,()->current.requireAdmin(session));

    request.addHeader("X-Admin-Session","secret-session-token");
    assertEquals(user,current.requireAdmin(session));
    assertEquals(user,current.requireSuperAdmin(session));

    user.role=Role.SUPER_ADMIN;session.removeAttribute(CurrentUser.ADMIN_TOKEN_KEY);
    assertThrows(ResponseStatusException.class,()->current.requireSuperAdmin(session));
  }
}
