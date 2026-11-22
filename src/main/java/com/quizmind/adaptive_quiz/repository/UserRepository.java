package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    List<User> findByRole(UserRole role);
}
