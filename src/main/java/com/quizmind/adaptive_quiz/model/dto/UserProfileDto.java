package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.AccountStatus;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserProfile;
import com.quizmind.adaptive_quiz.model.UserRole;

import java.time.LocalDateTime;

public class UserProfileDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private UserRole role;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String bio;
    private String institution;
    private String avatarInitial;

    public UserProfileDto() {}

    public UserProfileDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
            this.role = user.getRole();
            this.status = user.getStatus();
            this.createdAt = user.getCreatedAt();
            this.lastLoginAt = user.getLastLoginAt();
            if (this.fullName != null && !this.fullName.trim().isEmpty()) {
                this.avatarInitial = this.fullName.trim().substring(0, 1).toUpperCase();
            } else if (this.username != null && !this.username.trim().isEmpty()) {
                this.avatarInitial = this.username.trim().substring(0, 1).toUpperCase();
            } else {
                this.avatarInitial = "U";
            }
        }
    }

    public UserProfileDto(User user, UserProfile profile) {
        this(user);
        if (profile != null) {
            this.bio = profile.getBio();
            this.institution = profile.getInstitution();
            if (profile.getAvatarInitial() != null) {
                this.avatarInitial = profile.getAvatarInitial();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getAvatarInitial() {
        return avatarInitial;
    }

    public void setAvatarInitial(String avatarInitial) {
        this.avatarInitial = avatarInitial;
    }
}
