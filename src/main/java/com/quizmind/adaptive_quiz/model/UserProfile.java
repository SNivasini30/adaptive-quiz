package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String institution = "General Academy";

    private String bio;

    private String avatarInitial;

    private int totalQuizzesCompleted = 0;

    private int totalPuzzlesCompleted = 0;

    private double overallAccuracy = 0.0;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserProfile() {
        this.updatedAt = LocalDateTime.now();
    }

    public UserProfile(User user) {
        this();
        this.user = user;
        if (user != null && user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            this.avatarInitial = user.getFullName().trim().substring(0, 1).toUpperCase();
        } else if (user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            this.avatarInitial = user.getUsername().trim().substring(0, 1).toUpperCase();
        } else {
            this.avatarInitial = "U";
        }
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
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarInitial() {
        return avatarInitial;
    }

    public void setAvatarInitial(String avatarInitial) {
        this.avatarInitial = avatarInitial;
    }

    public int getTotalQuizzesCompleted() {
        return totalQuizzesCompleted;
    }

    public void setTotalQuizzesCompleted(int totalQuizzesCompleted) {
        this.totalQuizzesCompleted = totalQuizzesCompleted;
    }

    public int getTotalPuzzlesCompleted() {
        return totalPuzzlesCompleted;
    }

    public void setTotalPuzzlesCompleted(int totalPuzzlesCompleted) {
        this.totalPuzzlesCompleted = totalPuzzlesCompleted;
    }

    public double getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(double overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
