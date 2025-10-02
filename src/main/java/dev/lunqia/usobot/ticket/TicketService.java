package dev.lunqia.usobot.ticket;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.spec.TextChannelCreateSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
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
                      textChannel ->
                          ticketRepository.save(
                              ticketBuilder.channelId(textChannel.getId().asLong()).build()));
            });
  }
}
