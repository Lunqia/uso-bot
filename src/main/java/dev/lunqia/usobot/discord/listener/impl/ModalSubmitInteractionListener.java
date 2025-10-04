package dev.lunqia.usobot.discord.listener.impl;

import dev.lunqia.usobot.discord.listener.EventListener;
import dev.lunqia.usobot.discord.modal.ModalDispatcher;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.spec.InteractionCallbackSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ModalSubmitInteractionListener implements EventListener<ModalSubmitInteractionEvent> {
  private final ModalDispatcher modalDispatcher;

  @Override
  public Class<ModalSubmitInteractionEvent> getEventType() {
    return ModalSubmitInteractionEvent.class;
  }

  @Override
  public Mono<Void> execute(ModalSubmitInteractionEvent event) {
    return event
        .deferReply(InteractionCallbackSpec.builder().ephemeral(true).build())
        .then(modalDispatcher.dispatch(event))
        .onErrorResume(
            exception -> {
              log.error("Error while processing ModalSubmitInteractionEvent", exception);
              return Mono.error(exception);
            });
  }
}
