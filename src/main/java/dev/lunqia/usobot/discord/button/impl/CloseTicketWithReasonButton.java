package dev.lunqia.usobot.discord.button.impl;

import dev.lunqia.usobot.discord.button.Button;
import dev.lunqia.usobot.ticket.TicketService;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloseTicketWithReasonButton implements Button {
  private final TicketService ticketService;

  @Override
  public String id() {
    return "close_ticket_with_reason_button";
  }

  @Override
  public boolean shouldAutomaticallyDefer() {
    return false;
  }

  @Override
  public Mono<Void> handle(ButtonInteractionEvent event) {
    return ticketService
        .openTicketClosureReasonModal(event)
        .onErrorResume(
            exception -> {
              log.error(
                  "Error while opening ticket closure reason modal for {}",
                  event.getUser().getId().asLong(),
                  exception);
              return event
                  .createFollowup(
                      ":x: Failed to open the ticket closure reason modal, please try again later.")
                  .then();
            });
  }
}
