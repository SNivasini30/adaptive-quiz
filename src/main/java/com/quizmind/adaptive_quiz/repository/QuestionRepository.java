package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.ApprovalStatus;
import com.quizmind.adaptive_quiz.model.Difficulty;
import com.quizmind.adaptive_quiz.model.GeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<GeneratedQuestion, Long> {

    List<GeneratedQuestion> findByApprovalStatus(ApprovalStatus status);

    List<GeneratedQuestion> findAllByOrderByIdDesc();

    List<GeneratedQuestion> findByApprovalStatusOrderByIdDesc(ApprovalStatus status);

    List<GeneratedQuestion> findByApprovalStatusAndDifficulty(ApprovalStatus status, Difficulty difficulty);

    List<GeneratedQuestion> findByApprovalStatusAndTopic(ApprovalStatus status, String topic);

    List<GeneratedQuestion> findByApprovalStatusAndTopicAndDifficulty(ApprovalStatus status, String topic, Difficulty difficulty);

    @Query("SELECT DISTINCT q.topic FROM GeneratedQuestion q WHERE q.approvalStatus = 'APPROVED'")
    List<String> findDistinctApprovedTopics();

    @Query("SELECT DISTINCT q.topic FROM GeneratedQuestion q")
    List<String> findAllDistinctTopics();

    long countByApprovalStatus(ApprovalStatus status);

    List<GeneratedQuestion> findByDocumentId(Long documentId);

    List<GeneratedQuestion> findByDocumentIdAndApprovalStatus(Long documentId, ApprovalStatus status);
}
