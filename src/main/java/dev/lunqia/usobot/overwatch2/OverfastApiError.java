package dev.lunqia.usobot.overwatch2;

import dev.lunqia.usobot.overwatch2.player.PlayerLike;

public record OverfastApiError(String type, String message) implements PlayerLike {}
