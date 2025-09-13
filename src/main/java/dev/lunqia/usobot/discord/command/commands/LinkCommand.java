package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.overwatch.model.PlatformType;
import dev.lunqia.usobot.overwatch.model.UserLink;
import dev.lunqia.usobot.overwatch.repository.UserLinkRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
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
  private static final String OPTION_PLATFORM = "platform";

  private final UserLinkRepository userLinkRepository;

  @Override
  public String name() {
    return "link";
  }

  @Override
  public String description() {
    return "Link your BattleTag and platform to your Discord account.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
        ApplicationCommandOptionData.builder()
            .name(OPTION_BATTLE_TAG)
            .description("BattleTag, e.g. Player#1234")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build(),
        ApplicationCommandOptionData.builder()
            .name(OPTION_PLATFORM)
            .description(
                "Platform: "
                    + PlatformType.PC.getApiName()
                    + ", "
                    + PlatformType.PSN.getApiName()
                    + ", or "
                    + PlatformType.XBL.getApiName())
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
            .flatMap(option -> option.getValue().map(v -> v.asString().trim()))
            .orElse(null);

    Optional<Optional<PlatformType>> platformTypeOptional =
        event
            .getOption(OPTION_PLATFORM)
            .flatMap(option -> option.getValue().map(v -> v.asString().trim().toLowerCase()))
            .map(PlatformType::fromString);

    if (battleTag == null || battleTag.isEmpty()) {
      return event
          .editReply()
          .withContent(
              Possible.of(
                  Optional.of(
                      "Please provide both a BattleTag and a platform, e.g. `/link battle_tag:Lunqia#2834 platform:pc`.")))
          .then();
    }

    if (platformTypeOptional.isEmpty()) {
      return event
          .editReply()
          .withContent(
              Possible.of(
                  Optional.of(
                      "Invalid platform. Please use "
                          + PlatformType.PC.getApiName()
                          + ", "
                          + PlatformType.PSN.getApiName()
                          + ", or "
                          + PlatformType.XBL.getApiName()
                          + ".")))
          .then();
    }

    PlatformType platformType = platformTypeOptional.get().orElse(null);

    return userLinkRepository
        .findById(discordId)
        .flatMap(
            existingLink -> {
              if (existingLink.getBattleTag().equalsIgnoreCase(battleTag)
                  && existingLink.getPlatformType() == platformType) {
                return event
                    .editReply()
                    .withContent(
                        Possible.of(
                            Optional.of(
                                "Your BattleTag `"
                                    + battleTag
                                    + "` on platform `"
                                    + platformType
                                    + "` is already linked.")));
              }
              existingLink.setBattleTag(battleTag);
              existingLink.setPlatformType(platformType);
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
                                          + "` on platform `"
                                          + platformType
                                          + "`."))));
            })
        .switchIfEmpty(
            userLinkRepository
                .save(
                    UserLink.builder()
                        .discordId(discordId)
                        .battleTag(battleTag)
                        .platformType(platformType)
                        .build())
                .then(
                    event
                        .editReply()
                        .withContent(
                            Possible.of(
                                Optional.of(
                                    "Successfully linked BattleTag: `"
                                        + battleTag
                                        + "` on platform `"
                                        + platformType
                                        + "`.")))))
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
