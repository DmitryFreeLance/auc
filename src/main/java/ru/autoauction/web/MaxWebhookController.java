package ru.autoauction.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.autoauction.config.AppProperties;
import ru.autoauction.service.MaxBotService;

@RestController @RequestMapping("/api/max")
public class MaxWebhookController {
  private final AppProperties props;private final MaxBotService bot;
  public MaxWebhookController(AppProperties props,MaxBotService bot){this.props=props;this.bot=bot;}
  @PostMapping("/webhook") public void webhook(@RequestHeader(value="X-Max-Bot-Api-Secret",required=false) String secret,@RequestBody JsonNode payload){
    if(props.webhookSecret()!=null&&!props.webhookSecret().isBlank()&&!props.webhookSecret().equals(secret))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    JsonNode updates=payload.has("updates")?payload.path("updates"):payload;
    if(updates.isArray())updates.forEach(this::handle);else handle(updates);
  }
  private void handle(JsonNode u){String type=u.path("update_type").asText(u.path("type").asText());String text=u.path("message").path("body").path("text").asText();if(type.equals("bot_started")||text.equals("/start")||text.equalsIgnoreCase("начать")){long id=bot.extractUserId(u);if(id>0)bot.sendOpen(id);}}
}
