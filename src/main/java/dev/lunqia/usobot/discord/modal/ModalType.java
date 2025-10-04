package dev.lunqia.usobot.discord.modal;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Getter
public enum ModalType {
  CLOSE_TICKET_WITH_REASON(
      "Close ticket with reason",
      InteractionPresentModalSpec.builder()
          .customId("close_ticket_with_reason_modal")
          .title("Close Ticket")
          .addComponent(
              ActionRow.of(
                  TextInput.paragraph(
                      "close_ticket_reason_text_input",
                      "Enter the reason for closing the ticket",
                      1,
                      2000)))
          .build());

  private final String friendlyName;
  private final InteractionPresentModalSpec interactionPresentModalSpec;

  public static Optional<ModalType> fromId(String modalId) {
    for (ModalType type : values()) {
      if (type.getId().equalsIgnoreCase(modalId)) return Optional.of(type);
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return name().toLowerCase() + "_modal";
  }

  public String getId() {
    return toString();
  }

  public Mono<Void> sendModal(ButtonInteractionEvent event) {
    return event.presentModal(interactionPresentModalSpec);
  }
}
