package com.quizmind.adaptive_quiz.model.dto;

public class CoordinateDto {
    private int row;
    private int col;

    public CoordinateDto() {}

    public CoordinateDto(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }
}
