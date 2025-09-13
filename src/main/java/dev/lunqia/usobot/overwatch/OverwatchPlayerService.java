package dev.lunqia.usobot.overwatch;

import dev.lunqia.usobot.overwatch.model.OwCompleteStats;
import dev.lunqia.usobot.overwatch.model.OwProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OverwatchPlayerService {
  private final OverwatchPlayerClient client;

  public Mono<OwProfile> getProfile(String platform, String battleTag) {
    return client.getProfile(platform, battleTag);
  }

  public Mono<OwCompleteStats> getCompleteStats(String platform, String battleTag) {
    return client.getCompleteStats(platform, battleTag);
  }

  public Mono<Map<String, Object>> getProfileEmbed(String platform, String battleTag) {
    return getProfile(platform, battleTag).map(this::createEmbed);
  }

  public Mono<Map<String, Object>> getCompleteStatsEmbed(String platform, String battleTag) {
    return getCompleteStats(platform, battleTag).map(this::createCompleteStatsEmbed);
  }

  private Map<String, Object> createEmbed(OwProfile profile) {
    Map<String, Object> embed = new HashMap<>();
    embed.put("title", profile.getName() + "'s Overwatch Profile");
    embed.put("color", 0xFF9900);
    embed.put("thumbnail", Map.of("url", profile.getIcon()));
    embed.put("image", Map.of("url", profile.getNamecardImage()));

    List<Map<String, Object>> fields = new ArrayList<>();

    fields.add(
        Map.of(
            "name",
            "Endorsement Level",
            "value",
            profile.getEndorsement() != null ? "**" + profile.getEndorsement() + "**" : "**N/A**",
            "inline",
            true));
    fields.add(
        Map.of(
            "name",
            "Title",
            "value",
            profile.getTitle() != null ? "**" + profile.getTitle() + "**" : "**None**",
            "inline",
            true));
    fields.add(
        Map.of(
            "name",
            "Namecard",
            "value",
            profile.getNamecardTitle() != null
                ? "**" + profile.getNamecardTitle() + "**"
                : "**N/A**",
            "inline",
            true));

    String competitiveInfo = "**N/A**";
    if (profile.getCompetitiveStats() != null) {
      Object season = profile.getCompetitiveStats().get("season");
      Object mostPlayed = profile.getCompetitiveStats().get("mostPlayedHero");
      competitiveInfo =
          String.format(
              "Season: **%s**\nMost Played Hero: **%s**",
              season != null ? season : "N/A",
              mostPlayed != null ? capitalize(mostPlayed.toString()) : "N/A");
    }
    fields.add(Map.of("name", "Competitive Stats", "value", competitiveInfo, "inline", false));

    String quickplayInfo = "**N/A**";
    if (profile.getQuickplayStats() != null) {
      Object mostPlayed = profile.getQuickplayStats().get("mostPlayedHero");
      quickplayInfo =
          String.format(
              "Most Played Hero: **%s**",
              mostPlayed != null ? capitalize(mostPlayed.toString()) : "N/A");
    }
    fields.add(Map.of("name", "Quickplay Stats", "value", quickplayInfo, "inline", false));

    fields.add(
        Map.of(
            "name",
            "Profile Privacy",
            "value",
            profile.getPrivateProfile() != null && profile.getPrivateProfile()
                ? "**Private**"
                : "**Public**",
            "inline",
            true));

    embed.put("fields", fields);
    embed.put(
        "footer",
        Map.of(
            "text", "Data provided by owapi.eu",
            "icon_url", ""));

    return Map.of("embeds", List.of(embed));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> createCompleteStatsEmbed(OwCompleteStats stats) {
    Map<String, Object> embed = new HashMap<>();
    embed.put("title", stats.getName() + "'s Overwatch Complete Stats");
    embed.put("color", 0xFF9900);
    embed.put("thumbnail", Map.of("url", stats.getIcon()));
    embed.put("image", Map.of("url", stats.getNamecardImage()));

    List<Map<String, Object>> fields = new ArrayList<>();

    fields.add(
        Map.of(
            "name",
            "Endorsement Level",
            "value",
            stats.getEndorsement() != null ? "**" + stats.getEndorsement() + "**" : "**N/A**",
            "inline",
            true));
    fields.add(
        Map.of(
            "name",
            "Title",
            "value",
            stats.getTitle() != null ? "**" + stats.getTitle() + "**" : "**None**",
            "inline",
            true));
    fields.add(
        Map.of(
            "name",
            "Namecard",
            "value",
            stats.getNamecardTitle() != null ? "**" + stats.getNamecardTitle() + "**" : "**N/A**",
            "inline",
            true));

    int quickplayGamesWon = 0;
    int competitiveGamesWon = 0;
    if (stats.getQuickPlayStats() != null && stats.getQuickPlayStats().containsKey("topHeroes")) {
      Map<String, Object> topHeroes =
          (Map<String, Object>) stats.getQuickPlayStats().get("topHeroes");
      quickplayGamesWon =
          topHeroes.values().stream()
              .map(hero -> ((Map<String, Object>) hero).get("gamesWon"))
              .filter(Number.class::isInstance)
              .mapToInt(games -> ((Number) games).intValue())
              .sum();
    }
    if (stats.getCompetitiveStats() != null
        && stats.getCompetitiveStats().containsKey("topHeroes")) {
      Map<String, Object> topHeroes =
          (Map<String, Object>) stats.getCompetitiveStats().get("topHeroes");
      competitiveGamesWon =
          topHeroes.values().stream()
              .map(hero -> ((Map<String, Object>) hero).get("gamesWon"))
              .filter(Number.class::isInstance)
              .mapToInt(games -> ((Number) games).intValue())
              .sum();
    }
    int totalGamesWon = quickplayGamesWon + competitiveGamesWon;

    fields.add(Map.of("name", "Games Won", "value", "**" + totalGamesWon + "**", "inline", true));

    String quickplayHeroes = "**N/A**";
    if (stats.getQuickPlayStats() != null && stats.getQuickPlayStats().containsKey("topHeroes")) {
      Map<String, Object> topHeroes =
          (Map<String, Object>) stats.getQuickPlayStats().get("topHeroes");
      quickplayHeroes =
          topHeroes.entrySet().stream()
              .limit(3)
              .map(
                  entry -> {
                    String hero = capitalize(entry.getKey());
                    Map<String, Object> heroStats = (Map<String, Object>) entry.getValue();
                    return String.format(
                        "**%s**: %s (Won: **%s**)",
                        hero, heroStats.get("timePlayed"), heroStats.get("gamesWon"));
                  })
              .collect(Collectors.joining("\n"));
    }
    fields.add(Map.of("name", "Quickplay Top Heroes", "value", quickplayHeroes, "inline", false));

    String competitiveHeroes = "**N/A**";
    if (stats.getCompetitiveStats() != null
        && stats.getCompetitiveStats().containsKey("topHeroes")) {
      Map<String, Object> topHeroes =
          (Map<String, Object>) stats.getCompetitiveStats().get("topHeroes");
      competitiveHeroes =
          topHeroes.entrySet().stream()
              .limit(3)
              .map(
                  entry -> {
                    String hero = capitalize(entry.getKey());
                    Map<String, Object> heroStats = (Map<String, Object>) entry.getValue();
                    return String.format(
                        "**%s**: %s (Won: **%s**)",
                        hero, heroStats.get("timePlayed"), heroStats.get("gamesWon"));
                  })
              .collect(Collectors.joining("\n"));
    }
    fields.add(
        Map.of("name", "Competitive Top Heroes", "value", competitiveHeroes, "inline", false));

    fields.add(
        Map.of(
            "name",
            "Profile Privacy",
            "value",
            stats.getPrivateProfile() != null && stats.getPrivateProfile()
                ? "**Private**"
                : "**Public**",
            "inline",
            true));

    embed.put("fields", fields);
    embed.put("footer", Map.of("text", "Data provided by owapi.eu"));

    return Map.of("embeds", List.of(embed));
  }

  private String capitalize(String str) {
    if (str == null || str.isEmpty()) return str;
    return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
  }
}
