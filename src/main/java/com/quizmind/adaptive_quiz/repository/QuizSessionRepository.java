package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.QuizSession;
import com.quizmind.adaptive_quiz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {
    Optional<QuizSession> findBySessionId(String sessionId);
    List<QuizSession> findByUserOrderByStartTimeDesc(User user);
    List<QuizSession> findByUserIdOrderByStartTimeDesc(Long userId);
    List<QuizSession> findByUserAndCompletedFalse(User user);
    List<QuizSession> findByUserIdAndCompletedFalse(Long userId);
}
