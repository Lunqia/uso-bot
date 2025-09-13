package dev.lunqia.usobot.discord;

import dev.lunqia.usobot.discord.command.CommandDispatcher;
import dev.lunqia.usobot.discord.command.SlashCommand;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(DiscordBotProperties.class)
@Slf4j
public class DiscordBotConfiguration {
  @Bean
  public ApplicationRunner discordBotRunner(
      DiscordBotProperties properties, CommandDispatcher dispatcher) {
    return args -> {
      if (properties.getToken() == null || properties.getToken().isBlank()) {
        log.warn("Discord bot token is missing. Set discord.bot.token to enable the bot");
        return;
      }

      DiscordClient client = DiscordClient.create(properties.getToken());

      client
          .withGateway(
              gateway -> {
                log.info("Discord gateway session starting. Attaching listeners before READY");

                Mono<Void> interactionsStream =
                    gateway
                        .on(ChatInputInteractionEvent.class)
                        .doOnSubscribe(
                            __ -> log.info("Subscribed to ChatInputInteractionEvent stream"))
                        .flatMap(event -> event.deferReply().then(dispatcher.dispatch(event)))
                        .onErrorResume(
                            exception -> {
                              log.error("Error while processing interaction event", exception);
                              return Mono.empty();
                            })
                        .then();

                Mono<Void> registration =
                    Mono.defer(
                            () -> {
                              log.info("Starting slash command registration");
                              return registerSlashCommands(gateway, dispatcher, properties);
                            })
                        .onErrorResume(
                            exception -> {
                              log.error(
                                  "Error during slash command registration pipeline", exception);
                              return Mono.empty();
                            })
                        .then();

                return Mono.when(interactionsStream, registration);
              })
          .doOnSubscribe(__ -> log.info("Connecting to Discord gateway"))
          .doOnError(exception -> log.error("Discord gateway session error", exception))
          .doOnTerminate(() -> log.info("Discord gateway session terminated"))
          .subscribe();
    };
  }

  private Mono<Void> registerSlashCommands(
      GatewayDiscordClient gateway, CommandDispatcher dispatcher, DiscordBotProperties properties) {
    Map<String, SlashCommand> handlers = dispatcher.handlers();
    List<ApplicationCommandRequest> requests =
        handlers.values().stream()
            .map(
                handler ->
                    ApplicationCommandRequest.builder()
                        .name(handler.name())
                        .description(handler.description())
                        .options(handler.options())
                        .build())
            .<ApplicationCommandRequest>map(req -> req)
            .toList();

    Mono<Long> applicationId = gateway.getRestClient().getApplicationId();

    List<Long> configuredGuildIds = properties.getGuildIds();
    boolean hasGuildIds = configuredGuildIds != null && !configuredGuildIds.isEmpty();

    if (hasGuildIds) {
      log.info(
          "Registering {} slash command(s) for configured guild(s): {}",
          requests.size(),
          configuredGuildIds);

      return applicationId
          .flatMapMany(
              appId ->
                  Flux.fromIterable(configuredGuildIds)
                      .flatMap(
                          guildId ->
                              gateway
                                  .getRestClient()
                                  .getApplicationService()
                                  .bulkOverwriteGuildApplicationCommand(appId, guildId, requests)
                                  .collectList()
                                  .doOnNext(
                                      list ->
                                          log.info(
                                              "Registered {} guild command(s) for guild {}",
                                              list.size(),
                                              guildId))
                                  .onErrorResume(
                                      exception -> {
                                        log.error(
                                            "Failed to register commands for guild {}",
                                            guildId,
                                            exception);
                                        return Mono.empty();
                                      }))
                      .then())
          .then();
    }

    log.warn(
        "No guild IDs configured. Skipping slash command registration to avoid global registration");
    return Mono.empty();
  }
}
