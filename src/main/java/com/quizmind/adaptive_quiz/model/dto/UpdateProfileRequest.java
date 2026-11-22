package com.quizmind.adaptive_quiz.model.dto;

public class UpdateProfileRequest {
    private String fullName;
    private String email;
    private String bio;
    private String institution;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String fullName, String email, String bio, String institution) {
        this.fullName = fullName;
        this.email = email;
        this.bio = bio;
        this.institution = institution;
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
}
