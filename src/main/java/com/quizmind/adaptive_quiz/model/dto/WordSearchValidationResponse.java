package com.quizmind.adaptive_quiz.model.dto;

import java.util.List;
import java.util.Map;

public class WordSearchValidationResponse {

    private boolean correct;
    private String message;
    private String foundWord;
    private List<CoordinateDto> coordinates;
    private boolean questionCompleted;
    private int currentQuestionIndex;
    private int totalQuestions;
    private int completedCount;
    private boolean puzzleCompleted;
    private QuestionDto nextQuestion;
    private Map<String, Object> completionStats;

    public WordSearchValidationResponse() {}

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFoundWord() {
        return foundWord;
    }

    public void setFoundWord(String foundWord) {
        this.foundWord = foundWord;
    }

    public List<CoordinateDto> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<CoordinateDto> coordinates) {
        this.coordinates = coordinates;
    }

    public boolean isQuestionCompleted() {
        return questionCompleted;
    }

    public void setQuestionCompleted(boolean questionCompleted) {
        this.questionCompleted = questionCompleted;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public boolean isPuzzleCompleted() {
        return puzzleCompleted;
    }

    public void setPuzzleCompleted(boolean puzzleCompleted) {
        this.puzzleCompleted = puzzleCompleted;
    }

    public QuestionDto getNextQuestion() {
        return nextQuestion;
    }

    public void setNextQuestion(QuestionDto nextQuestion) {
        this.nextQuestion = nextQuestion;
    }

    public Map<String, Object> getCompletionStats() {
        return completionStats;
    }

    public void setCompletionStats(Map<String, Object> completionStats) {
        this.completionStats = completionStats;
    }
}
