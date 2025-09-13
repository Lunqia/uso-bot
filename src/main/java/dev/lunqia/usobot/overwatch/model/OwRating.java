package dev.lunqia.usobot.overwatch.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OwRating {
  private String group;
  private Integer tier;
  private String role;
  private String roleIcon;
  private String rankIcon;
}
