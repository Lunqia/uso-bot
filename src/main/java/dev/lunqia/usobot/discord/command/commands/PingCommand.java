package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.discord.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PingCommand implements SlashCommand {
  @Override
  public String name() {
    return "ping";
  }

  @Override
  public String description() {
    return "Check the bot's latency.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return Collections.emptyList();
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    Instant start = Instant.now();
    return Mono.defer(
            () ->
                event
                    .editReply()
                    .withContent(Possible.of(Optional.of("Processing...")))
                    .then(Mono.fromRunnable(() -> {})))
        .then(
            Mono.defer(
                () -> {
                  Duration latency = Duration.between(start, Instant.now());
                  String content = "Pong! Latency: " + latency.toMillis() + " ms";
                  return event.editReply().withContent(Possible.of(Optional.of(content)));
                }))
        .then();
  }
}
