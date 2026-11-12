package com.Support.Ticket.Management.System.Service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.Support.Ticket.Management.System.Entity.AppRole;
import com.Support.Ticket.Management.System.Entity.AppUser;
import com.Support.Ticket.Management.System.Repository.UserRepository;
import com.Support.Ticket.Management.System.Security.JwtService;

@Service
public class AuthService {
	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, JwtService jwtService) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResult register(String name, String email, String password) {
		String normalizedEmail = email.trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
		}
		AppUser user = new AppUser();
		user.setName(name.trim());
		user.setEmail(normalizedEmail);
		user.setPassword(passwordEncoder.encode(password));
		user.setRole(AppRole.USER);
		user = users.save(user);
		return new AuthResult(jwtService.generateToken(user), user);
	}

	public AuthResult login(String email, String password) {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email.trim(), password));
		} catch (AuthenticationException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		AppUser user = users.findByEmailIgnoreCase(email.trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
		return new AuthResult(jwtService.generateToken(user), user);
	}

	public record AuthResult(String token, AppUser user) {}
}