package dev.lunqia.usobot.discord.listener.listeners;

import dev.lunqia.usobot.discord.DiscordBotProperties;
import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberJoinListener implements EventListener<MemberJoinEvent> {
  private final DiscordBotProperties discordBotProperties;

  @Override
  public Class<MemberJoinEvent> getEventType() {
    return MemberJoinEvent.class;
  }

  @Override
  public Mono<Void> execute(MemberJoinEvent event) {
    List<Long> autoRoleIds = discordBotProperties.getAutoRoleIds();
    boolean hasAutoRoles = autoRoleIds != null && autoRoleIds.size() == 2;
    if (!hasAutoRoles) return Mono.empty();

    long guildId = event.getGuildId().asLong();
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
        .onErrorResume(
            exception -> {
              log.error("Failed to assign role", exception);
              return Mono.empty();
            });
  }
}
