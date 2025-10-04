package dev.lunqia.usobot.discord.button.impl;

import dev.lunqia.usobot.discord.button.Button;
import dev.lunqia.usobot.discord.button.ButtonType;
import dev.lunqia.usobot.ticket.TicketService;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloseTicketButton implements Button {
  private final TicketService ticketService;

  @Override
  public String id() {
    return ButtonType.CLOSE_TICKET.getId();
  }

  @Override
  public Mono<Void> handle(ButtonInteractionEvent event) {
    Snowflake guildId = event.getInteraction().getGuildId().orElseThrow();
    return ticketService
        .getTicket(event.getUser().getId(), guildId)
        .flatMap(
            ticket -> {
              if (ticket.getClosedAt() != null)
                return event.createFollowup(":x: This ticket is already closed.").then();
              return Mono.zip(
                      event.getInteraction().getChannel().ofType(TextChannel.class),
                      event.getInteraction().getGuild())
                  .flatMap(
                      tuple -> {
                        TextChannel textChannel = tuple.getT1();
                        Guild guild = tuple.getT2();
                        return ticketService
                            .closeTicket(
                                ticket,
                                textChannel,
                                event.getUser().getId(),
                                ticket.getUserId() != event.getUser().getId().asLong()
                                    ? Optional.empty()
                                    : Optional.of("Self-closed"))
                            .flatMap(
                                closedTicket -> ticketService.sendTicketLog(closedTicket, guild))
                            .then();
                      })
                  .onErrorResume(
                      exception -> {
                        log.error(
                            "Error while closing ticket for {}",
                            event.getUser().getId().asLong(),
                            exception);
                        return event
                            .createFollowup(
                                ":x: Failed to close the ticket, please try again later.")
                            .then();
                      });
            })
        .then();
  }
}
