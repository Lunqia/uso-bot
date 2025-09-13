package dev.lunqia.usobot.catapi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "catapi")
public class CatApiProperties {
  private String baseUrl = "https://api.thecatapi.com";
  private String apiKey;
}
