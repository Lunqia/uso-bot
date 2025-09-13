package dev.lunqia.usobot.overwatch.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(collection = "user_links")
public class UserLink {
  @Id private String discordId;
  private String battleTag;
  private PlatformType platformType;
}
