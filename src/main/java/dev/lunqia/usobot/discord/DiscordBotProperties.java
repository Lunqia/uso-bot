package dev.lunqia.usobot.discord;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "discord.bot")
public class DiscordBotProperties {
  private String token;
  private List<Long> guildIds;
}
