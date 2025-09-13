package dev.lunqia.usobot.overwatch.model;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PlatformType {
  PC,
  PSN,
  XBL;

  private final String apiName = name().toLowerCase();

  public static Optional<PlatformType> fromString(String text) {
    for (PlatformType platformType : PlatformType.values()) {
      if (platformType.name().equalsIgnoreCase(text)) return Optional.of(platformType);
    }
    return Optional.empty();
  }

  public static boolean isValidPlatform(String text) {
    return fromString(text).isPresent();
  }
}
