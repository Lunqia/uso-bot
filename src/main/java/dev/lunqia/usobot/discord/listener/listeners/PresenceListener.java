package dev.lunqia.usobot.discord.listener.listeners;

import dev.lunqia.usobot.discord.listener.EventListener;
import discord4j.core.event.domain.lifecycle.ConnectEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class PresenceListener implements EventListener<ConnectEvent> {
  @Override
  public Class<ConnectEvent> getEventType() {
    return ConnectEvent.class;
  }

  @Override
  public Mono<Void> execute(ConnectEvent event) {
    log.info("Setting bot presence");
    return event
        .getClient()
        .updatePresence(ClientPresence.online(ClientActivity.custom("Pocketing you")))
        .then();
  }
}
