package dev.lunqia.usobot.discord;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.discord.command.SlashCommandDispatcher;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ConnectEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import java.util.List;
import java.util.Locale;
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
          .gateway()
          .setEnabledIntents(
              IntentSet.of(
                  Intent.GUILDS,
                  Intent.GUILD_MEMBERS,
                  Intent.GUILD_MESSAGES,
                  Intent.MESSAGE_CONTENT))
          .withGateway(
              gateway -> {
                log.info("Discord gateway session starting, attaching listeners");

                Mono<Void> setCustomPresence =
                    gateway
                        .on(ConnectEvent.class)
                        .flatMap(
                            event ->
                                event
                                    .getClient()
                                    .updatePresence(
                                        ClientPresence.online(
                                            ClientActivity.custom("Pocketing you")))
                                    .doOnSubscribe(__ -> log.info("Setting custom bot presence"))
                                    .then())
                        .then();

                Mono<Void> slashCommandInteractionListener =
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

                Mono<Void> slashCommandRegistration =
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

                Mono<Void> autoRoleOnJoin =
                    gateway
                        .on(MemberJoinEvent.class)
                        .doOnSubscribe(__ -> log.info("Subscribed to MemberJoinEvent stream"))
                        .flatMap(
                            event -> {
                              long guildId = event.getGuildId().asLong();
                              List<Long> autoRoleIds = discordBotProperties.getAutoRoleIds();
                              boolean hasAutoRoles = autoRoleIds != null && autoRoleIds.size() == 2;
                              if (!hasAutoRoles) return Mono.empty();
                              Long autoRoleId =
                                  guildId == discordBotProperties.getGuildIds().getFirst()
                                      ? autoRoleIds.getFirst()
                                      : autoRoleIds.getLast();

                              log.info(
                                  "Assigning auto-role {} to new member {} ({}) in guild {}",
                                  autoRoleId,
                                  event.getMember().getUsername(),
                                  event.getMember().getId().asLong(),
                                  guildId);
                              return event
                                  .getMember()
                                  .addRole(Snowflake.of(autoRoleId), "Auto-role on join")
                                  .doOnError(
                                      exception -> log.error("Failed to assign role", exception))
                                  .onErrorResume(exception -> Mono.empty());
                            })
                        .then();

                Mono<Void> messageListener =
                    gateway
                        .on(MessageCreateEvent.class)
                        .doOnSubscribe(__ -> log.info("Subscribed to MessageCreateEvent stream"))
                        .flatMap(
                            event -> {
                              String content =
                                  event.getMessage().getContent().toLowerCase(Locale.ROOT);
                              if (event.getGuildId().isEmpty()) return Mono.empty();
                              if (event.getMember().isEmpty()) return Mono.empty();
                              if (event.getMember().get().getId().asLong() != 647710846595629057L)
                                return Mono.empty();

                              if (content.contains("6") && content.contains("7")
                                  || content.contains("six") && content.contains("7")
                                  || content.contains("6") && content.contains("seven")
                                  || content.contains("67")
                                  || (content.contains("six") && content.contains("seven"))) {
                                return event
                                    .getMessage()
                                    .getChannel()
                                    .flatMap(channel -> channel.createMessage("Bad Abby! Shoo!"))
                                    .then();
                              }
                              return Mono.empty();
                            })
                        .then();

                return Mono.when(
                    setCustomPresence,
                    slashCommandInteractionListener,
                    slashCommandRegistration,
                    autoRoleOnJoin,
                    messageListener);
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
      List<Long> botGuildIds =
          gateway.getGuilds().map(guild -> guild.getId().asLong()).collectList().block();
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
