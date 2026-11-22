package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_sessions")
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String studentName;

    private String topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private PdfDocument document;

    private int totalAttempted = 0;

    private int correctCount = 0;

    private int totalScore = 0;

    private boolean completed = false;

    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime endTime;

    public QuizSession() {
        this.startTime = LocalDateTime.now();
    }

    public QuizSession(String sessionId, String studentName, String topic, PdfDocument document) {
        this();
        this.sessionId = sessionId;
        this.studentName = studentName != null ? studentName : "Student";
        this.topic = topic != null ? topic : "General";
        this.document = document;
    }

    public QuizSession(String sessionId, User user, String topic, PdfDocument document) {
        this(sessionId, user != null ? user.getUsername() : "Student", topic, document);
        this.user = user;
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
        if (user != null && (this.studentName == null || this.studentName.isEmpty())) {
            this.studentName = user.getUsername();
        }
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public PdfDocument getDocument() {
        return document;
    }

    public void setDocument(PdfDocument document) {
        this.document = document;
    }

    public int getTotalAttempted() {
        return totalAttempted;
    }

    public void setTotalAttempted(int totalAttempted) {
        this.totalAttempted = totalAttempted;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
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
}
