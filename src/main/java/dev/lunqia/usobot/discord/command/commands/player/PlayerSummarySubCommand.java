package dev.lunqia.usobot.discord.command.commands.player;

import dev.lunqia.usobot.discord.command.SlashSubCommand;
import dev.lunqia.usobot.overwatch2.OverfastApiError;
import dev.lunqia.usobot.overwatch2.Overwatch2Constants;
import dev.lunqia.usobot.overwatch2.link.UserLink;
import dev.lunqia.usobot.overwatch2.link.UserLinkRepository;
import dev.lunqia.usobot.overwatch2.player.PlayerService;
import dev.lunqia.usobot.overwatch2.player.summary.PlayerSummary;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Color;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerSummarySubCommand implements SlashSubCommand {
  private static final String OPTION_BATTLE_TAG = "battle_tag";

  private final UserLinkRepository userLinkRepository;
  private final PlayerService playerService;

  @Override
  public String name() {
    return "summary";
  }

  @Override
  public String description() {
    return "Look up an Overwatch 2 player's summary by BattleTag or linked account.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
        ApplicationCommandOptionData.builder()
            .name(OPTION_BATTLE_TAG)
            .description("Case-sensitive BattleTag, e.g. Lunqia#2834 (optional if linked)")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(false)
            .build());
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    return Mono.defer(
        () -> {
          String discordId = event.getInteraction().getUser().getId().asString();
          Optional<String> battleTagOption =
              event.getOptions().stream()
                  .findFirst()
                  .flatMap(option -> option.getOption(OPTION_BATTLE_TAG))
                  .flatMap(ApplicationCommandInteractionOption::getValue)
                  .map(ApplicationCommandInteractionOptionValue::asString)
                  .filter(tag -> !tag.isBlank());

          if (battleTagOption.isPresent())
            return fetchAndReplyPlayerSummary(event, battleTagOption.get(), discordId);

          return userLinkRepository
              .findById(discordId)
              .defaultIfEmpty(UserLink.builder().build())
              .flatMap(
                  userLink -> {
                    String battleTag = userLink.getBattleTag();
                    if (battleTag == null || battleTag.isBlank()) {
                      return event
                          .editReply()
                          .withContent(
                              Possible.of(
                                  Optional.of("No BattleTag found. Provide one or use `/link`.")))
                          .then();
                    }
                    return fetchAndReplyPlayerSummary(event, battleTag, discordId);
                  })
              .onErrorResume(
                  exception -> {
                    log.error(
                        "Error handling '/player summary' command for {}", discordId, exception);
                    return event
                        .editReply()
                        .withContent(
                            Possible.of(
                                Optional.of(
                                    "An unexpected error occurred, please try again later.")))
                        .then();
                  });
        });
  }

  private Mono<Void> fetchAndReplyPlayerSummary(
      ChatInputInteractionEvent event, String battleTag, String discordId) {
    if (!discordId.equals("199228071079968768")) {
      return event
          .editReply()
          .withContent(
              Possible.of(
                  Optional.of(
                      "This command is temporarily disabled due to a recode of the Overwatch module, please check back later.")))
          .then();
    }

    String playerId = battleTag.trim().replace("#", "-");
    return playerService
        .getPlayerSummary(playerId)
        .flatMap(
            playerSummaryLike -> {
              if (playerSummaryLike instanceof OverfastApiError(String type, String message)) {
                if (!type.equals("404"))
                  log.error(
                      "OverfastApiError while fetching player summary for {}: {} - {}",
                      playerId,
                      type,
                      message);

                return event
                    .editReply()
                    .withEmbeds(
                        EmbedCreateSpec.builder()
                            .color(Color.RED)
                            .title("❌ Error")
                            .description(
                                type.equals("404")
                                    ? "Player with BattleTag `" + battleTag + "` was not found."
                                    : "Error fetching data:\n```java\n" + message + "\n```")
                            .build())
                    .then();
              } else if (playerSummaryLike instanceof PlayerSummary playerSummary) {
                return event
                    .editReply()
                    .withEmbeds(
                        buildPlayerSummaryEmbed(playerSummary),
                        buildPlayerSummaryEndorsementPartEmbed(playerSummary))
                    .then();
              }
              return Mono.empty();
            });
  }

  private EmbedCreateSpec buildPlayerSummaryEmbed(PlayerSummary playerSummary) {
    EmbedCreateSpec.Builder playerSummaryEmbedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.of(0xFF9900))
            .title(playerSummary.getUsername() + " - Player Summary")
            .thumbnail(playerSummary.getAvatar())
            .addField("Title", Optional.ofNullable(playerSummary.getTitle()).orElse("N/A"), false);

    if (playerSummary.getNamecard() != null && !playerSummary.getNamecard().isBlank())
      playerSummaryEmbedBuilder.image(playerSummary.getNamecard());

    if (playerSummary.getCompetitive() != null) {
      if (playerSummary.getCompetitive().getPc() != null)
        playerSummaryEmbedBuilder.addField(
            "PC Competitive", formatPlatform(playerSummary.getCompetitive().getPc()), false);

      if (playerSummary.getCompetitive().getConsole() != null)
        playerSummaryEmbedBuilder.addField(
            "Console Competitive",
            formatPlatform(playerSummary.getCompetitive().getConsole()),
            false);
    }

    if (playerSummary.getLastUpdatedAt() != null)
      playerSummaryEmbedBuilder.description(
          "Last updated: <t:" + playerSummary.getLastUpdatedAt() + ":R>\n");

    return playerSummaryEmbedBuilder.build();
  }

  private EmbedCreateSpec buildPlayerSummaryEndorsementPartEmbed(PlayerSummary playerSummary) {
    if (playerSummary.getEndorsement() == null) return null;
    int endorsementLevel = Optional.ofNullable(playerSummary.getEndorsement().getLevel()).orElse(1);
    return EmbedCreateSpec.builder()
        .color(Color.of(0xFF9900))
        .title(playerSummary.getUsername() + " - Endorsement")
        .thumbnail(getEndorsementLevelThumbnail(playerSummary.getEndorsement().getLevel()))
        .addField("Level", String.valueOf(endorsementLevel), false)
        .build();
  }

  private String formatPlatform(PlayerSummary.Competitive.Platform platform) {
    StringBuilder platformStringBuilder = new StringBuilder();
    platformStringBuilder.append("Season: **").append(platform.getSeason()).append("**\n");

    boolean hasAnyRole = false;

    if (platform.getTank() != null) {
      platformStringBuilder.append("Tank: ").append(formatRole(platform.getTank())).append("\n");
      hasAnyRole = true;
    }

    if (platform.getDamage() != null) {
      platformStringBuilder
          .append("Damage: ")
          .append(formatRole(platform.getDamage()))
          .append("\n");
      hasAnyRole = true;
    }

    if (platform.getSupport() != null) {
      platformStringBuilder
          .append("Support: ")
          .append(formatRole(platform.getSupport()))
          .append("\n");
      hasAnyRole = true;
    }

    if (platform.getOpen() != null) {
      platformStringBuilder
          .append("Open Queue: ")
          .append(formatRole(platform.getOpen()))
          .append("\n");
      hasAnyRole = true;
    }

    if (!hasAnyRole) platformStringBuilder.append("Data not available for this season.");

    return platformStringBuilder.toString();
  }

  private String formatRole(PlayerSummary.Competitive.Role role) {
    return "**" + role.getDivision() + "** (Rank **" + role.getRank() + "**)";
  }

  private String getEndorsementLevelThumbnail(Integer endorsementLevel) {
    endorsementLevel = Math.clamp(endorsementLevel, 1, 5);
    return switch (endorsementLevel) {
      case 2 -> Overwatch2Constants.ENDORSEMENT_LEVEL_2_URL;
      case 3 -> Overwatch2Constants.ENDORSEMENT_LEVEL_3_URL;
      case 4 -> Overwatch2Constants.ENDORSEMENT_LEVEL_4_URL;
      case 5 -> Overwatch2Constants.ENDORSEMENT_LEVEL_5_URL;
      default -> Overwatch2Constants.ENDORSEMENT_LEVEL_1_URL;
    };
  }
}
