package dev.lunqia.usobot.catapi;

import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class CatApiService {
  private final WebClient webClient;

  public CatApiService(WebClient.Builder webClientBuilder, CatApiProperties catApiProperties) {
    String catApiKey = catApiProperties.getApiKey();
    webClientBuilder =
        webClientBuilder
            .baseUrl(catApiProperties.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    if (StringUtils.hasText(catApiKey)) webClientBuilder.defaultHeader("x-api-key", catApiKey);

    webClient = webClientBuilder.build();
  }

  public Mono<String> getRandomCatImageUrl() {
    return webClient
        .get()
        .uri(
            uri ->
                uri.path("/v1/images/search")
                    .queryParam("size", "med")
                    .queryParam("mime_types", "jpg")
                    .queryParam("format", "json")
                    .queryParam("has_breeds", true)
                    .queryParam("order", "RANDOM")
                    .queryParam("page", 0)
                    .queryParam("limit", 1)
                    .build())
        .retrieve()
        .bodyToFlux(CatApiServiceResponse.class)
        .next()
        .map(CatApiServiceResponse::url)
        .filter(Objects::nonNull);
  }

  public record CatApiServiceResponse(String id, String url) {}
}
