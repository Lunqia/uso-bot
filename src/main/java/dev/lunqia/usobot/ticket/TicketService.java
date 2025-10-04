package dev.lunqia.usobot.ticket;

import dev.lunqia.usobot.discord.button.ButtonType;
import dev.lunqia.usobot.discord.modal.ModalType;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class TicketService {
  private final TicketRepository ticketRepository;

  public Mono<Ticket> createTicket(
      Ticket.TicketBuilder ticketBuilder, ButtonInteractionEvent event) {
    return ticketRepository
        .count()
        .flatMap(
            ticketCount -> {
              String channelName = "ticket-" + (ticketCount + 1);
              return event
                  .getInteraction()
                  .getGuild()
                  .flatMap(
                      guild ->
                          guild
                              .getChannels()
                              .filter(channel -> channel.getName().equalsIgnoreCase("Tickets"))
                              .next()
                              .flatMap(
                                  parentChannel ->
                                      guild.createTextChannel(
                                          TextChannelCreateSpec.builder()
                                              .name(channelName)
                                              .parentId(parentChannel.getId())
                                              .permissionOverwrites(
                                                  Set.of(
                                                      PermissionOverwrite.forRole(
                                                          guild.getId(),
                                                          PermissionSet.none(),
                                                          PermissionSet.of(
                                                              Permission.VIEW_CHANNEL)),
                                                      PermissionOverwrite.forMember(
                                                          event.getUser().getId(),
                                                          PermissionSet.of(
                                                              Permission.VIEW_CHANNEL,
                                                              Permission.SEND_MESSAGES),
                                                          PermissionSet.none())))
                                              .topic(
                                                  "Support ticket for "
                                                      + event.getUser().getUsername())
                                              .build())))
                  .publishOn(Schedulers.boundedElastic())
                  .flatMap(
                      textChannel -> {
                        Ticket ticket =
                            ticketBuilder.channelId(textChannel.getId().asLong()).build();
                        return ButtonType.CLOSE_TICKET
                            .sendButton(textChannel, event.getUser().getId().asString())
                            .then(ticketRepository.save(ticket))
                            .thenReturn(ticket);
                      });
            });
  }

  public Mono<Boolean> canOpenTicket(Snowflake userId, Snowflake guildId) {
    return ticketRepository.existsByUserIdAndGuildIdAndStatus(
        userId.asLong(), guildId.asLong(), Ticket.Status.OPEN);
  }

  public Mono<Ticket> getTicket(Snowflake userId, Snowflake guildId) {
    return ticketRepository.findByUserIdAndGuildIdAndClosedAtIsNull(
        userId.asLong(), guildId.asLong());
  }

  public Mono<Ticket> closeTicket(
      Ticket ticket,
      TextChannel ticketChannel,
      Snowflake closedByUserId,
      Optional<String> optionalReason) {
    ticket.setStatus(Ticket.Status.CLOSED);
    ticket.setClosedAt(Instant.now());
    ticket.setClosedByUserId(closedByUserId.asLong());
    optionalReason.ifPresent(ticket::setClosureReason);

    return ticketChannel
        .delete(ticket.getClosureReason() != null ? ticket.getClosureReason() : "")
        .then(ticketRepository.save(ticket));
  }

  public Mono<Void> sendTicketLog(Ticket ticket, Guild guild) {
    return guild
        .getChannels()
        .filter(guildChannel -> guildChannel.getName().equalsIgnoreCase("ticket-logs"))
        .filter(TextChannel.class::isInstance)
        .next()
        .map(TextChannel.class::cast)
        .flatMap(
            textChannel ->
                textChannel.createMessage(
                    MessageCreateSpec.builder()
                        .addEmbed(
                            EmbedCreateSpec.builder()
                                .title("Ticket Closed")
                                .color(Color.RED)
                                .addField("User", "<@" + ticket.getUserId() + ">", true)
                                .addField(
                                    "Closed By", "<@" + ticket.getClosedByUserId() + ">", true)
                                .addField(
                                    "Reason",
                                    ticket.getClosureReason() != null
                                        ? ticket.getClosureReason()
                                        : "No reason provided",
                                    false)
                                .addField("Created At", ticket.getCreatedAt().toString(), true)
                                .addField(
                                    "Closed At",
                                    ticket.getClosedAt() != null
                                        ? "<t:"
                                            + ticket.getClosedAt().getEpochSecond()
                                            + ":F> (<t:"
                                            + ticket.getClosedAt().getEpochSecond()
                                            + ":R>)"
                                        : "N/A",
                                    true)
                                .footer(EmbedCreateFields.Footer.of(ticket.getId(), null))
                                .build())
                        .build()))
        .then();
  }

  public Mono<Void> openTicketClosureReasonModal(ButtonInteractionEvent event) {
    return ModalType.CLOSE_TICKET_WITH_REASON.sendModal(event);
  }
}
