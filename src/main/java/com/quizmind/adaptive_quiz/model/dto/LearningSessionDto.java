package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.LearningSession;

import java.time.LocalDateTime;

public class LearningSessionDto {
    private Long id;
    private String sessionId;
    private String sessionType;
    private String topic;
    private Long documentId;
    private int totalQuestions;
    private int completedQuestions;
    private int score;
    private int incorrectAttempts;
    private boolean completed;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime lastActiveTime;
    private int durationSeconds;

    public LearningSessionDto() {}

    public LearningSessionDto(LearningSession session) {
        if (session != null) {
            this.id = session.getId();
            this.sessionId = session.getSessionId();
            this.sessionType = session.getSessionType() != null ? session.getSessionType().name() : "QUIZ";
            this.topic = session.getTopic();
            this.documentId = session.getDocumentId();
            this.totalQuestions = session.getTotalQuestions();
            this.completedQuestions = session.getCompletedQuestions();
            this.score = session.getScore();
            this.incorrectAttempts = session.getIncorrectAttempts();
            this.completed = session.isCompleted();
            this.startTime = session.getStartTime();
            this.endTime = session.getEndTime();
            this.lastActiveTime = session.getLastActiveTime();
            this.durationSeconds = session.getDurationSeconds();
        }
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

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
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
}
