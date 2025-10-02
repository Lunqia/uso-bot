package dev.lunqia.usobot.ticket;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tickets")
@Data
@Builder
public class Ticket {
  @Id private String id;
  private Long userId;
  private Long guildId;
  private Long channelId;
  private Instant createdAt;
  private Status status;
  private Long closedByUserId;
  private Instant closedAt;

  public enum Status {
    OPEN,
    CLOSED
  }
}
