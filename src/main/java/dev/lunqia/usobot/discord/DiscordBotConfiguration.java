package dev.lunqia.usobot.discord;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.discord.command.SlashCommandDispatcher;
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
      DiscordBotProperties discordBotProperties, SlashCommandDispatcher slashCommandDispatcher) {
    return args -> {
      if (discordBotProperties.getToken() == null || discordBotProperties.getToken().isBlank()) {
        log.warn("Discord bot token is missing, set {DISCORD_BOT_TOKEN} first");
        System.exit(1);
        return;
      }

      DiscordClient client = DiscordClient.create(discordBotProperties.getToken());

      client
          .withGateway(
              gateway -> {
                log.info("Discord gateway session starting, attaching listeners");

                Mono<Void> interactionsStream =
                    gateway
                        .on(ChatInputInteractionEvent.class)
                        .doOnSubscribe(
                            __ -> log.info("Subscribed to ChatInputInteractionEvent stream"))
                        .flatMap(
                            event ->
                                event.deferReply().then(slashCommandDispatcher.dispatch(event)))
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
                              return registerSlashCommands(
                                  gateway, slashCommandDispatcher, discordBotProperties);
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
      GatewayDiscordClient gateway,
      SlashCommandDispatcher slashCommandDispatcher,
      DiscordBotProperties discordBotProperties) {
    Map<String, SlashCommand> slashCommandMap = slashCommandDispatcher.getSlashCommandMap();
    List<ApplicationCommandRequest> applicationCommandRequests =
        slashCommandMap.values().stream()
            .map(
                slashCommand ->
                    ApplicationCommandRequest.builder()
                        .name(slashCommand.name())
                        .description(slashCommand.description())
                        .options(slashCommand.options())
                        .build())
            .<ApplicationCommandRequest>map(
                immutableApplicationCommandRequest -> immutableApplicationCommandRequest)
            .toList();

    Mono<Long> applicationId = gateway.getRestClient().getApplicationId();
    List<Long> configuredGuildIds = discordBotProperties.getGuildIds();
    boolean hasGuildIds = configuredGuildIds != null && !configuredGuildIds.isEmpty();

    if (hasGuildIds) {
      log.info(
          "Registering {} slash command(s) for configured guild(s): {}",
          applicationCommandRequests.size(),
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
                                  .bulkOverwriteGuildApplicationCommand(
                                      appId, guildId, applicationCommandRequests)
                                  .collectList()
                                  .doOnNext(
                                      applicationCommandDataList ->
                                          log.info(
                                              "Registered {} guild command(s) for guild {}",
                                              applicationCommandDataList.size(),
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

    log.warn("No guild IDs configured, skipping slash command registration");
    return Mono.empty();
  }
}
