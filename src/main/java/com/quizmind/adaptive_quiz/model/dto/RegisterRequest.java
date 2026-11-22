package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.UserRole;

public class RegisterRequest {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private UserRole role = UserRole.STUDENT;
    private String teacherPasscode;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String fullName, String email, UserRole role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role != null ? role : UserRole.STUDENT;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getTeacherPasscode() {
        return teacherPasscode;
    }

    public void setTeacherPasscode(String teacherPasscode) {
        this.teacherPasscode = teacherPasscode;
    }
}
