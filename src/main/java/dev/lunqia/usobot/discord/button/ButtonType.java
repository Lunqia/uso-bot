package dev.lunqia.usobot.discord.button;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Getter
public enum ButtonType {
  CREATE_TICKET(
      "Contact the staff",
      MessageCreateSpec.builder()
          .addEmbed(
              EmbedCreateSpec.builder()
                  .title("Need help? Want to report an issue?")
                  .description("Click the button below to create a support ticket.")
                  .color(Color.CYAN)
                  .build())
          .addComponent(
              ActionRow.of(
                  Button.primary(
                      "create_ticket_button", ReactionEmoji.unicode("❤️"), "Create a ticket")))
          .build());

  private final String friendlyName;
  private final MessageCreateSpec messageCreateSpec;

  public static Optional<ButtonType> fromId(String buttonId) {
    for (ButtonType type : values()) {
      if (type.getId().equals(buttonId)) return Optional.of(type);
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return name().toLowerCase() + "_button";
  }

  public String getId() {
    return toString();
  }

  public Mono<Message> sendButton(TextChannel textChannel) {
    return textChannel.createMessage(messageCreateSpec);
  }
}
