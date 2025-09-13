package dev.lunqia.usobot.discord.command;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.possible.Possible;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CommandDispatcher {
  private final Map<String, SlashCommand> handlers;

  public CommandDispatcher(List<SlashCommand> handlerList) {
    this.handlers = new HashMap<>();
    for (SlashCommand command : handlerList) this.handlers.put(command.name(), command);
  }

  public Map<String, SlashCommand> handlers() {
    return handlers;
  }

  public Mono<Void> dispatch(ChatInputInteractionEvent event) {
    return Mono.defer(
            () -> {
              String commandName = event.getCommandName();
              SlashCommand command = handlers.get(commandName);
              if (command == null)
                return event
                    .editReply()
                    .withContent(Possible.of(Optional.of("Unknown command, please report this.")))
                    .then();
              log.info(
                  "Dispatching command: '/{}' | requester: {} ({}) | guildId: {}",
                  commandName,
                  event.getInteraction().getUser().getUsername(),
                  event.getInteraction().getUser().getId().asString(),
                  event.getInteraction().getGuildId().map(Snowflake::asString).orElse("DM"));
              return command.handle(event);
            })
        .then();
  }
}
