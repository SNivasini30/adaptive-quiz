package com.quizmind.adaptive_quiz.model.dto;

import java.util.List;

public class WordSearchValidationRequest {

    private Long puzzleId;
    private String sessionToken;
    private Long questionId;
    private Integer startRow;
    private Integer startCol;
    private Integer endRow;
    private Integer endCol;
    private String selectedWord;
    private List<CoordinateDto> coordinates;

    public WordSearchValidationRequest() {}

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

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }

    public Integer getStartCol() {
        return startCol;
    }

    public void setStartCol(Integer startCol) {
        this.startCol = startCol;
    }

    public Integer getEndRow() {
        return endRow;
    }

    public void setEndRow(Integer endRow) {
        this.endRow = endRow;
    }

    public Integer getEndCol() {
        return endCol;
    }

    public void setEndCol(Integer endCol) {
        this.endCol = endCol;
    }

    public String getSelectedWord() {
        return selectedWord;
    }

    public void setSelectedWord(String selectedWord) {
        this.selectedWord = selectedWord;
    }

    public List<CoordinateDto> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<CoordinateDto> coordinates) {
        this.coordinates = coordinates;
    }
}
