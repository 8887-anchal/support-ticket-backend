package com.Support.Ticket.Management.System.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.Support.Ticket.Management.System.Entity.TicketComment;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    @EntityGraph(attributePaths = "author")
    List<TicketComment> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}