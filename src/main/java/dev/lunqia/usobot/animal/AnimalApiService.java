package dev.lunqia.usobot.animal;

import dev.lunqia.usobot.utils.HttpUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@EnableConfigurationProperties(AnimalApiProperties.class)
public class AnimalApiService {
  private final WebClient webClient;

  public AnimalApiService(AnimalApiProperties animalApiProperties) {
    webClient =
        WebClient.builder()
            .baseUrl(animalApiProperties.getBaseUrl())
            .defaultHeader(HttpHeaders.USER_AGENT, HttpUtils.USER_AGENT)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .build();
  }

  public Mono<String> getRandomAnimalImageUrl(AnimalType animalType) {
    return webClient
        .get()
        .uri("/img/{animal}", animalType.getApiName())
        .retrieve()
        .bodyToMono(AnimalResponse.class)
        .map(AnimalResponse::image);
  }

  public Mono<String> getRandomAnimalFact(AnimalType animalType) {
    return webClient
        .get()
        .uri("/fact/{animal}", animalType.getApiName())
        .retrieve()
        .bodyToMono(AnimalResponse.class)
        .map(AnimalResponse::fact);
  }

  public Mono<AnimalResponse> getRandomAnimalImageAndFact(AnimalType animalType) {
    return webClient
        .get()
        .uri("/all/{animal}", animalType.getApiName())
        .retrieve()
        .bodyToMono(AnimalResponse.class);
  }

  public record AnimalResponse(
      String animal, String image, String fact, String image_id, String fact_id) {}
}
