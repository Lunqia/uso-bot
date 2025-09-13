package dev.lunqia.usobot.catapi;

import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class CatService {
  private final WebClient client;

  public CatService(WebClient.Builder builder, CatApiProperties properties) {
    String apiKey = properties.getApiKey();
    WebClient.Builder base =
        builder
            .baseUrl(properties.getBaseUrl())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    if (StringUtils.hasText(apiKey)) base.defaultHeader("x-api-key", apiKey);

    client = base.build();
  }

  public Mono<String> getRandomCatImageUrl() {
    return client
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
        .bodyToFlux(CatServiceResponse.class)
        .next()
        .map(CatServiceResponse::url)
        .filter(Objects::nonNull);
  }

  public record CatServiceResponse(String id, String url) {}
}
