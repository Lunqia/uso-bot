package dev.lunqia.usobot.discord.button;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.discordjson.possible.Possible;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Getter
@Slf4j
public class ButtonDispatcher {
  private final Map<String, Button> buttonMap;

  public ButtonDispatcher(List<Button> buttons) {
    buttonMap = new HashMap<>();
    for (Button button : buttons) buttonMap.put(button.id(), button);
  }

  public Mono<Void> dispatch(ButtonInteractionEvent event) {
    return Mono.defer(
        () -> {
          String buttonId = event.getCustomId();
          Button button = buttonMap.get(buttonId);
          if (button == null)
            return event
                .editReply()
                .withContent(Possible.of(Optional.of("Unknown button, please report this.")))
                .then();

          log.info(
              "Dispatching button: '{}' | requester: {} ({}) | guildId: {}",
              buttonId,
              event.getInteraction().getUser().getUsername(),
              event.getInteraction().getUser().getId().asString(),
              event.getInteraction().getGuildId().map(Snowflake::asString).orElse("DM"));

          return button.handle(event);
        });
  }
}
