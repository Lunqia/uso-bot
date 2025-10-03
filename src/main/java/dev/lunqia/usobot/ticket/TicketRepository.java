package dev.lunqia.usobot.ticket;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TicketRepository extends ReactiveMongoRepository<Ticket, String> {
  Mono<Boolean> existsByUserIdAndGuildIdAndStatus(Long userId, Long guildId, Ticket.Status status);
}
