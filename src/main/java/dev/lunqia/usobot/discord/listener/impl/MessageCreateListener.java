package dev.lunqia.usobot.discord.listener.impl;

import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MessageCreateListener implements EventListener<MessageCreateEvent> {
  private static final long TARGET_USER_ID = 647710846595629057L;
  private static final List<String> SIX_VARIANTS = List.of("6", "six");
  private static final List<String> SEVEN_VARIANTS = List.of("7", "seven");

  @Override
  public Class<MessageCreateEvent> getEventType() {
    return MessageCreateEvent.class;
  }

  @Override
  public Mono<Void> execute(MessageCreateEvent event) {
    if (event.getGuildId().isEmpty()) return Mono.empty();

    Message message = event.getMessage();
    if (!isStandardMessage(message)) return Mono.empty();

    String content = message.getContent().toLowerCase(Locale.ROOT);
    if (content.isBlank() || content.startsWith("https://")) return Mono.empty();

    Optional<Member> memberOptional = event.getMember();
    if (memberOptional.isEmpty() || memberOptional.get().getId().asLong() != TARGET_USER_ID)
      return Mono.empty();

    if (containsSixAndSeven(content))
      return message
          .getChannel()
          .flatMap(channel -> channel.createMessage("Bad Abby! Shoo!"))
          .then();

    return Mono.empty();
  }

  private boolean isStandardMessage(Message message) {
    return message.getType() == Message.Type.DEFAULT || message.getType() == Message.Type.REPLY;
  }

  private boolean containsSixAndSeven(String content) {
    boolean hasSix = SIX_VARIANTS.stream().anyMatch(content::contains);
    boolean hasSeven = SEVEN_VARIANTS.stream().anyMatch(content::contains);
    return hasSix && hasSeven;
  }
}
