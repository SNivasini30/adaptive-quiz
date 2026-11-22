package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.QuizAttempt;
import java.util.ArrayList;
import java.util.List;

public class DashboardStatsDto {
    private String studentName;
    private String fullName;
    private int totalQuizzesCompleted;
    private long totalQuestionsAttempted;
    private double overallAccuracy;
    private double averageResponseTimeSec;
    private int totalTopicsPracticed;

    private int totalPuzzlesCompleted;
    private int totalCorrectWords;
    private int totalIncorrectAttempts;

    private String strongestTopic;
    private String weakestTopic;

    private long correctAnswersCount;
    private long incorrectAnswersCount;

    private List<TopicPerformanceDto> topicPerformances = new ArrayList<>();
    private List<String> weakTopics = new ArrayList<>();
    private List<String> strongTopics = new ArrayList<>();
    private List<QuizAttempt> recentActivity = new ArrayList<>();

    public DashboardStatsDto() {}

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getTotalQuizzesCompleted() {
        return totalQuizzesCompleted;
    }

    public void setTotalQuizzesCompleted(int totalQuizzesCompleted) {
        this.totalQuizzesCompleted = totalQuizzesCompleted;
    }

    public long getTotalQuestionsAttempted() {
        return totalQuestionsAttempted;
    }

    public void setTotalQuestionsAttempted(long totalQuestionsAttempted) {
        this.totalQuestionsAttempted = totalQuestionsAttempted;
    }

    public double getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(double overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public double getAverageResponseTimeSec() {
        return averageResponseTimeSec;
    }

    public void setAverageResponseTimeSec(double averageResponseTimeSec) {
        this.averageResponseTimeSec = averageResponseTimeSec;
    }

    public int getTotalTopicsPracticed() {
        return totalTopicsPracticed;
    }

    public void setTotalTopicsPracticed(int totalTopicsPracticed) {
        this.totalTopicsPracticed = totalTopicsPracticed;
    }

    public int getTotalPuzzlesCompleted() {
        return totalPuzzlesCompleted;
    }

    public void setTotalPuzzlesCompleted(int totalPuzzlesCompleted) {
        this.totalPuzzlesCompleted = totalPuzzlesCompleted;
    }

    public int getTotalCorrectWords() {
        return totalCorrectWords;
    }

    public void setTotalCorrectWords(int totalCorrectWords) {
        this.totalCorrectWords = totalCorrectWords;
    }

    public int getTotalIncorrectAttempts() {
        return totalIncorrectAttempts;
    }

    public void setTotalIncorrectAttempts(int totalIncorrectAttempts) {
        this.totalIncorrectAttempts = totalIncorrectAttempts;
    }

    public String getStrongestTopic() {
        return strongestTopic;
    }

    public void setStrongestTopic(String strongestTopic) {
        this.strongestTopic = strongestTopic;
    }

    public String getWeakestTopic() {
        return weakestTopic;
    }

    public void setWeakestTopic(String weakestTopic) {
        this.weakestTopic = weakestTopic;
    }

    public long getCorrectAnswersCount() {
        return correctAnswersCount;
    }

    public void setCorrectAnswersCount(long correctAnswersCount) {
        this.correctAnswersCount = correctAnswersCount;
    }

    public long getIncorrectAnswersCount() {
        return incorrectAnswersCount;
    }

    public void setIncorrectAnswersCount(long incorrectAnswersCount) {
        this.incorrectAnswersCount = incorrectAnswersCount;
    }

    public long getTotalCorrect() {
        return correctAnswersCount;
    }

    public long getTotalIncorrect() {
        return incorrectAnswersCount;
    }

    public List<TopicPerformanceDto> getTopicPerformances() {
        return topicPerformances;
    }

    public void setTopicPerformances(List<TopicPerformanceDto> topicPerformances) {
        this.topicPerformances = topicPerformances;
    }

    public List<String> getWeakTopics() {
        return weakTopics;
    }

    public void setWeakTopics(List<String> weakTopics) {
        this.weakTopics = weakTopics;
    }

    public List<String> getStrongTopics() {
        return strongTopics;
    }

    public void setStrongTopics(List<String> strongTopics) {
        this.strongTopics = strongTopics;
    }

    public List<QuizAttempt> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<QuizAttempt> recentActivity) {
        this.recentActivity = recentActivity;
    }
}
