package dev.lunqia.usobot.discord.modal;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.discordjson.possible.Possible;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Getter
@Slf4j
public class ModalDispatcher {
  private final Map<String, Modal> modalMap;

  public ModalDispatcher(List<Modal> modals) {
    modalMap = new HashMap<>();
    for (Modal modal : modals) modalMap.put(modal.id(), modal);
  }

  public Mono<Void> dispatch(ModalSubmitInteractionEvent event) {
    return Mono.defer(
        () -> {
          String modalId = event.getCustomId();
          Modal modal = modalMap.get(modalId);
          if (modal == null)
            return event
                .editReply()
                .withContent(Possible.of(Optional.of("Unknown modal, please report this.")))
                .then();

          log.info(
              "Dispatching modal: '{}' | requester: {} ({}) | guildId: {}",
              modalId,
              event.getInteraction().getUser().getUsername(),
              event.getInteraction().getUser().getId().asString(),
              event.getInteraction().getGuildId().map(Snowflake::asString).orElse("N/A"));

          return modal.handle(event);
        });
  }
}
