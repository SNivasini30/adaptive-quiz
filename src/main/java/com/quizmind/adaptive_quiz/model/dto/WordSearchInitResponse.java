package com.quizmind.adaptive_quiz.model.dto;

import java.util.List;

public class WordSearchInitResponse {

    private Long puzzleId;
    private String sessionToken;
    private int size;
    private char[][] grid;
    private int totalQuestions;
    private int currentQuestionIndex;
    private QuestionDto currentQuestion;
    private int completedCount;
    private List<String> completedWords;

    public WordSearchInitResponse() {}

    public Long getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(Long puzzleId) {
        this.puzzleId = puzzleId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public char[][] getGrid() {
        return grid;
    }

    public void setGrid(char[][] grid) {
        this.grid = grid;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public QuestionDto getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(QuestionDto currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public List<String> getCompletedWords() {
        return completedWords;
    }

    public void setCompletedWords(List<String> completedWords) {
        this.completedWords = completedWords;
    }
}
