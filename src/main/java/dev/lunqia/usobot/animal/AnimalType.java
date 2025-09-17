package dev.lunqia.usobot.animal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.WordUtils;

@RequiredArgsConstructor
@Getter
public enum AnimalType {
  CAT("cat"),
  DOG("dog"),
  BIRD("bird"),
  PANDA("panda"),
  RED_PANDA("redpanda"),
  KOALA("koala"),
  FOX("fox"),
  WHALE("whale"),
  DOLPHIN("dolphin"),
  KANGAROO("kangaroo"),
  RABBIT("rabbit"),
  LION("lion"),
  BEAR("bear"),
  FROG("frog"),
  DUCK("duck"),
  PENGUIN("penguin"),
  AXOLOTL("axolotl"),
  CAPYBARA("capybara"),
  HEDGEHOG("hedgehog"),
  TURTLE("turtle"),
  NARWHAL("narwhal"),
  SQUIRREL("squirrel"),
  FISH("fish"),
  HORSE("horse");

  private final String apiName;

  public static AnimalType fromApiName(String apiName) {
    for (AnimalType type : values()) {
      if (type.getApiName().equalsIgnoreCase(apiName)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid animal type: " + apiName);
  }

  public String getFriendlyName() {
    return WordUtils.capitalizeFully(name().replace("_", " "));
  }
}
