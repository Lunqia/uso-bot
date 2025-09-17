package dev.lunqia.usobot.animal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "animal-api")
@Data
public class AnimalApiProperties {
  private String baseUrl;
}
