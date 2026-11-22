package com.quizmind.adaptive_quiz.model.dto;

public class TopicPerformanceDto {
    private String topic;
    private int totalQuestions;
    private int correctQuestions;
    private double accuracyPercentage;
    private String currentDifficulty;
    private boolean weak;
    private boolean strong;

    public TopicPerformanceDto() {}

    public TopicPerformanceDto(String topic, int totalQuestions, int correctQuestions,
                               double accuracyPercentage, String currentDifficulty,
                               boolean weak, boolean strong) {
        this.topic = topic;
        this.totalQuestions = totalQuestions;
        this.correctQuestions = correctQuestions;
        this.accuracyPercentage = accuracyPercentage;
        this.currentDifficulty = currentDifficulty;
        this.weak = weak;
        this.strong = strong;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectQuestions() {
        return correctQuestions;
    }

    public void setCorrectQuestions(int correctQuestions) {
        this.correctQuestions = correctQuestions;
    }

    public double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public void setAccuracyPercentage(double accuracyPercentage) {
        this.accuracyPercentage = accuracyPercentage;
    }

    public String getCurrentDifficulty() {
        return currentDifficulty;
    }

    public void setCurrentDifficulty(String currentDifficulty) {
        this.currentDifficulty = currentDifficulty;
    }

    public boolean isWeak() {
        return weak;
    }

    public void setWeak(boolean weak) {
        this.weak = weak;
    }

    public boolean isStrong() {
        return strong;
    }

    public void setStrong(boolean strong) {
        this.strong = strong;
    }
}
