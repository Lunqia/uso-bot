package dev.lunqia.usobot.overwatch2.player.stats.summary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerStatsSummary {
  private GeneralStats general;
  private Map<String, GeneralStats> heroes;
  private Map<String, GeneralStats> roles;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class GeneralStats {
    private CombatStats average;
    private CombatStats total;

    @JsonProperty("games_lost")
    private Integer gamesLost;

    @JsonProperty("games_played")
    private Integer gamesPlayed;

    @JsonProperty("games_won")
    private Integer gamesWon;

    private Double kda;

    @JsonProperty("time_played")
    private Long timePlayed;

    private Double winrate;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CombatStats {
    private Double assists;
    private Double damage;
    private Double deaths;
    private Double eliminations;
    private Double healing;
  }
}
