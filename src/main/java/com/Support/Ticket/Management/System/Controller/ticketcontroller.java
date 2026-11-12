package com.Support.Ticket.Management.System.Controller;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.Support.Ticket.Management.System.Entity.AppUser;
import com.Support.Ticket.Management.System.Entity.TicketActivity;
import com.Support.Ticket.Management.System.Entity.TicketComment;
import com.Support.Ticket.Management.System.Entity.TicketDetail;
import com.Support.Ticket.Management.System.Entity.TicketPriority;
import com.Support.Ticket.Management.System.Entity.TicketStatus;
import com.Support.Ticket.Management.System.Service.ticketservice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
public class ticketcontroller {
	private final ticketservice ticketService;
	public ticketcontroller(ticketservice ticketService) { this.ticketService = ticketService; }

	@GetMapping("/tickets")
	public TicketPage list(@AuthenticationPrincipal AppUser actor,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String priority,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Page<TicketDetail> result = ticketService.list(actor, q, status, priority, category, page, size);
		return new TicketPage(result.getContent().stream().map(this::ticketResponse).toList(), result.getNumber(),
				result.getSize(), result.getTotalElements(), result.getTotalPages());
	}

	@PostMapping("/tickets")
	@ResponseStatus(HttpStatus.CREATED)
	public TicketResponse create(@Valid @RequestBody TicketRequest request, @AuthenticationPrincipal AppUser actor) {
		return ticketResponse(ticketService.create(request.title(), request.description(), request.category(), request.priority(), actor));
	}

	@GetMapping("/tickets/{id}")
	public TicketResponse get(@PathVariable Long id, @AuthenticationPrincipal AppUser actor) {
		return ticketResponse(ticketService.get(id, actor));
	}

	@PutMapping("/tickets/{id}")
	public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketRequest request,
			@AuthenticationPrincipal AppUser actor) {
		return ticketResponse(ticketService.update(id, request.title(), request.description(), request.category(),
				request.priority(), request.status(), actor));
	}

	@DeleteMapping("/tickets/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id, @AuthenticationPrincipal AppUser actor) { ticketService.delete(id, actor); }

	@PutMapping("/tickets/{id}/assignment")
	public TicketResponse assign(@PathVariable Long id, @RequestBody AssignmentRequest request,
			@AuthenticationPrincipal AppUser actor) {
		return ticketResponse(ticketService.assign(id, request.agentId(), actor));
	}

	@GetMapping("/tickets/{id}/comments")
	public List<CommentResponse> comments(@PathVariable Long id, @AuthenticationPrincipal AppUser actor) {
		return ticketService.getComments(id, actor).stream().map(this::commentResponse).toList();
	}

	@PostMapping("/tickets/{id}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
			@AuthenticationPrincipal AppUser actor) {
		return commentResponse(ticketService.addComment(id, request.body(), actor));
	}

	@GetMapping("/tickets/{id}/activity")
	public List<ActivityResponse> activity(@PathVariable Long id, @AuthenticationPrincipal AppUser actor) {
		return ticketService.getActivity(id, actor).stream().map(this::activityResponse).toList();
	}

	@GetMapping("/dashboard")
	public ticketservice.Dashboard dashboard(@AuthenticationPrincipal AppUser actor) { return ticketService.dashboard(actor); }

	@GetMapping("/agents")
	public List<UserResponse> agents() {
		return ticketService.agents().stream().map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail())).toList();
	}

	private TicketResponse ticketResponse(TicketDetail ticket) {
		return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(), ticket.getCategory(),
				ticket.getPriority(), ticket.getStatus(), userResponse(ticket.getCreatedBy()),
				ticket.getAssignedAgent() == null ? null : userResponse(ticket.getAssignedAgent()),
				ticket.getCreatedAt(), ticket.getUpdatedAt());
	}

	private UserResponse userResponse(AppUser user) { return new UserResponse(user.getId(), user.getName(), user.getEmail()); }
	private CommentResponse commentResponse(TicketComment comment) {
		return new CommentResponse(comment.getId(), comment.getBody(), userResponse(comment.getAuthor()), comment.getCreatedAt());
	}
	private ActivityResponse activityResponse(TicketActivity activity) {
		return new ActivityResponse(activity.getId(), activity.getType(), activity.getDetail(),
				userResponse(activity.getActor()), activity.getCreatedAt());
	}

	public record TicketRequest(@NotBlank @Size(max = 160) String title,
			@NotBlank @Size(max = 10000) String description,
			@NotBlank @Size(max = 80) String category,
			@NotNull TicketPriority priority, TicketStatus status) {}
	public record AssignmentRequest(Long agentId) {}
	public record CommentRequest(@NotBlank @Size(max = 5000) String body) {}
	public record UserResponse(Long id, String name, String email) {}
	public record TicketResponse(Long id, String title, String description, String category, TicketPriority priority,
			TicketStatus status, UserResponse createdBy, UserResponse assignedAgent, Instant createdAt, Instant updatedAt) {}
	public record CommentResponse(Long id, String body, UserResponse author, Instant createdAt) {}
	public record ActivityResponse(Long id, String type, String detail, UserResponse actor, Instant createdAt) {}
	public record TicketPage(List<TicketResponse> content, int page, int size, long totalElements, int totalPages) {}
}
