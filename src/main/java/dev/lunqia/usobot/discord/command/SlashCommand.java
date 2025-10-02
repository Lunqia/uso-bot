package dev.lunqia.usobot.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import java.util.List;
import reactor.core.publisher.Mono;

public interface SlashCommand {
  String name();

  String description();

  List<ApplicationCommandOptionData> options();

  Mono<Void> handle(ChatInputInteractionEvent event);

  default String defaultMemberPermissions() {
    return "0";
  }

  default boolean dmPermission() {
    return false;
  }
}
