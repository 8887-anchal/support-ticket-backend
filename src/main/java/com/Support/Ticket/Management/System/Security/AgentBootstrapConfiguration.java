package com.Support.Ticket.Management.System.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.Support.Ticket.Management.System.Entity.AppRole;
import com.Support.Ticket.Management.System.Entity.AppUser;
import com.Support.Ticket.Management.System.Repository.UserRepository;

@Configuration
public class AgentBootstrapConfiguration {
    @Bean
    ApplicationRunner bootstrapAgent(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-agent.email:}") String email,
            @Value("${app.bootstrap-agent.password:}") String password,
            @Value("${app.bootstrap-agent.name:}") String name) {
        return args -> {
            if (email.isBlank() || password.isBlank()) return;
            if (password.length() < 8) throw new IllegalStateException("Bootstrap agent password must have at least 8 characters");
            if (users.existsByEmailIgnoreCase(email)) return;
            AppUser agent = new AppUser();
            agent.setName(name.isBlank() ? email.substring(0, email.indexOf('@')) : name.trim());
            agent.setEmail(email.trim());
            agent.setPassword(passwordEncoder.encode(password));
            agent.setRole(AppRole.AGENT);
            users.save(agent);
        };
    }
}