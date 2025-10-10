package dev.lunqia.usobot.discord.modal.impl;

import dev.lunqia.usobot.discord.modal.Modal;
import dev.lunqia.usobot.discord.modal.ModalType;
import dev.lunqia.usobot.ticket.TicketService;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
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
public class CloseTicketWithReasonModal implements Modal {
  private final TicketService ticketService;

  @Override
  public String id() {
    return ModalType.CLOSE_TICKET_WITH_REASON.getId();
  }

  @Override
  public Mono<Void> handle(ModalSubmitInteractionEvent event) {
    return ticketService
        .getTicketByChannelId(event.getInteraction().getChannelId())
        .flatMap(
            ticket -> {
              if (ticket.getClosedAt() != null)
                return event.createFollowup(":x: This ticket is already closed.").then();

              String reason =
                  event
                      .getComponents(TextInput.class)
                      .getFirst()
                      .getValue()
                      .orElse("No reason provided");

              return Mono.zip(
                      event.getInteraction().getChannel().ofType(TextChannel.class),
                      event.getInteraction().getGuild())
                  .flatMap(
                      tuple -> {
                        TextChannel textChannel = tuple.getT1();
                        Guild guild = tuple.getT2();

                        return ticketService
                            .closeTicket(
                                ticket, textChannel, event.getUser().getId(), Optional.of(reason))
                            .flatMap(
                                closedTicket -> ticketService.sendTicketLog(closedTicket, guild))
                            .then();
                      })
                  .onErrorResume(
                      exception -> {
                        log.error(
                            "Error while closing ticket with reason for {}",
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
