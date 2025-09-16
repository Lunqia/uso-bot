package dev.lunqia.usobot.animalapi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "animal-api")
@Data
public class AnimalApiProperties {
  private String baseUrl;
}
