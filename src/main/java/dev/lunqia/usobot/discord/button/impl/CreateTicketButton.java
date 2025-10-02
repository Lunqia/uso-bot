package dev.lunqia.usobot.discord.button.impl;

import dev.lunqia.usobot.discord.button.Button;
import dev.lunqia.usobot.discord.button.ButtonConstants;
import dev.lunqia.usobot.ticket.Ticket;
import dev.lunqia.usobot.ticket.TicketService;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class CreateTicketButton implements Button {
  private final TicketService ticketService;

  @Override
  public String id() {
    return ButtonConstants.CREATE_TICKET_BUTTON_ID;
  }

  @Override
  public Mono<Void> handle(ButtonInteractionEvent event) {
    return ticketService
        .createTicket(
            Ticket.builder()
                .userId(event.getUser().getId().asLong())
                .guildId(event.getInteraction().getGuildId().orElseThrow().asLong())
                .createdAt(Instant.now())
                .status(Ticket.Status.OPEN))
        .then();
  }
}
