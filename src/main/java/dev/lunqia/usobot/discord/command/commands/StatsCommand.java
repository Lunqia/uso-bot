package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.discord.command.SlashCommand;
import dev.lunqia.usobot.overwatch.OverwatchPlayerService;
import dev.lunqia.usobot.overwatch.error.ProfileNotFoundException;
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
public class StatsCommand implements SlashCommand {
  private static final String OPTION_PLATFORM = "platform";
  private static final String OPTION_BATTLE_TAG = "battle_tag";

  private final OverwatchPlayerService service;
  private final UserLinkRepository userLinkRepository;

  @Override
  public String name() {
    return "stats";
  }

  @Override
  public String description() {
    return "Look up an Overwatch profile by platform and BattleTag (complete stats by default).";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
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

              String platform =
                  platformOption.orElseGet(
                      () ->
                          Optional.ofNullable(userLink.getPlatformType())
                              .map(PlatformType::getApiName)
                              .orElse(null));

              String battleTag =
                  battleTagOption.orElseGet(
                      () -> Optional.ofNullable(userLink.getBattleTag()).orElse(null));

              if (battleTag == null || battleTag.isBlank() || platform == null) {
                return event
                    .editReply()
                    .withContent(
                        Possible.of(
                            Optional.of(
                                "No platform or BattleTag could be found. Please provide one or use /link.")))
                    .then();
              }

              if (!PlatformType.isValidPlatform(platform)) {
                return event
                    .editReply()
                    .withContent(
                        Possible.of(Optional.of("Invalid platform. Please use pc, psn, or xbl.")))
                    .then();
              }

              return service
                  .getCompleteStatsEmbed(platform, battleTag)
                  .flatMap(
                      embedMap -> {
                        EmbedCreateSpec embed = createEmbedFromMap(embedMap);
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
                        log.error("Error retrieving Overwatch profile", exception);
                        return event
                            .editReply()
                            .withContent(
                                Possible.of(Optional.of("Failed to retrieve Overwatch profile.")))
                            .then();
                      });
            });
  }

  @SuppressWarnings("unchecked")
  private EmbedCreateSpec createEmbedFromMap(Map<String, Object> embedMap) {
    List<Map<String, Object>> embeds = (List<Map<String, Object>>) embedMap.get("embeds");
    Map<String, Object> embedData = embeds.getFirst();

    EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

    if (embedData.containsKey("title")) {
      builder.title((String) embedData.get("title"));
    }

    if (embedData.containsKey("color")) {
      builder.color(discord4j.rest.util.Color.of((Integer) embedData.get("color")));
    }

    if (embedData.containsKey("thumbnail")) {
      Map<String, Object> thumbnail = (Map<String, Object>) embedData.get("thumbnail");
      if (thumbnail.containsKey("url")) {
        builder.thumbnail((String) thumbnail.get("url"));
      }
    }

    if (embedData.containsKey("image")) {
      Map<String, Object> image = (Map<String, Object>) embedData.get("image");
      if (image.containsKey("url")) {
        builder.image((String) image.get("url"));
      }
    }

    if (embedData.containsKey("fields")) {
      List<Map<String, Object>> fields = (List<Map<String, Object>>) embedData.get("fields");
      for (Map<String, Object> field : fields) {
        String fieldName = (String) field.get("name");
        String fieldValue = (String) field.get("value");
        Boolean isInline = (Boolean) field.get("inline");
        builder.addField(fieldName, fieldValue, isInline != null && isInline);
      }
    }

    if (embedData.containsKey("footer")) {
      Map<String, Object> footer = (Map<String, Object>) embedData.get("footer");
      String footerText = (String) footer.get("text");
      String footerIconUrl =
          footer.containsKey("icon_url") ? (String) footer.get("icon_url") : null;
      builder.footer(footerText, footerIconUrl);
    }

    return builder.build();
  }
}
