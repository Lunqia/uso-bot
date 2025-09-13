package dev.lunqia.usobot.overwatch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OwCompleteStats {
  private String icon;
  private String name;
  private Integer endorsement;
  private String endorsementIcon;
  private String title;
  private String namecardId;
  private String namecardTitle;
  private String namecardImage;
  private List<OwRating> ratings;
  private String gamesPlayed;
  private Integer gamesWon;
  private Integer gamesLost;
  private Map<String, Object> quickPlayStats;
  private Map<String, Object> competitiveStats;

  @JsonProperty("private")
  private Boolean privateProfile;
}
