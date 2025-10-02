package dev.lunqia.usobot.overwatch2.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lunqia.usobot.overwatch2.OverfastApiCache;
import dev.lunqia.usobot.overwatch2.OverfastApiEndpointType;
import dev.lunqia.usobot.overwatch2.OverfastApiError;
import dev.lunqia.usobot.overwatch2.player.stats.summary.PlayerStatsSummary;
import dev.lunqia.usobot.overwatch2.player.summary.PlayerSummary;
import dev.lunqia.usobot.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class PlayerService {
  private final WebClient webClient;
  private final OverfastApiCache overfastApiCache;
  private final ObjectMapper objectMapper;

  public PlayerService(OverfastApiCache overfastApiCache, ObjectMapper objectMapper) {
    webClient =
        WebClient.builder()
            .baseUrl(OverfastApiEndpointType.BASE_URL)
            .defaultHeader(HttpHeaders.USER_AGENT, HttpUtils.USER_AGENT)
            .defaultHeaders(
                headers ->
                    headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE)))
            .build();
    this.overfastApiCache = overfastApiCache;
    this.objectMapper = objectMapper;
  }

  public Mono<PlayerSummary> getPlayerSummary(String playerId) {
    return fetchWithCache(
        OverfastApiEndpointType.PLAYERS_PLAYER_SUMMARY, playerId, PlayerSummary.class, 600);
  }

  public Mono<PlayerStatsSummary> getPlayerStatsSummary(String playerId) {
    return fetchWithCache(
        OverfastApiEndpointType.PLAYERS_PLAYER_STATS_SUMMARY,
        playerId,
        PlayerStatsSummary.class,
        600);
  }

  private <T> Mono<T> fetchWithCache(
      OverfastApiEndpointType endpointType,
      String playerId,
      Class<T> responseType,
      long ttlSeconds) {
    T cached = overfastApiCache.get(endpointType, playerId, responseType);
    if (cached != null) return Mono.just(cached);

    return webClient
        .get()
        .uri(endpointType.getUrl(playerId))
        .retrieve()
        .bodyToMono(responseType)
        .doOnNext(t -> overfastApiCache.put(endpointType, playerId, t, ttlSeconds))
        .publishOn(Schedulers.boundedElastic());
  }

  public OverfastApiError handleError(Throwable throwable) {
    if (throwable instanceof WebClientResponseException webClientResponseException) {
      int statusCode = webClientResponseException.getStatusCode().value();
      String body = webClientResponseException.getResponseBodyAsString();
      String message =
          "Unknown error has occurred on the data provider side, please try again later.";

      try {
        JsonNode jsonNode = objectMapper.readTree(body);
        if (jsonNode.has("error")) message = jsonNode.get("error").asText();
        else if (jsonNode.has("detail")) message = jsonNode.get("detail").toPrettyString();
        else message = body;
      } catch (Exception ignored) { // NOSONAR
      }

      return new OverfastApiError(String.valueOf(statusCode), message);
    }
    return new OverfastApiError("Unknown", throwable.getMessage());
  }
}
