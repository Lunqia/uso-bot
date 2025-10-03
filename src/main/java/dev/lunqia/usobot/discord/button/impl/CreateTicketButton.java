package dev.lunqia.usobot.discord.button.impl;

import dev.lunqia.usobot.discord.button.Button;
import dev.lunqia.usobot.discord.button.ButtonType;
import dev.lunqia.usobot.ticket.Ticket;
import dev.lunqia.usobot.ticket.TicketService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateTicketButton implements Button {
  private final TicketService ticketService;

  @Override
  public String id() {
    return ButtonType.CREATE_TICKET.getId();
  }

  @Override
  public Mono<Void> handle(ButtonInteractionEvent event) {
    long guildId = event.getInteraction().getGuildId().orElseThrow().asLong();
    return ticketService
        .canOpenTicket(event.getUser().getId(), Snowflake.of(guildId))
        .flatMap(
            isTicketAlreadyOpen -> {
              if (isTicketAlreadyOpen)
                return event.createFollowup(":x: You already have an open ticket.").then();
              return ticketService
                  .createTicket(
                      Ticket.builder()
                          .userId(event.getUser().getId().asLong())
                          .guildId(guildId)
                          .createdAt(Instant.now())
                          .status(Ticket.Status.OPEN),
                      event)
                  .flatMap(
                      ticket ->
                          event.createFollowup(
                              ":white_check_mark: Your ticket was successfully created! <#"
                                  + ticket.getChannelId()
                                  + ">"))
                  .onErrorResume(
                      exception -> {
                        log.error(
                            "Error while creating ticket for {}",
                            event.getUser().getId().asLong(),
                            exception);
                        return event.createFollowup(
                            ":x: Failed to create a ticket, please try again later.");
                      })
                  .then();
            });
  }
}
