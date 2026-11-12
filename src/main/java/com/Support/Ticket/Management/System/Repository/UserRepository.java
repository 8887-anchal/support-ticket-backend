package com.Support.Ticket.Management.System.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.Support.Ticket.Management.System.Entity.AppRole;
import com.Support.Ticket.Management.System.Entity.AppUser;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<AppUser> findAllByRoleOrderByNameAsc(AppRole role);
}