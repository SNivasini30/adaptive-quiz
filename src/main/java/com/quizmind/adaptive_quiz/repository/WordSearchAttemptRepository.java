package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.WordSearchAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordSearchAttemptRepository extends JpaRepository<WordSearchAttempt, Long> {
    Optional<WordSearchAttempt> findBySessionToken(String sessionToken);
    List<WordSearchAttempt> findByStudentNameOrderByStartTimeDesc(String studentName);
    List<WordSearchAttempt> findByUserOrderByStartTimeDesc(User user);
    List<WordSearchAttempt> findByUserIdOrderByStartTimeDesc(Long userId);
    List<WordSearchAttempt> findByUserAndCompletedFalse(User user);
    List<WordSearchAttempt> findByUserIdAndCompletedFalse(Long userId);
}
