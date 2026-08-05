package ru.autoauction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.Set;

@ConfigurationProperties("auction")
public record AppProperties(
    String botToken, String botUsername, String publicUrl,
    Set<Long> adminMaxIds, Set<Long> superAdminMaxIds, boolean demoAuth,
    Duration maxAuthAge, String uploadDir, String adminAccessPassword) {
  public AppProperties {
    adminMaxIds = adminMaxIds == null ? Set.of() : adminMaxIds;
    superAdminMaxIds = superAdminMaxIds == null ? Set.of() : superAdminMaxIds;
    maxAuthAge = maxAuthAge == null ? Duration.ofHours(24) : maxAuthAge;
    uploadDir = uploadDir == null ? "./data/uploads" : uploadDir;
    adminAccessPassword = adminAccessPassword == null ? "" : adminAccessPassword;
  }
}
