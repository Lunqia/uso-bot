package dev.lunqia.usobot.overwatch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OwapiProperties.class)
public class OwapiConfiguration {
  @Bean
  public OverwatchPlayerClient overwatchPlayerClient(
      WebClient.Builder builder, OwapiProperties properties) {
    return new OwapiOverwatchPlayerClient(builder, properties);
  }
}
