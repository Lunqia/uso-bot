package dev.lunqia.usobot.overwatch;

import dev.lunqia.usobot.overwatch.model.OwCompleteStats;
import dev.lunqia.usobot.overwatch.model.OwProfile;
import reactor.core.publisher.Mono;

public interface OverwatchPlayerClient {
  Mono<OwProfile> getProfile(String platform, String battleTag);

  Mono<OwCompleteStats> getCompleteStats(String platform, String battleTag);
}
