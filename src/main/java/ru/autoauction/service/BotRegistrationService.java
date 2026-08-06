package ru.autoauction.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.autoauction.config.AppProperties;
import ru.autoauction.model.AppUser;
import ru.autoauction.model.Role;
import ru.autoauction.repo.UserRepository;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BotRegistrationService {
  private static final Pattern PHONE=Pattern.compile("(?mi)^TEL(?:;[^:]*)?:(\\+?[0-9][0-9 ()-]{8,20})\\r?$");
  private final AppProperties props;private final UserRepository users;
  public BotRegistrationService(AppProperties props,UserRepository users){this.props=props;this.users=users;}

  @Transactional public AppUser observe(long maxUserId,String maxName){
    AppUser user=users.findByMaxUserId(maxUserId).orElseGet(()->new AppUser(maxUserId,cleanName(maxName),configuredRole(maxUserId)));
    if(maxName!=null&&!maxName.isBlank())user.name=maxName.trim();user.lastSeenAt=Instant.now();synchronizeConfiguredRole(user);return users.save(user);
  }

  @Transactional public AppUser registerVerifiedContact(long maxUserId,String maxName,JsonNode payload){
    String vcf=payload.path("vcf_info").asText("").replace("\\r\\n","\r\n"),supplied=payload.path("hash").asText("");
    if(vcf.isBlank()||supplied.isBlank()||!validHash(vcf,supplied))throw new IllegalArgumentException("Контакт не подтверждён MAX");
    Matcher matcher=PHONE.matcher(vcf);if(!matcher.find())throw new IllegalArgumentException("В контакте отсутствует номер телефона");
    String digits=matcher.group(1).replaceAll("\\D","");if(digits.length()<10||digits.length()>15)throw new IllegalArgumentException("Некорректный номер телефона");
    AppUser user=users.findByMaxUserId(maxUserId).orElseGet(()->new AppUser(maxUserId,cleanName(maxName),configuredRole(maxUserId)));
    if(maxName!=null&&!maxName.isBlank())user.name=maxName.trim();user.phone="+"+digits;user.phoneVerified=true;user.registered=true;user.lastSeenAt=Instant.now();synchronizeConfiguredRole(user);return users.save(user);
  }

  private boolean validHash(String vcf,String supplied){
    try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(props.botToken().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));String expected=HexFormat.of().formatHex(mac.doFinal(vcf.getBytes(StandardCharsets.UTF_8)));return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),supplied.toLowerCase().getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException("Не удалось проверить контакт MAX",e);}
  }
  private String cleanName(String value){return value==null||value.isBlank()?"Пользователь MAX":value.trim();}
  private Role configuredRole(long maxUserId){return props.superAdminMaxIds().contains(maxUserId)?Role.SUPER_ADMIN:props.adminMaxIds().contains(maxUserId)?Role.ADMIN:Role.USER;}
  private void synchronizeConfiguredRole(AppUser user){if(props.superAdminMaxIds().contains(user.maxUserId))user.role=Role.SUPER_ADMIN;else if(props.adminMaxIds().contains(user.maxUserId)&&user.role==Role.USER)user.role=Role.ADMIN;}
}
