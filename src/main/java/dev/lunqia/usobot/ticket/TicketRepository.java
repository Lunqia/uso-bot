package dev.lunqia.usobot.ticket;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends ReactiveMongoRepository<Ticket, String> {}
