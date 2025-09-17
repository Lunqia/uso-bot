package dev.lunqia.usobot.overwatch2;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OverfastApiEndpointType {
  // Heroes
  HEROES_LIST_OF_HEROES("heroes"),
  HEROES_HERO_STATISTICS("heroes/stats"),
  HEROES_HERO_DATA("heroes/%s"),
  HEROES_LIST_OF_ROLES("roles"),

  // Game modes
  GAME_MODES_LIST_OF_GAME_MODES("gamemodes"),

  // Maps
  MAPS_LIST_OF_MAPS("maps"),

  // Players
  PLAYERS_SPECIFIC_PLAYER("players"),
  PLAYERS_PLAYER_SUMMARY("players/%s/summary"),
  PLAYERS_PLAYER_STATS_SUMMARY("players/%s/stats/summary"),
  PLAYERS_PLAYER_CAREER_STATS("players/%s/stats/career"),
  PLAYERS_PLAYER_STATS_WITH_LABELS("players/%s/stats"),
  PLAYERS_ALL_PLAYER_DATA("players/%s");

  public static final String BASE_URL = "https://overfast-api.tekrop.fr/";

  private final String path;

  public String getUrl() {
    return BASE_URL + path;
  }

  public String getUrl(Object... args) {
    return BASE_URL + String.format(path, args);
  }
}
