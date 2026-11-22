package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.StudentTopicProgress;
import com.quizmind.adaptive_quiz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentTopicProgressRepository extends JpaRepository<StudentTopicProgress, Long> {

    Optional<StudentTopicProgress> findByStudentNameAndTopic(String studentName, String topic);

    List<StudentTopicProgress> findByStudentName(String studentName);

    Optional<StudentTopicProgress> findByUserAndTopic(User user, String topic);

    Optional<StudentTopicProgress> findByUserIdAndTopic(Long userId, String topic);

    List<StudentTopicProgress> findByUser(User user);

    List<StudentTopicProgress> findByUserId(Long userId);
}
