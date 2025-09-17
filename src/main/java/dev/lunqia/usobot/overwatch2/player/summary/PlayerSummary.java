package dev.lunqia.usobot.overwatch2.player.summary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSummary {
  private String username;
  private String avatar;
  private String namecard;
  private String title;
  private Endorsement endorsement;
  private Competitive competitive;

  @JsonProperty("last_updated_at")
  private Long lastUpdatedAt;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Endorsement {
    private Integer level;
    private String frame;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Competitive {
    private Platform pc;
    private Platform console;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Platform {
      private Integer season;
      private Role tank;
      private Role damage;
      private Role support;
      private Role open;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Role {
      private String division;
      private Integer rank;

      @JsonProperty("role_icon")
      private String roleIcon;

      @JsonProperty("rank_icon")
      private String rankIcon;

      @JsonProperty("tier_icon")
      private String tierIcon;
    }
  }
}
