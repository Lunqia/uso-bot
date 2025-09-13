package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.catapi.CatApiService;
import dev.lunqia.usobot.discord.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CatCommand implements SlashCommand {
  private final CatApiService catApiService;

  public CatCommand(CatApiService catApiService) {
    this.catApiService = catApiService;
  }

  @Override
  public String name() {
    return "cat";
  }

  @Override
  public String description() {
    return "Send a random cat image.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return Collections.emptyList();
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return catApiService
        .getRandomCatImageUrl()
        .flatMap(url -> event.editReply().withContent(Possible.of(Optional.of(url))))
        .switchIfEmpty(
            event
                .editReply()
                .withContent(Possible.of(Optional.of("No cat image found. Please try again.")))
                .then(Mono.empty()))
        .onErrorResume(
            exception ->
                event
                    .editReply()
                    .withContent(
                        Possible.of(Optional.of("Failed to fetch a cat image. Please try again.")))
                    .then(Mono.empty()))
        .then();
  }
}
