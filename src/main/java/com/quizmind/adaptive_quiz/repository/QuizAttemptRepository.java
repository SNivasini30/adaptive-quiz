package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.QuizAttempt;
import com.quizmind.adaptive_quiz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findAllByOrderByAttemptTimestampDesc();

    List<QuizAttempt> findByCorrectFalseOrderByAttemptTimestampDesc();

    long countByCorrect(boolean correct);

    long countByStudentName(String studentName);

    long countByStudentNameAndCorrect(String studentName, boolean correct);

    @Query("SELECT AVG(q.responseTimeSec) FROM QuizAttempt q")
    Double getAverageResponseTime();

    @Query("SELECT DISTINCT q.topic FROM QuizAttempt q")
    List<String> findDistinctAttemptedTopics();

    List<QuizAttempt> findTop10ByOrderByAttemptTimestampDesc();

    List<QuizAttempt> findTop20ByOrderByAttemptTimestampDesc();

    List<QuizAttempt> findByStudentNameOrderByAttemptTimestampDesc(String studentName);

    List<QuizAttempt> findBySessionId(String sessionId);

    // User-specific queries
    List<QuizAttempt> findByUserOrderByAttemptTimestampDesc(User user);

    List<QuizAttempt> findByUserIdOrderByAttemptTimestampDesc(Long userId);

    List<QuizAttempt> findByUserAndCorrectFalseOrderByAttemptTimestampDesc(User user);

    List<QuizAttempt> findByUserIdAndCorrectFalseOrderByAttemptTimestampDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndCorrect(Long userId, boolean correct);

    @Query("SELECT AVG(q.responseTimeSec) FROM QuizAttempt q WHERE q.user.id = :userId")
    Double getAverageResponseTimeByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT q.topic FROM QuizAttempt q WHERE q.user.id = :userId")
    List<String> findDistinctAttemptedTopicsByUserId(@Param("userId") Long userId);
}
