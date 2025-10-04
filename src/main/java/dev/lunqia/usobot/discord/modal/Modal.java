package dev.lunqia.usobot.discord.modal;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import reactor.core.publisher.Mono;

public interface Modal {
  String id();

  Mono<Void> handle(ModalSubmitInteractionEvent event);
}
