package com.Support.Ticket.Management.System.Service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.Support.Ticket.Management.System.Entity.AppRole;
import com.Support.Ticket.Management.System.Entity.AppUser;
import com.Support.Ticket.Management.System.Entity.TicketActivity;
import com.Support.Ticket.Management.System.Entity.TicketComment;
import com.Support.Ticket.Management.System.Entity.TicketDetail;
import com.Support.Ticket.Management.System.Entity.TicketPriority;
import com.Support.Ticket.Management.System.Entity.TicketStatus;
import com.Support.Ticket.Management.System.Repository.TicketActivityRepository;
import com.Support.Ticket.Management.System.Repository.TicketCommentRepository;
import com.Support.Ticket.Management.System.Repository.TicketRepository;
import com.Support.Ticket.Management.System.Repository.UserRepository;

@Service
public class ticketservice {
	private final TicketRepository tickets;
	private final UserRepository users;
	private final TicketCommentRepository comments;
	private final TicketActivityRepository activities;

	public ticketservice(TicketRepository tickets, UserRepository users, TicketCommentRepository comments,
			TicketActivityRepository activities) {
		this.tickets = tickets;
		this.users = users;
		this.comments = comments;
		this.activities = activities;
	}

	public Page<TicketDetail> list(AppUser actor, String query, String status, String priority, String category,
			int page, int size) {
		int safeSize = Math.clamp(size, 1, 100);
		Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
		Specification<TicketDetail> spec = (root, criteria, builder) -> {
			var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
			if (actor.getRole() == AppRole.USER) {
				predicates.add(builder.equal(root.get("createdBy").get("id"), actor.getId()));
			}
			if (query != null && !query.isBlank()) {
				String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(builder.or(builder.like(builder.lower(root.get("title")), pattern),
						builder.like(builder.lower(root.get("description")), pattern)));
			}
			if (status != null && !status.isBlank()) predicates.add(builder.equal(root.get("status"), enumValue(TicketStatus.class, status)));
			if (priority != null && !priority.isBlank()) predicates.add(builder.equal(root.get("priority"), enumValue(TicketPriority.class, priority)));
			if (category != null && !category.isBlank()) predicates.add(builder.equal(builder.lower(root.get("category")), category.toLowerCase(Locale.ROOT)));
			return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
		return tickets.findAll(spec, pageable);
	}

	public TicketDetail get(Long id, AppUser actor) {
		TicketDetail ticket = tickets.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
		ensureVisible(ticket, actor);
		return ticket;
	}

	@Transactional
	public TicketDetail create(String title, String description, String category, TicketPriority priority, AppUser actor) {
		TicketDetail ticket = new TicketDetail();
		ticket.setTitle(title.trim());
		ticket.setDescription(description.trim());
		ticket.setCategory(category.trim());
		ticket.setPriority(priority == null ? TicketPriority.MEDIUM : priority);
		ticket.setStatus(TicketStatus.OPEN);
		ticket.setCreatedBy(actor);
		ticket = tickets.save(ticket);
		log(ticket, actor, "CREATED", "Created this ticket");
		return ticket;
	}

	@Transactional
	public TicketDetail update(Long id, String title, String description, String category, TicketPriority priority,
			TicketStatus status, AppUser actor) {
		TicketDetail ticket = get(id, actor);
		if (actor.getRole() != AppRole.AGENT && !ticket.getCreatedBy().getId().equals(actor.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		if (actor.getRole() == AppRole.USER && status != null && status != ticket.getStatus()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only agents can change ticket status");
		}
		ticket.setTitle(title.trim());
		ticket.setDescription(description.trim());
		ticket.setCategory(category.trim());
		if (priority != null) ticket.setPriority(priority);
		if (status != null && status != ticket.getStatus()) {
			String previous = ticket.getStatus().name();
			ticket.setStatus(status);
			log(ticket, actor, "STATUS_CHANGED", "Changed status from " + previous + " to " + status.name());
		}
		return tickets.save(ticket);
	}

	@Transactional
	public void delete(Long id, AppUser actor) {
		TicketDetail ticket = get(id, actor);
		if (actor.getRole() != AppRole.AGENT && (!ticket.getCreatedBy().getId().equals(actor.getId()) || ticket.getStatus() != TicketStatus.OPEN)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only agents or the ticket owner of an open ticket can delete it");
		}
		activities.deleteAll(activities.findAllByTicketIdOrderByCreatedAtAsc(id));
		comments.deleteAll(comments.findAllByTicketIdOrderByCreatedAtAsc(id));
		tickets.delete(ticket);
	}

	@Transactional
	public TicketDetail assign(Long id, Long agentId, AppUser actor) {
		requireAgent(actor);
		TicketDetail ticket = get(id, actor);
		AppUser assignee = agentId == null ? null : users.findById(agentId)
				.filter(user -> user.getRole() == AppRole.AGENT)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must be an agent"));
		ticket.setAssignedAgent(assignee);
		log(ticket, actor, "ASSIGNED", assignee == null ? "Removed ticket assignment" : "Assigned to " + assignee.getName());
		return tickets.save(ticket);
	}

	@Transactional
	public TicketComment addComment(Long id, String body, AppUser actor) {
		TicketDetail ticket = get(id, actor);
		TicketComment comment = new TicketComment();
		comment.setTicket(ticket);
		comment.setAuthor(actor);
		comment.setBody(body.trim());
		comment = comments.save(comment);
		return comment;
	}

	public List<TicketComment> getComments(Long id, AppUser actor) {
		get(id, actor);
		return comments.findAllByTicketIdOrderByCreatedAtAsc(id);
	}

	public List<TicketActivity> getActivity(Long id, AppUser actor) {
		get(id, actor);
		return activities.findAllByTicketIdOrderByCreatedAtAsc(id);
	}

	public List<AppUser> agents() { return users.findAllByRoleOrderByNameAsc(AppRole.AGENT); }

	public Dashboard dashboard(AppUser actor) {
		if (actor.getRole() == AppRole.AGENT) {
		    return new Dashboard(tickets.count(), tickets.countByStatus(TicketStatus.OPEN),
			    tickets.countByStatus(TicketStatus.IN_PROGRESS), tickets.countByStatus(TicketStatus.RESOLVED));
		}
		return new Dashboard(tickets.countByCreatedById(actor.getId()),
				tickets.countByCreatedByIdAndStatus(actor.getId(), TicketStatus.OPEN),
				tickets.countByCreatedByIdAndStatus(actor.getId(), TicketStatus.IN_PROGRESS),
				tickets.countByCreatedByIdAndStatus(actor.getId(), TicketStatus.RESOLVED));
	}

	private void ensureVisible(TicketDetail ticket, AppUser actor) {
		if (actor.getRole() == AppRole.USER && !ticket.getCreatedBy().getId().equals(actor.getId())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
		}
	}

	private void requireAgent(AppUser actor) {
		if (actor.getRole() != AppRole.AGENT) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
	}

	private void log(TicketDetail ticket, AppUser actor, String type, String detail) {
		TicketActivity activity = new TicketActivity();
		activity.setTicket(ticket);
		activity.setActor(actor);
		activity.setType(type);
		activity.setDetail(detail);
		activities.save(activity);
	}

	private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
		try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + type.getSimpleName()); }
	}

	public record Dashboard(long total, long open, long inProgress, long resolved) {}
}
