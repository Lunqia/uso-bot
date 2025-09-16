package dev.lunqia.usobot.overwatch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "owapi")
@Data
public class OwapiProperties {
  private String baseUrl = "https://owapi.eu";
  private int requestTimeoutSeconds = 10;
}
