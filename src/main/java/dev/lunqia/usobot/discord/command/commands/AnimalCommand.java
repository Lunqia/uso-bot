package dev.lunqia.usobot.discord.command.commands;

import dev.lunqia.usobot.animal.AnimalApiService;
import dev.lunqia.usobot.animal.AnimalType;
import dev.lunqia.usobot.discord.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionChoiceData;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.Color;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AnimalCommand implements SlashCommand {

  private static final String OPTION_ANIMAL = "animal";
  private final AnimalApiService animalApiService;

  @Override
  public String name() {
    return OPTION_ANIMAL;
  }

  @Override
  public String description() {
    return "Send a random animal image and fact.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return List.of(
        ApplicationCommandOptionData.builder()
            .name(OPTION_ANIMAL)
            .description("Animal type, e.g. cat, dog, capybara")
            .type(ApplicationCommandOption.Type.STRING.getValue())
            .required(true)
            .choices(
                Arrays.stream(AnimalType.values())
                    .map(
                        animalType ->
                            ImmutableApplicationCommandOptionChoiceData.builder()
                                .name(animalType.getFriendlyName())
                                .value(animalType.getApiName())
                                .build())
                    .map(ApplicationCommandOptionChoiceData.class::cast)
                    .toList())
            .build());
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    Optional<String> animalOption =
        event
            .getOption(OPTION_ANIMAL)
            .flatMap(
                option -> option.getValue().map(ApplicationCommandInteractionOptionValue::asString))
            .map(String::toLowerCase);

    if (animalOption.isEmpty()) {
      return event
          .editReply()
          .withContent(Possible.of(Optional.of("Please specify an animal type.")))
          .then();
    }

    AnimalType animalType;
    try {
      animalType = AnimalType.fromApiName(animalOption.get());
    } catch (IllegalArgumentException exception) {
      return event.editReply().withContent(Possible.of(Optional.of("Invalid animal type."))).then();
    }

    return animalApiService
        .getRandomAnimalImageAndFact(animalType)
        .flatMap(
            response -> {
              EmbedCreateSpec embedCreateSpec =
                  EmbedCreateSpec.builder()
                      .title(animalType.getFriendlyName())
                      .image(response.image())
                      .description(response.fact())
                      .color(Color.of(0xFF9900))
                      .build();
              return event
                  .editReply()
                  .withEmbeds(Possible.of(Optional.of(List.of(embedCreateSpec))));
            })
        .switchIfEmpty(
            event
                .editReply()
                .withContent(Possible.of(Optional.of("No data found for this animal."))))
        .onErrorResume(
            exception ->
                event
                    .editReply()
                    .withContent(Possible.of(Optional.of("Failed to fetch animal data."))))
        .then();
  }
}
