package dev.lunqia.usobot.discord.listener.listeners;

import dev.lunqia.usobot.discord.command.SlashCommandDispatcher;
import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlashCommandListener implements EventListener<ChatInputInteractionEvent> {
  private final SlashCommandDispatcher slashCommandDispatcher;

  @Override
  public Class<ChatInputInteractionEvent> getEventType() {
    return ChatInputInteractionEvent.class;
  }

  @Override
  public Mono<Void> execute(ChatInputInteractionEvent event) {
    return event
        .deferReply()
        .then(slashCommandDispatcher.dispatch(event))
        .onErrorResume(
            exception -> {
              log.error("Error while processing interaction event", exception);
              return Mono.empty();
            });
  }
}
