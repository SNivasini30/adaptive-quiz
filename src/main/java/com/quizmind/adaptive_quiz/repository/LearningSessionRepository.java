package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.LearningSession;
import com.quizmind.adaptive_quiz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {
    Optional<LearningSession> findBySessionId(String sessionId);
    List<LearningSession> findByUserAndCompletedFalseOrderByLastActiveTimeDesc(User user);
    List<LearningSession> findByUserIdAndCompletedFalseOrderByLastActiveTimeDesc(Long userId);
    List<LearningSession> findByUserOrderByStartTimeDesc(User user);
    List<LearningSession> findByUserIdOrderByStartTimeDesc(Long userId);
}
