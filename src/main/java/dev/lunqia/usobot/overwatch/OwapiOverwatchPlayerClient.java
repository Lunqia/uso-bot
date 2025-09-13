package dev.lunqia.usobot.overwatch;

import dev.lunqia.usobot.overwatch.error.BadRequestException;
import dev.lunqia.usobot.overwatch.error.InternalServerErrorException;
import dev.lunqia.usobot.overwatch.error.NotAcceptableException;
import dev.lunqia.usobot.overwatch.error.OwapiException;
import dev.lunqia.usobot.overwatch.error.ProfileNotFoundException;
import dev.lunqia.usobot.overwatch.error.ServiceUnavailableException;
import dev.lunqia.usobot.overwatch.model.OwCompleteStats;
import dev.lunqia.usobot.overwatch.model.OwProfile;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class OwapiOverwatchPlayerClient implements OverwatchPlayerClient {
  private final WebClient webClient;
  private final OwapiProperties properties;

  public OwapiOverwatchPlayerClient(WebClient.Builder builder, OwapiProperties properties) {
    this.properties = properties;
    webClient =
        builder
            .baseUrl(properties.getBaseUrl())
            .defaultHeaders(
                headers ->
                    headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE)))
            .build();
  }

  @Override
  public Mono<OwProfile> getProfile(String platform, String battleTag) {
    String path =
        "/stats/" + platform + "/" + normalizeBattleTag(battleTag) + "/profile"; // NOSONAR
    return webClient
        .get()
        .uri(path)
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(
            response -> {
              if (response.statusCode().is4xxClientError()
                  || response.statusCode().is5xxServerError()) {
                return mapError(response).flatMap(Mono::error);
              }
              return response.bodyToMono(OwProfile.class);
            })
        .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
  }

  @Override
  public Mono<OwCompleteStats> getCompleteStats(String platform, String battleTag) {
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/stats/{platform}/{battle_tag}/complete")
                    .build(platform, normalizeBattleTag(battleTag)))
        .accept(MediaType.APPLICATION_JSON)
        .exchangeToMono(
            response -> {
              if (response.statusCode().is4xxClientError()
                  || response.statusCode().is5xxServerError()) {
                return mapError(response).flatMap(Mono::error);
              }
              return response.bodyToMono(OwCompleteStats.class);
            })
        .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
  }

  private Mono<? extends Throwable> mapError(ClientResponse response) {
    int code = response.statusCode().value();
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("")
        .map(
            body ->
                switch (code) {
                  case 400 -> new BadRequestException(message("Bad Request", body));
                  case 404 -> new ProfileNotFoundException(message("Profile Not Found", body));
                  case 406 -> new NotAcceptableException(message("Not Acceptable", body));
                  case 503 -> new ServiceUnavailableException(message("Service Unavailable", body));
                  default ->
                      code >= 500
                          ? new InternalServerErrorException(message("Internal Server Error", body))
                          : new OwapiException(message("Unexpected Error", body));
                });
  }

  private String message(String title, String body) {
    return body == null || body.isBlank() ? title : title + ": " + body;
  }

  private String normalizeBattleTag(String battleTag) {
    if (battleTag == null) return "";
    String trimmed = battleTag.trim();
    return trimmed.isEmpty() ? trimmed : trimmed.replace('#', '-');
  }
}
