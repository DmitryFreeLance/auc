package ru.autoauction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.autoauction.config.AppProperties;
import ru.autoauction.repo.UserRepository;
import java.time.Duration;
import java.util.Set;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ru.autoauction.model.AppUser;

class MaxAuthServiceTest {
  @Test void demoAuthenticationIsDisabledInProduction(){
    var props=new AppProperties("token","bot","https://example.ru",Set.of(),Set.of(),false,Duration.ofHours(1),"tmp");
    var service=new MaxAuthService(props,mock(UserRepository.class),new ObjectMapper());
    assertThrows(Exception.class,()->service.demo(1,"Demo"));
  }
  @Test void acceptsCorrectlySignedMaxData() throws Exception {
    String token="test-token";var repo=mock(UserRepository.class);
    when(repo.findByMaxUserId(42L)).thenReturn(Optional.empty());when(repo.save(any())).thenAnswer(i->{AppUser u=i.getArgument(0);u.id=7L;return u;});
    var props=new AppProperties(token,"bot","https://example.ru",Set.of(),Set.of(42L),false,Duration.ofHours(1),"tmp");
    String user="{\"id\":42,\"first_name\":\"Max\",\"last_name\":\"User\"}";
    String auth=String.valueOf(Instant.now().getEpochSecond());String canonical="auth_date="+auth+"\nuser="+user;
    byte[] secret=hmac("WebAppData".getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8));String hash=HexFormat.of().formatHex(hmac(secret,canonical.getBytes(StandardCharsets.UTF_8)));
    String init="user="+URLEncoder.encode(user,StandardCharsets.UTF_8)+"&auth_date="+auth+"&hash="+hash;
    AppUser result=new MaxAuthService(props,repo,new ObjectMapper()).authenticate(init);
    assertEquals(42L,result.maxUserId);assertEquals("Max User",result.name);assertEquals(ru.autoauction.model.Role.SUPER_ADMIN,result.role);
  }
  private static byte[] hmac(byte[] key,byte[] value)throws Exception{Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(key,"HmacSHA256"));return m.doFinal(value);}
}
