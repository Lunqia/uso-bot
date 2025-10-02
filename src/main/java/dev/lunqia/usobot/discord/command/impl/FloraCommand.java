package dev.lunqia.usobot.discord.command.impl;

import dev.lunqia.usobot.discord.command.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FloraCommand implements SlashCommand {
  private static final Random RANDOM = new Random();

  private static final Set<Long> ALLOWED_USER_IDS =
      Set.of(1059922516317052978L, 199228071079968768L);

  private static final String[] AFFIRMATIONS = {
    "You’re a legendary trans queen, serving looks since day one 👑🌈",
    "The gender euphoria is radiating off you ✨💖",
    "Your trans power level is over 9000 ⚡🏳️‍⚧️",
    "You’re literally main character energy 💅"
  };

  private static final String[] EMOJI_EXPLOSIONS = {
    "🏳️‍⚧️🏳️‍⚧️🏳️‍⚧️💖✨", "🏳️‍🌈🌟🌈🔥", "💖💛💜🏳️‍🌈🏳️‍⚧️", "🌸🌈✨🏳️‍⚧️🌟"
  };

  private static final String[] BUFFS = {
    "+10 Charisma because you’re THAT girl 💅",
    "+50 Euphoria, -100 Dysphoria 🏳️‍⚧️✨",
    "Critical hit on gender roles, they fainted 💥",
    "Unlocked passive: Infinite Slay Mode 🌈"
  };

  private static final String[] ANIMAL_FACTS = {
    "🐧 Did you know many penguins form same-sex couples?",
    "🐠 Clownfish can change sex — Nemo could’ve been trans!",
    "🦢 Swans are super gay, lots of pairs are same-sex.",
    "🦎 Some reptiles can literally change gender in response to their environment!"
  };

  private static final String[] HOROSCOPES = {
    "🌙 Stars say: today you slay, tomorrow you slay, forever you slay ✨",
    "☀️ Horoscope: Gender euphoria at maximum levels!",
    "🌈 Destiny reveals: all dysphoria debuffs will miss you today.",
    "💫 Cosmic vibes: you’re glowing trans joy into the galaxy."
  };

  @Override
  public String name() {
    return "flora";
  }

  @Override
  public String description() {
    return "A gay command.";
  }

  @Override
  public List<ApplicationCommandOptionData> options() {
    return Collections.emptyList();
  }

  @Override
  public Mono<Void> handle(ChatInputInteractionEvent event) {
    if (!ALLOWED_USER_IDS.contains(event.getInteraction().getUser().getId().asLong()))
      return event
          .createFollowup("❌ You don't look like Flora to me...")
          .withEphemeral(true)
          .then();

    int choice = RANDOM.nextInt(5);
    String response;

    switch (choice) {
      case 0 -> response = AFFIRMATIONS[RANDOM.nextInt(AFFIRMATIONS.length)];
      case 1 -> response = EMOJI_EXPLOSIONS[RANDOM.nextInt(EMOJI_EXPLOSIONS.length)];
      case 2 -> response = BUFFS[RANDOM.nextInt(BUFFS.length)];
      case 3 -> response = ANIMAL_FACTS[RANDOM.nextInt(ANIMAL_FACTS.length)];
      case 4 -> response = HOROSCOPES[RANDOM.nextInt(HOROSCOPES.length)];
      default -> response = "✨ Gay chaos energy unleashed ✨";
    }

    return event.createFollowup(response).then();
  }
}
