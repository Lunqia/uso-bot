package dev.lunqia.usobot.discord;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.discord.command.SlashCommandDispatcher;
import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
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
  public <T extends Event> ApplicationRunner discordBotRunner(
      DiscordBotProperties discordBotProperties,
      SlashCommandDispatcher slashCommandDispatcher,
      List<EventListener<T>> eventListeners) {
    return args -> {
      if (discordBotProperties.getToken() == null || discordBotProperties.getToken().isBlank()) {
        log.warn("Discord bot token is missing, set {DISCORD_BOT_TOKEN} first");
        System.exit(1);
        return;
      }

      GatewayDiscordClient gatewayDiscordClient =
          DiscordClient.create(discordBotProperties.getToken())
              .gateway()
              .setEnabledIntents(
                  IntentSet.of(
                      Intent.GUILDS,
                      Intent.GUILD_MEMBERS,
                      Intent.GUILD_MESSAGES,
                      Intent.MESSAGE_CONTENT))
              .login()
              .block();

      if (gatewayDiscordClient == null) {
        log.error("Failed to connect to Discord");
        return;
      }

      log.info("Registering {} event listeners", eventListeners.size());

      eventListeners.forEach(
          eventListener ->
              gatewayDiscordClient
                  .on(eventListener.getEventType())
                  .flatMap(eventListener::execute)
                  .onErrorResume(eventListener::handleException)
                  .subscribe());

      registerSlashCommands(gatewayDiscordClient, slashCommandDispatcher, discordBotProperties)
          .doOnSubscribe(__ -> log.info("Starting slash command registration"))
          .subscribe();
    };
  }

  private Mono<Void> registerSlashCommands(
      GatewayDiscordClient gatewayDiscordClient,
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

    Mono<Long> applicationId = gatewayDiscordClient.getRestClient().getApplicationId();
    List<Long> configuredGuildIds = discordBotProperties.getGuildIds();
    boolean hasGuildIds = configuredGuildIds != null && !configuredGuildIds.isEmpty();

    if (hasGuildIds) {
      List<Long> botGuildIds =
          gatewayDiscordClient
              .getGuilds()
              .map(guild -> guild.getId().asLong())
              .collectList()
              .block();
      configuredGuildIds.removeIf(
          guildId -> {
            boolean isValidGuild = botGuildIds != null && !botGuildIds.contains(guildId);
            if (!isValidGuild)
              log.warn(
                  "Bot is not a member of guild {}, skipping slash command registration for this guild",
                  guildId);
            return isValidGuild;
          });

      if (configuredGuildIds.isEmpty()) {
        log.warn(
            "No valid guild IDs configured after checking bot's guilds, skipping slash command registration");
        return Mono.empty();
      }

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
                              gatewayDiscordClient
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
