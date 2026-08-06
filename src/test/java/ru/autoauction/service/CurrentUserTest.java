package ru.autoauction.service;

import org.junit.jupiter.api.Test;
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
  void adminAccessDependsOnStoredRole() {
    UserRepository users=mock(UserRepository.class);
    AppUser user=new AppUser(42L,"Участник", Role.USER);user.id=7L;user.registered=true;user.phoneVerified=true;
    when(users.findById(7L)).thenReturn(Optional.of(user));
    MockHttpSession session=new MockHttpSession();session.setAttribute(CurrentUser.SESSION_KEY,7L);
    CurrentUser current=new CurrentUser(users);

    assertThrows(ResponseStatusException.class,()->current.requireAdmin(session));

    user.role=Role.ADMIN;
    assertEquals(user,current.requireAdmin(session));
    assertThrows(ResponseStatusException.class,()->current.requireSuperAdmin(session));

    user.role=Role.SUPER_ADMIN;
    assertEquals(user,current.requireAdmin(session));
    assertEquals(user,current.requireSuperAdmin(session));
  }
}
