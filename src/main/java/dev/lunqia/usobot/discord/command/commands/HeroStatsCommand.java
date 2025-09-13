package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.overwatch.OverwatchPlayerService;
import dev.lunqia.usobot.overwatch.error.ProfileNotFoundException;
import dev.lunqia.usobot.overwatch.model.OwCompleteStats;
import dev.lunqia.usobot.overwatch.model.PlatformType;
import dev.lunqia.usobot.overwatch.model.UserLink;
import dev.lunqia.usobot.overwatch.repository.UserLinkRepository;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class HeroStatsCommand implements SlashCommand {
  private static final String OPTION_PLATFORM = "platform";
  private static final String OPTION_BATTLE_TAG = "battle_tag";
  private static final String OPTION_HERO = "hero";

  private final OverwatchPlayerService service;
  private final UserLinkRepository userLinkRepository;

  @Override
  public String name() {
    return "hero_stats";
  }

  @Override
  public String description() {
    return "Display Overwatch profile stats for a specific hero.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
        ApplicationCommandOptionData.builder()
            .name(OPTION_HERO)
            .description("Hero name, e.g. Mercy, Ana")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .build(),
        ApplicationCommandOptionData.builder()
            .name(OPTION_PLATFORM)
            .description("Platform: pc, psn, or xbl (optional if linked)")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(false)
            .build(),
        ApplicationCommandOptionData.builder()
            .name(OPTION_BATTLE_TAG)
            .description("BattleTag, e.g. Player#1234 (optional if linked)")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(false)
            .build());
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    String discordId = event.getInteraction().getUser().getId().asString();

    return userLinkRepository
        .findById(discordId)
        .defaultIfEmpty(UserLink.builder().build())
        .flatMap(
            userLink -> {
              Optional<String> platformOption =
                  event
                      .getOption(OPTION_PLATFORM)
                      .flatMap(
                          option ->
                              option
                                  .getValue()
                                  .map(ApplicationCommandInteractionOptionValue::asString))
                      .map(platform -> platform.toLowerCase(Locale.ROOT).trim());

              Optional<String> battleTagOption =
                  event
                      .getOption(OPTION_BATTLE_TAG)
                      .flatMap(
                          option ->
                              option
                                  .getValue()
                                  .map(ApplicationCommandInteractionOptionValue::asString));

              Optional<String> heroOption =
                  event
                      .getOption(OPTION_HERO)
                      .flatMap(
                          option ->
                              option
                                  .getValue()
                                  .map(ApplicationCommandInteractionOptionValue::asString))
                      .map(String::toLowerCase);

              String platform =
                  platformOption.orElseGet(
                      () ->
                          Optional.ofNullable(userLink.getPlatformType())
                              .map(PlatformType::getApiName)
                              .orElse(null));

              String battleTag = battleTagOption.orElseGet(userLink::getBattleTag);

              String hero = heroOption.orElse(null);

              if (battleTag == null || battleTag.isBlank()) {
                return event
                    .editReply()
                    .withContent(
                        Possible.of(
                            Optional.of(
                                "No BattleTag provided and none linked. Use `/link` to set one or provide one explicitly.")))
                    .then();
              }

              if (!PlatformType.isValidPlatform(platform)) {
                return event
                    .editReply()
                    .withContent(
                        Possible.of(Optional.of("Invalid platform. Please use pc, psn, or xbl.")))
                    .then();
              }

              if (hero == null || hero.isBlank()) {
                return event
                    .editReply()
                    .withContent(Possible.of(Optional.of("Please provide a hero name.")))
                    .then();
              }

              return service
                  .getCompleteStats(platform, battleTag)
                  .flatMap(
                      stats -> {
                        EmbedCreateSpec embed = createHeroEmbed(stats, hero);
                        return event
                            .editReply()
                            .withEmbeds(Possible.of(Optional.of(List.of(embed))))
                            .then();
                      })
                  .onErrorResume(
                      ProfileNotFoundException.class,
                      exception ->
                          event
                              .editReply()
                              .withContent(Possible.of(Optional.of("Overwatch profile not found.")))
                              .then())
                  .onErrorResume(
                      exception -> {
                        log.error("Error retrieving Overwatch hero stats", exception);
                        return event
                            .editReply()
                            .withContent(
                                Possible.of(
                                    Optional.of("Failed to retrieve Overwatch hero stats.")))
                            .then();
                      });
            });
  }

  @SuppressWarnings("unchecked")
  private EmbedCreateSpec createHeroEmbed(OwCompleteStats stats, String heroName) {
    EmbedCreateSpec.Builder builder =
        EmbedCreateSpec.builder()
            .title(stats.getName() + "'s " + capitalize(heroName) + " Stats")
            .color(discord4j.rest.util.Color.of(0xFF9900))
            .thumbnail(stats.getIcon())
            .image(stats.getNamecardImage());

    if (stats.getQuickPlayStats() != null && stats.getQuickPlayStats().containsKey("topHeroes")) {
      Map<String, Object> topHeroes =
          (Map<String, Object>) stats.getQuickPlayStats().get("topHeroes");
      if (topHeroes.containsKey(heroName)) {
        Map<String, Object> heroStats = (Map<String, Object>) topHeroes.get(heroName);
        builder
            .addField("Time Played", heroStats.get("timePlayed").toString(), true)
            .addField("Games Won", "**" + heroStats.get("gamesWon") + "**", true)
            .addField("Eliminations/Life", heroStats.get("eliminationsPerLife").toString(), true)
            .addField("Best Multi-Kill", "**" + heroStats.get("multiKillBest") + "**", true);
      }
    }

    if (stats.getCompetitiveStats() != null
        && stats.getCompetitiveStats().containsKey("topHeroes")) {
      Map<String, Object> topHeroes =
          (Map<String, Object>) stats.getCompetitiveStats().get("topHeroes");
      if (topHeroes.containsKey(heroName)) {
        Map<String, Object> heroStats = (Map<String, Object>) topHeroes.get(heroName);
        builder
            .addField("Comp Time Played", heroStats.get("timePlayed").toString(), true)
            .addField("Comp Games Won", "**" + heroStats.get("gamesWon") + "**", true)
            .addField(
                "Comp Eliminations/Life", heroStats.get("eliminationsPerLife").toString(), true)
            .addField("Comp Best Multi-Kill", "**" + heroStats.get("multiKillBest") + "**", true);
      }
    }

    builder.footer("Data provided by owapi.eu", "");
    return builder.build();
  }

  private String capitalize(String text) {
    if (text == null || text.isEmpty()) return text;
    return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
  }
}
