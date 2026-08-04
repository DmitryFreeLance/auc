package ru.autoauction.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AppProperties props;
  public WebConfig(AppProperties props) { this.props = props; }
  @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(props.uploadDir()).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
