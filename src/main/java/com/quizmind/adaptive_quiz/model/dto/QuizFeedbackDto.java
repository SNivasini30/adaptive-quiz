package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.Difficulty;

public class QuizFeedbackDto {
    private boolean correct;
    private String selectedAnswer;
    private String correctAnswer;
    private String explanation;
    private String topic;
    private Difficulty previousDifficulty;
    private Difficulty newDifficulty;
    private boolean difficultyChanged;
    private int consecutiveCorrect;
    private int pointsEarned;
    private int totalScore;
    private int currentStreak;
    private boolean misconceptionAlert;
    private boolean confidenceBoost;
    private QuestionDto nextQuestion;
    private boolean quizCompleted;

    public QuizFeedbackDto() {}

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Difficulty getPreviousDifficulty() {
        return previousDifficulty;
    }

    public void setPreviousDifficulty(Difficulty previousDifficulty) {
        this.previousDifficulty = previousDifficulty;
    }

    public Difficulty getNewDifficulty() {
        return newDifficulty;
    }

    public void setNewDifficulty(Difficulty newDifficulty) {
        this.newDifficulty = newDifficulty;
    }

    public boolean isDifficultyChanged() {
        return difficultyChanged;
    }

    public void setDifficultyChanged(boolean difficultyChanged) {
        this.difficultyChanged = difficultyChanged;
    }

    public int getConsecutiveCorrect() {
        return consecutiveCorrect;
    }

    public void setConsecutiveCorrect(int consecutiveCorrect) {
        this.consecutiveCorrect = consecutiveCorrect;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public boolean isMisconceptionAlert() {
        return misconceptionAlert;
    }

    public void setMisconceptionAlert(boolean misconceptionAlert) {
        this.misconceptionAlert = misconceptionAlert;
    }

    public boolean isConfidenceBoost() {
        return confidenceBoost;
    }

    public void setConfidenceBoost(boolean confidenceBoost) {
        this.confidenceBoost = confidenceBoost;
    }

    public QuestionDto getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(QuestionDto nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public boolean isQuizCompleted() {
        return quizCompleted;
    }

    public void setQuizCompleted(boolean quizCompleted) {
        this.quizCompleted = quizCompleted;
    }
}
