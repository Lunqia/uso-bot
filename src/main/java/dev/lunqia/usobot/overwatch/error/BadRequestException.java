package dev.lunqia.usobot.overwatch.error;

public class BadRequestException extends OwapiException {
  public BadRequestException(String message) {
    super(message);
  }
}
