package dev.lunqia.usobot.overwatch2.link;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_links")
@Data
@Builder
public class UserLink {
  @Id private String discordId;
  private String battleTag;
}
