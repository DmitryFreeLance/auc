package ru.autoauction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AppProperties props;
  public WebConfig(AppProperties props) { this.props = props; }
  @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(props.uploadDir()).toAbsolutePath().normalize().toUri().toString();
    CacheControl immutable=CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();
    registry.addResourceHandler("/uploads/**").addResourceLocations(location).setCacheControl(immutable);
    registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/").setCacheControl(immutable);
    registry.addResourceHandler("/styles.css","/app.js").addResourceLocations("classpath:/static/").setCacheControl(immutable);
  }
}
