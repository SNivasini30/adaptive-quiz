package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_sessions")
public class LearningSession {

    public enum SessionType {
        QUIZ,
        WORD_SEARCH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    private String topic;

    private Long documentId;

    private int totalQuestions = 0;

    private int completedQuestions = 0;

    private int score = 0;

    private int incorrectAttempts = 0;

    private boolean completed = false;

    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime endTime;

    private LocalDateTime lastActiveTime = LocalDateTime.now();

    private int durationSeconds = 0;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String stateSnapshot; // Serialized state for resumption

    public LearningSession() {
        this.startTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    public LearningSession(String sessionId, User user, SessionType sessionType, String topic, Long documentId) {
        this();
        this.sessionId = sessionId;
        this.user = user;
        this.sessionType = sessionType;
        this.topic = topic;
        this.documentId = documentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCompletedQuestions() {
        return completedQuestions;
    }

    public void setCompletedQuestions(int completedQuestions) {
        this.completedQuestions = completedQuestions;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getIncorrectAttempts() {
        return incorrectAttempts;
    }

    public void setIncorrectAttempts(int incorrectAttempts) {
        this.incorrectAttempts = incorrectAttempts;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getStateSnapshot() {
        return stateSnapshot;
    }

    public void setStateSnapshot(String stateSnapshot) {
        this.stateSnapshot = stateSnapshot;
    }
}
