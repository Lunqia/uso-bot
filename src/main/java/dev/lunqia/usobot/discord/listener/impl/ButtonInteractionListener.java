package dev.lunqia.usobot.discord.listener.impl;

import dev.lunqia.usobot.discord.button.ButtonDispatcher;
import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.InteractionCallbackSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ButtonInteractionListener implements EventListener<ButtonInteractionEvent> {
  private final ButtonDispatcher buttonDispatcher;

  @Override
  public Class<ButtonInteractionEvent> getEventType() {
    return ButtonInteractionEvent.class;
  }

  @Override
  public Mono<Void> execute(ButtonInteractionEvent event) {
    return event
        .deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
        .then(buttonDispatcher.dispatch(event))
        .onErrorResume(
            exception -> {
              log.error("Error while processing ButtonInteractionEvent", exception);
              return Mono.error(exception);
            });
  }
}
