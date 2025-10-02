package dev.lunqia.usobot.discord.listener.impl;

import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MessageCreateListener implements EventListener<MessageCreateEvent> {
  @Override
  public Class<MessageCreateEvent> getEventType() {
    return MessageCreateEvent.class;
  }

  @Override
  public Mono<Void> execute(MessageCreateEvent event) {
    Message message = event.getMessage();

    if (message.getType() != Message.Type.DEFAULT && message.getType() != Message.Type.REPLY)
      return Mono.empty();

    String messageContent = message.getContent().toLowerCase(Locale.ROOT);

    if (event.getGuildId().isEmpty()) return Mono.empty();

    Optional<Member> memberOptional = event.getMember();
    if (memberOptional.isEmpty()) return Mono.empty();
    if (memberOptional.get().getId().asLong() != 647710846595629057L) return Mono.empty();

    if ((messageContent.contains("6") && messageContent.contains("7"))
        || (messageContent.contains("six") && messageContent.contains("7"))
        || (messageContent.contains("6") && messageContent.contains("seven"))
        || messageContent.contains("67")
        || (messageContent.contains("six") && messageContent.contains("seven"))) {
      return message
          .getChannel()
          .flatMap(channel -> channel.createMessage("Bad Abby! Shoo!"))
          .then();
    }

    return Mono.empty();
  }
}
