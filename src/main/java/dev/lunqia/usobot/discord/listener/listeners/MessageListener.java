package dev.lunqia.usobot.discord.listener.listeners;

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
public class MessageListener implements EventListener<MessageCreateEvent> {
  @Override
  public Class<MessageCreateEvent> getEventType() {
    return MessageCreateEvent.class;
  }

  @Override
  public Mono<Void> execute(MessageCreateEvent event) {
    Message message = event.getMessage();

    if (message.getContent().isBlank()
        && (!message.getEmbeds().isEmpty() || !message.getAttachments().isEmpty()))
      return Mono.empty();

    String content = message.getContent().toLowerCase(Locale.ROOT);

    if (event.getGuildId().isEmpty()) return Mono.empty();

    Optional<Member> memberOptional = event.getMember();
    if (memberOptional.isEmpty()) return Mono.empty();
    if (memberOptional.get().getId().asLong() != 647710846595629057L) return Mono.empty();

    if (content.contains("6") && content.contains("7")
        || content.contains("six") && content.contains("7")
        || content.contains("6") && content.contains("seven")
        || content.contains("67")
        || (content.contains("six") && content.contains("seven")))
      return message
          .getChannel()
          .flatMap(channel -> channel.createMessage("Bad Abby! Shoo!"))
          .then();

    return Mono.empty();
  }
}
