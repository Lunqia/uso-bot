package dev.lunqia.usobot.discord.command;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.discordjson.possible.Possible;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Getter
@Slf4j
public class SlashCommandDispatcher {
  private final Map<String, SlashCommand> slashCommandMap;

  public SlashCommandDispatcher(List<SlashCommand> slashCommands) {
    slashCommandMap = new HashMap<>();
    for (SlashCommand slashCommand : slashCommands)
      slashCommandMap.put(slashCommand.name(), slashCommand);
  }

  public Mono<Void> dispatch(ChatInputInteractionEvent event) {
    return Mono.defer(
        () -> {
          String commandName = event.getCommandName();
          SlashCommand slashCommand = slashCommandMap.get(commandName);
          if (slashCommand == null) {
            return event
                .editReply()
                .withContent(Possible.of(Optional.of("Unknown slash command, please report this.")))
                .then();
          }

          String optionsLog =
              event.getOptions().stream()
                  .map(
                      option -> {
                        String optionValue =
                            option
                                .getValue()
                                .map(ApplicationCommandInteractionOptionValue::asString)
                                .orElseGet(
                                    () ->
                                        option.getOptions().stream()
                                            .map(
                                                subOption ->
                                                    subOption.getName()
                                                        + "="
                                                        + subOption
                                                            .getValue()
                                                            .map(
                                                                ApplicationCommandInteractionOptionValue
                                                                    ::asString)
                                                            .orElse("no-value"))
                                            .toList()
                                            .toString());
                        return option.getName() + "=" + optionValue;
                      })
                  .toList()
                  .toString();

          log.info(
              "Dispatching slash command: '{}{}' | requester: {} ({}) | guildId: {}",
              commandName,
              optionsLog,
              event.getInteraction().getUser().getUsername(),
              event.getInteraction().getUser().getId().asString(),
              event.getInteraction().getGuildId().map(Snowflake::asString).orElse("DM"));

          return slashCommand.handle(event);
        });
  }
}
