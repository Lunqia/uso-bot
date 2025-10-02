package dev.lunqia.usobot.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TicketService {
  private final TicketRepository ticketRepository;

  public Mono<Ticket> createTicket(Ticket.TicketBuilder ticketBuilder) {
    // TODO: Complete the ticket creation process (e.g., create a channel, set permissions, etc.)
    return ticketRepository.save(ticketBuilder.build());
  }
}
