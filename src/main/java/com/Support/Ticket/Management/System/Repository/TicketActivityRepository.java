package com.Support.Ticket.Management.System.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.Support.Ticket.Management.System.Entity.TicketActivity;

public interface TicketActivityRepository extends JpaRepository<TicketActivity, Long> {
    @EntityGraph(attributePaths = "actor")
    List<TicketActivity> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}