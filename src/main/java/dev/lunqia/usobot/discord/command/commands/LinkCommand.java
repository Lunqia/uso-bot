package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.overwatch2.link.UserLink;
import dev.lunqia.usobot.overwatch2.link.UserLinkRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkCommand implements SlashCommand {
  private static final String OPTION_BATTLE_TAG = "battle_tag";

  private final UserLinkRepository userLinkRepository;

  @Override
  public String name() {
    return "link";
  }

  @Override
  public String description() {
    return "Link your BattleTag to your Discord account.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
        ApplicationCommandOptionData.builder()
            .name(OPTION_BATTLE_TAG)
            .description("Case-sensitive BattleTag, e.g. Lunqia#2834")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build());
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    String discordId = event.getInteraction().getUser().getId().asString();

    String battleTag =
        event
            .getOption(OPTION_BATTLE_TAG)
            .flatMap(
                option -> option.getValue().map(ApplicationCommandInteractionOptionValue::asString))
            .orElse(null);

    if (battleTag == null || battleTag.isEmpty()) {
      return event
          .editReply()
          .withContent(
              Possible.of(
                  Optional.of("Please provide a BattleTag, e.g. `/link battle_tag:Lunqia#2834`.")))
          .then();
    }

    String playerId = battleTag.trim().replace("#", "-");

    return userLinkRepository
        .findById(discordId)
        .flatMap(
            existingLink -> {
              if (existingLink.getBattleTag().equals(playerId)) {
                return event
                    .editReply()
                    .withContent(
                        Possible.of(
                            Optional.of("Your BattleTag `" + battleTag + "` is already linked.")));
              }
              existingLink.setBattleTag(playerId);
              return userLinkRepository
                  .save(existingLink)
                  .then(
                      event
                          .editReply()
                          .withContent(
                              Possible.of(
                                  Optional.of(
                                      "Successfully updated linked BattleTag to `"
                                          + battleTag
                                          + "`."))));
            })
        .switchIfEmpty(
            userLinkRepository
                .save(UserLink.builder().discordId(discordId).battleTag(playerId).build())
                .then(
                    event
                        .editReply()
                        .withContent(
                            Possible.of(
                                Optional.of(
                                    "Successfully linked BattleTag: `" + battleTag + "`.")))))
        .onErrorResume(
            exception -> {
              log.error("Error linking BattleTag for {}", discordId, exception);
              return event
                  .editReply()
                  .withContent(
                      Possible.of(Optional.of("Failed to link BattleTag. Try again later.")));
            })
        .then();
  }
}
