package com.Support.Ticket.Management.System.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

import com.Support.Ticket.Management.System.Entity.TicketDetail;

public interface TicketRepository extends JpaRepository<TicketDetail, Long>, JpaSpecificationExecutor<TicketDetail> {
    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedAgent"})
    Optional<TicketDetail> findById(Long id);
    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedAgent"})
    Page<TicketDetail> findAll(Specification<TicketDetail> specification, Pageable pageable);
    long countByStatus(com.Support.Ticket.Management.System.Entity.TicketStatus status);
    long countByCreatedById(Long userId);
    long countByCreatedByIdAndStatus(Long userId, com.Support.Ticket.Management.System.Entity.TicketStatus status);
    long countByAssignedAgentIdAndStatus(Long userId, com.Support.Ticket.Management.System.Entity.TicketStatus status);
}