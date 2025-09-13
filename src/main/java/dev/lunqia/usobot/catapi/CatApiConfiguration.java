package dev.lunqia.usobot.catapi;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(CatApiProperties.class)
public class CatApiConfiguration {

  @Bean
  public CatService catService(WebClient.Builder builder, CatApiProperties properties) {
    return new CatService(builder, properties);
  }
}
