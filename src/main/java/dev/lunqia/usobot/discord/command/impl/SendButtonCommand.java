package dev.lunqia.usobot.discord.command.impl;

import dev.lunqia.usobot.discord.button.ButtonType;
import dev.lunqia.usobot.discord.command.SlashCommand;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionChoiceData;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendButtonCommand implements SlashCommand {
  private static final String OPTION_BUTTON_ID = "button_id";

  @Override
  public String name() {
    return "send_button";
  }

  @Override
  public String description() {
    return "Sends a button with the specified ID.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
        ApplicationCommandOptionData.builder()
            .name(OPTION_BUTTON_ID)
            .description("The ID of the button to send.")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .choices(
                Arrays.stream(ButtonType.values())
                    .map(
                        buttonType ->
                            ImmutableApplicationCommandOptionChoiceData.builder()
                                .name(buttonType.getFriendlyName())
                                .value(buttonType.getId())
                                .build())
                    .map(ApplicationCommandOptionChoiceData.class::cast)
                    .toList())
            .build());
  }

  @Override
  public String defaultMemberPermissions() {
    return "16";
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    String buttonId =
        event
            .getOption(OPTION_BUTTON_ID)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .orElse(null);

    if (buttonId == null) return event.createFollowup("Button ID is required.").then();

    Optional<ButtonType> buttonTypeOptional = ButtonType.fromId(buttonId);
    return buttonTypeOptional
        .map(
            buttonType ->
                event
                    .getInteraction()
                    .getChannel()
                    .flatMap(
                        messageChannel -> {
                          if (messageChannel instanceof TextChannel textChannel) {
                            log.info(
                                "Sending button: '{}' | requester: {} ({}) | guildId: {}",
                                buttonId,
                                event.getInteraction().getUser().getUsername(),
                                event.getInteraction().getUser().getId().asString(),
                                event
                                    .getInteraction()
                                    .getGuildId()
                                    .map(Snowflake::asString)
                                    .orElse("N/A"));

                            return buttonType.sendButton(textChannel);
                          }
                          return Mono.empty();
                        })
                    .then(event.deleteReply()))
        .orElseGet(
            () -> event.createFollowup("Invalid button ID. Please choose a valid button.").then());
  }
}
