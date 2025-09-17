package dev.lunqia.usobot.overwatch2.player;

import dev.lunqia.usobot.overwatch2.OverfastApiCache;
import dev.lunqia.usobot.overwatch2.OverfastApiEndpointType;
import dev.lunqia.usobot.overwatch2.OverfastApiError;
import dev.lunqia.usobot.overwatch2.player.summary.PlayerSummary;
import dev.lunqia.usobot.overwatch2.player.summary.PlayerSummaryLike;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class PlayerService {
  private final WebClient webClient;
  private final OverfastApiCache overfastApiCache;
  private final ObjectMapper objectMapper;

  public PlayerService(
      WebClient.Builder webClientBuilder,
      OverfastApiCache overfastApiCache,
      ObjectMapper objectMapper) {
    this.webClient =
        webClientBuilder
            .baseUrl(OverfastApiEndpointType.BASE_URL)
            .defaultHeaders(
                headers ->
                    headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE)))
            .build();
    this.overfastApiCache = overfastApiCache;
    this.objectMapper = objectMapper;
  }

  /**
   * Get player summary : name, avatar, competitive ranks, etc.
   *
   * @param playerId BattleTag (with "#" replaced by "-"), e.g. <b>Lunqia-2834</b>. Case-sensitive.
   * @return Mono<PlayerSummary>
   */
  public Mono<PlayerSummaryLike> getPlayerSummary(String playerId) {
    PlayerSummaryLike cachedPlayerSummaryLike =
        overfastApiCache.get(
            OverfastApiEndpointType.PLAYERS_PLAYER_SUMMARY, playerId, PlayerSummaryLike.class);
    if (cachedPlayerSummaryLike != null) return Mono.just(cachedPlayerSummaryLike);

    return webClient
        .get()
        .uri(OverfastApiEndpointType.PLAYERS_PLAYER_SUMMARY.getUrl(playerId))
        .retrieve()
        .bodyToMono(PlayerSummary.class)
        .map(PlayerSummaryLike.class::cast)
        .doOnNext(
            playerSummaryLike -> {
              long ttlSeconds = 600;
              overfastApiCache.put(
                  OverfastApiEndpointType.PLAYERS_PLAYER_SUMMARY,
                  playerId,
                  playerSummaryLike,
                  ttlSeconds);
            })
        .onErrorResume(
            exception -> {
              if (exception instanceof WebClientResponseException webClientResponseException) {
                int statusCode = webClientResponseException.getStatusCode().value();
                String body = webClientResponseException.getResponseBodyAsString();
                String message =
                    "Unknown error has occurred on the data provider side, please try again later.";

                try {
                  JsonNode jsonNode = objectMapper.readTree(body);
                  if (jsonNode.has("error")) message = jsonNode.get("error").asText();
                  else if (jsonNode.has("detail"))
                    message = jsonNode.get("detail").toPrettyString();
                  else message = body;
                } catch (Exception ignored) {
                }

                return Mono.just(new OverfastApiError(String.valueOf(statusCode), message));
              }

              return Mono.just(new OverfastApiError("Unknown", exception.getMessage()));
            })
        .publishOn(Schedulers.boundedElastic());
  }
}
