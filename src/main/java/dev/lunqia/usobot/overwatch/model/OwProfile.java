package dev.lunqia.usobot.overwatch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OwProfile {
  private String icon;
  private String name;
  private Integer endorsement;
  private String endorsementIcon;
  private String title;
  private String namecardId;
  private String namecardTitle;
  private String namecardImage;
  private Map<String, Object> competitiveStats;
  private Map<String, Object> quickplayStats;
  private List<OwRating> ratings;

  @JsonProperty("private")
  private Boolean privateProfile;
}
