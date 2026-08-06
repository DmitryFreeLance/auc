package ru.autoauction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.autoauction.config.AppProperties;
import ru.autoauction.model.AppUser;
import ru.autoauction.repo.UserRepository;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BotRegistrationServiceTest {
  @Test void storesOnlyCorrectlySignedMaxContact() throws Exception {
    String token="bot-token",vcf="BEGIN:VCARD\r\nVERSION:3.0\r\nTEL;TYPE=cell:79990000000\r\nFN:Ivan Ivanov\r\nEND:VCARD\r\n";
    UserRepository users=mock(UserRepository.class);when(users.findByMaxUserId(42L)).thenReturn(Optional.empty());when(users.save(any())).thenAnswer(call->{AppUser user=call.getArgument(0);user.id=7L;return user;});
    BotRegistrationService service=new BotRegistrationService(props(token),users);
    var payload=new ObjectMapper().createObjectNode().put("vcf_info",vcf).put("hash",hmac(token,vcf));

    AppUser user=service.registerVerifiedContact(42L,"Иван Иванов",payload);

    assertEquals("+79990000000",user.phone);assertEquals("Иван Иванов",user.name);assertTrue(user.registered);assertTrue(user.phoneVerified);
  }

  @Test void rejectsForwardedContactWithoutMaxHash(){
    UserRepository users=mock(UserRepository.class);BotRegistrationService service=new BotRegistrationService(props("bot-token"),users);
    var payload=new ObjectMapper().createObjectNode().put("vcf_info","BEGIN:VCARD\r\nTEL:79990000000\r\nEND:VCARD\r\n");
    assertThrows(IllegalArgumentException.class,()->service.registerVerifiedContact(42L,"Иван",payload));verify(users,never()).save(any());
  }

  private AppProperties props(String token){return new AppProperties(token,"bot","https://example.ru",Set.of(),Set.of(),false,Duration.ofHours(24),"tmp");}
  private String hmac(String token,String value)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}
}
