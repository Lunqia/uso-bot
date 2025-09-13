package dev.lunqia.usobot.overwatch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "owapi")
public class OwapiProperties {
  private String baseUrl = "https://owapi.eu";
  private int requestTimeoutSeconds = 10;
}
