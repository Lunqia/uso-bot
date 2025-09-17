package dev.lunqia.usobot.discord.command.commands.player;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.discord.command.SlashSubCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerCommand implements SlashCommand {
  private final List<SlashSubCommand> slashSubCommands;
  private Map<String, SlashSubCommand> slashSubCommandMap;

  @PostConstruct
  private void init() {
    slashSubCommandMap =
        slashSubCommands.stream()
            .collect(Collectors.toMap(SlashSubCommand::name, slashSubCommand -> slashSubCommand));
  }

  @Override
  public String name() {
    return "player";
  }

  @Override
  public String description() {
    return "Overwatch 2 player commands.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return slashSubCommands.stream()
        .map(
            slashSubCommand ->
                ApplicationCommandOptionData.builder()
                    .name(slashSubCommand.name())
                    .description(slashSubCommand.name())
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .options(slashSubCommand.options())
                    .build())
        .map(ApplicationCommandOptionData.class::cast)
        .toList();
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return Mono.defer(
        () -> {
          Optional<ApplicationCommandInteractionOption> subOption =
              event.getOptions().stream()
                  .filter(option -> slashSubCommandMap.containsKey(option.getName()))
                  .findFirst();

          if (subOption.isEmpty())
            return event
                .editReply()
                .withContent(Possible.of(Optional.of("Unknown player subcommand.")))
                .then();

          SlashSubCommand slashSubCommand = slashSubCommandMap.get(subOption.get().getName());
          return slashSubCommand.handle(event);
        });
  }
}
