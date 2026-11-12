package com.Support.Ticket.Management.System.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.Support.Ticket.Management.System.Entity.AppUser;
import com.Support.Ticket.Management.System.Service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class AuthController {
	private final AuthService authService;
	public AuthController(AuthService authService) { this.authService = authService; }

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return response(authService.register(request.name(), request.email(), request.password()));
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return response(authService.login(request.email(), request.password()));
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AppUser user) { return userResponse(user); }

	private AuthResponse response(AuthService.AuthResult result) {
		return new AuthResponse(result.token(), userResponse(result.user()));
	}

	private UserResponse userResponse(AppUser user) {
		if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name());
	}

	public record RegisterRequest(@NotBlank @Size(max = 100) String name,
			@NotBlank @Email @Size(max = 190) String email,
			@NotBlank @Size(min = 8, max = 72) String password) {}
	public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
	public record UserResponse(Long id, String name, String email, String role) {}
	public record AuthResponse(String token, UserResponse user) {}
}