package dev.lunqia.usobot.discord.button;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface Button {
  String id();

  Mono<Void> handle(ButtonInteractionEvent event);

  default boolean shouldAutomaticallyDefer() {
    return true;
  }
}
