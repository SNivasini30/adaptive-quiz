package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_topic_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_topic", columnNames = {"studentName", "topic"})
})
public class StudentTopicProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String studentName;

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty currentDifficulty = Difficulty.EASY;

    private int consecutiveCorrect = 0;

    private int consecutiveIncorrect = 0;

    private int totalAttempts = 0;

    private int correctAttempts = 0;

    private LocalDateTime lastUpdated = LocalDateTime.now();

    public StudentTopicProgress() {}

    public StudentTopicProgress(String studentName, String topic) {
        this.studentName = studentName;
        this.topic = topic;
        this.currentDifficulty = Difficulty.EASY;
        this.consecutiveCorrect = 0;
        this.consecutiveIncorrect = 0;
        this.totalAttempts = 0;
        this.correctAttempts = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    public StudentTopicProgress(User user, String topic) {
        this(user != null ? user.getUsername() : "Student", topic);
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(Difficulty currentDifficulty) {
        this.currentDifficulty = currentDifficulty;
    }

    public int getConsecutiveCorrect() {
        return consecutiveCorrect;
    }

    public void setConsecutiveCorrect(int consecutiveCorrect) {
        this.consecutiveCorrect = consecutiveCorrect;
    }

    public int getConsecutiveIncorrect() {
        return consecutiveIncorrect;
    }

    public void setConsecutiveIncorrect(int consecutiveIncorrect) {
        this.consecutiveIncorrect = consecutiveIncorrect;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public int getCorrectAttempts() {
        return correctAttempts;
    }

    public void setCorrectAttempts(int correctAttempts) {
        this.correctAttempts = correctAttempts;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public double getAccuracyPercentage() {
        if (totalAttempts == 0) return 0.0;
        return ((double) correctAttempts / totalAttempts) * 100.0;
    }
}
