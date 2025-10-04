package dev.lunqia.usobot.discord.modal.impl;

import dev.lunqia.usobot.discord.modal.Modal;
import dev.lunqia.usobot.discord.modal.ModalType;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloseTicketWithReasonModal implements Modal {
  @Override
  public String id() {
    return ModalType.CLOSE_TICKET_WITH_REASON.getId();
  }

  @Override
  public Mono<Void> handle(ModalSubmitInteractionEvent event) {
    return event.createFollowup(":x: This feature is not implemented yet.").then();
  }
}
