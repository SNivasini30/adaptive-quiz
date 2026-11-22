package com.quizmind.adaptive_quiz.model.dto;

import java.util.ArrayList;
import java.util.List;

public class PuzzleDataDto {
    private int size;
    private char[][] grid;
    private List<WordClueDto> targetWords = new ArrayList<>();

    public PuzzleDataDto() {}

    public PuzzleDataDto(int size, char[][] grid, List<WordClueDto> targetWords) {
        this.size = size;
        this.grid = grid;
        this.targetWords = targetWords;
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

    public List<WordClueDto> getTargetWords() {
        return targetWords;
    }

    public void setTargetWords(List<WordClueDto> targetWords) {
        this.targetWords = targetWords;
    }
}
