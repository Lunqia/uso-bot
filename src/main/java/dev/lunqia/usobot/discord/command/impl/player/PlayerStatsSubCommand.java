package dev.lunqia.usobot.discord.command.impl.player;

import dev.lunqia.usobot.discord.command.SlashSubCommand;
import dev.lunqia.usobot.discord.emoji.EmojiConstants;
import dev.lunqia.usobot.overwatch2.OverfastApiError;
import dev.lunqia.usobot.overwatch2.link.UserLink;
import dev.lunqia.usobot.overwatch2.link.UserLinkRepository;
import dev.lunqia.usobot.overwatch2.player.PlayerService;
import dev.lunqia.usobot.overwatch2.player.stats.summary.PlayerStatsSummary;
import dev.lunqia.usobot.overwatch2.player.summary.PlayerSummary;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerStatsSubCommand implements SlashSubCommand {
  private static final String OPTION_BATTLE_TAG = "battle_tag";

  private final UserLinkRepository userLinkRepository;
  private final PlayerService playerService;

  @Override
  public String name() {
    return "stats";
  }

  @Override
  public String description() {
    return "Look up an Overwatch 2 player's statistics by BattleTag or linked account.";
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
          String discordUserId = event.getInteraction().getUser().getId().asString();
          Optional<String> battleTagInput =
              event.getOptions().stream()
                  .findFirst()
                  .flatMap(option -> option.getOption(OPTION_BATTLE_TAG))
                  .flatMap(ApplicationCommandInteractionOption::getValue)
                  .map(ApplicationCommandInteractionOptionValue::asString)
                  .filter(tag -> !tag.isBlank());

          return battleTagInput
              .map(battleTag -> fetchAndReplyWithStats(event, battleTag))
              .orElseGet(
                  () ->
                      userLinkRepository
                          .findById(discordUserId)
                          .defaultIfEmpty(UserLink.builder().build())
                          .flatMap(
                              userLink -> {
                                String linkedBattleTag = userLink.getBattleTag();
                                if (linkedBattleTag == null || linkedBattleTag.isBlank()) {
                                  return event
                                      .editReply()
                                      .withContent(
                                          Possible.of(
                                              Optional.of(
                                                  "No BattleTag found. Provide one or use `/link`.")))
                                      .then();
                                }
                                return fetchAndReplyWithStats(event, linkedBattleTag);
                              })
                          .onErrorResume(
                              error -> {
                                log.error(
                                    "Error handling '/player stats' command for {}",
                                    discordUserId,
                                    error);
                                return event
                                    .editReply()
                                    .withContent(
                                        Possible.of(
                                            Optional.of(
                                                "An unexpected error occurred, please try again later.")))
                                    .then();
                              }));
        });
  }

  private Mono<Void> fetchAndReplyWithStats(ChatInputInteractionEvent event, String battleTag) {
    String playerId = battleTag.trim().replace("#", "-");
    Mono<PlayerSummary> playerSummaryMono =
        playerService
            .getPlayerSummary(playerId)
            .switchIfEmpty(Mono.just(PlayerSummary.builder().build()));
    Mono<PlayerStatsSummary> playerStatsMono =
        playerService
            .getPlayerStatsSummary(playerId)
            .switchIfEmpty(Mono.just(PlayerStatsSummary.builder().build()));

    return Mono.zip(
            playerSummaryMono.switchIfEmpty(Mono.just(PlayerSummary.builder().build())),
            playerStatsMono.switchIfEmpty(Mono.just(PlayerStatsSummary.builder().build())))
        .flatMap(
            tuple -> {
              PlayerSummary playerSummary = tuple.getT1();
              PlayerStatsSummary playerStatsSummary = tuple.getT2();

              boolean hasData =
                  (playerSummary.getUsername() != null && !playerSummary.getUsername().isBlank())
                      || (playerStatsSummary.getGeneral() != null
                          && playerStatsSummary.getGeneral().getGamesPlayed() != null);

              if (!hasData)
                return event
                    .editReply()
                    .withEmbeds(
                        EmbedCreateSpec.builder()
                            .color(Color.RED)
                            .title("❌ No data available")
                            .description(
                                "No data found for BattleTag: `"
                                    + battleTag
                                    + "`.\nMake sure the BattleTag is correct and the profile is public.")
                            .build())
                    .then();

              List<EmbedCreateSpec> embedCreateSpecs = new ArrayList<>();
              embedCreateSpecs.add(buildPlayerStatsEmbed(playerSummary, playerStatsSummary));
              return event.editReply().withEmbeds(embedCreateSpecs).then();
            })
        .onErrorResume(
            exception -> {
              OverfastApiError overfastApiError = playerService.handleError(exception);
              return event
                  .editReply()
                  .withEmbeds(
                      EmbedCreateSpec.builder()
                          .color(Color.RED)
                          .title("❌ Error")
                          .description(
                              "Error fetching data:\n```\n" + overfastApiError.message() + "\n```")
                          .build())
                  .then();
            });
  }

  private EmbedCreateSpec buildPlayerStatsEmbed(
      PlayerSummary playerSummary, PlayerStatsSummary playerStatsSummary) {
    EmbedCreateSpec.Builder embedBuilder =
        EmbedCreateSpec.builder()
            .color(Color.of(0xFF9900))
            .title(playerSummary.getUsername() + " - Stats");

    if (playerSummary.getLastUpdatedAt() != null)
      embedBuilder.description(
          "Last updated: <t:"
              + playerSummary.getLastUpdatedAt()
              + ":R>\n*Please note that this command is still a work in progress and will be updated with more features over time.*");

    embedBuilder.addField("", "", true);

    if (playerSummary.getAvatar() != null) embedBuilder.thumbnail(playerSummary.getAvatar());
    if (playerSummary.getTitle() != null)
      embedBuilder.addField("Title", playerSummary.getTitle(), false);
    if (playerSummary.getNamecard() != null && !playerSummary.getNamecard().isBlank())
      embedBuilder.image(playerSummary.getNamecard());

    if (playerSummary.getEndorsement() != null)
      embedBuilder.addField(
          "Endorsement",
          getEndorsementLevelEmoji(playerSummary.getEndorsement().getLevel()),
          false);

    if (playerStatsSummary.getGeneral() != null) {
      PlayerStatsSummary.GeneralStats general = playerStatsSummary.getGeneral();
      if (general.getGamesPlayed() != null)
        embedBuilder.addField("Games Played", String.valueOf(general.getGamesPlayed()), true);
      if (general.getGamesWon() != null)
        embedBuilder.addField("Games Won", String.valueOf(general.getGamesWon()), true);
      if (general.getGamesLost() != null)
        embedBuilder.addField("Games Lost", String.valueOf(general.getGamesLost()), true);
      if (general.getWinrate() != null)
        embedBuilder.addField("Winrate", String.format("%.2f%%", general.getWinrate()), true);
      if (general.getKda() != null)
        embedBuilder.addField("KDA", String.format("%.2f", general.getKda()), true);
      if (general.getTimePlayed() != null)
        embedBuilder.addField("Time Played", formatTimePlayed(general.getTimePlayed()), true);
    }

    return embedBuilder.build();
  }

  private String formatTimePlayed(Long seconds) {
    if (seconds == null || seconds <= 0) return "0s";
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    return hours + "h " + minutes + "m " + secs + "s";
  }

  private String getEndorsementLevelEmoji(Integer level) {
    level = Math.clamp(level, 1, 5);
    return switch (level) {
      case 2 -> EmojiConstants.OVERWATCH_2_ENDORSEMENT_LEVEL_2;
      case 3 -> EmojiConstants.OVERWATCH_2_ENDORSEMENT_LEVEL_3;
      case 4 -> EmojiConstants.OVERWATCH_2_ENDORSEMENT_LEVEL_4;
      case 5 -> EmojiConstants.OVERWATCH_2_ENDORSEMENT_LEVEL_5;
      default -> EmojiConstants.OVERWATCH_2_ENDORSEMENT_LEVEL_1;
    };
  }
}
