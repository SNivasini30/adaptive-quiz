package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;

@Entity
@Table(name = "word_placements")
public class WordPlacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "puzzle_id")
    private WordSearchPuzzle puzzle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    private GeneratedQuestion question;

    @Column(nullable = false)
    private String word;

    private int startRow;
    private int startCol;
    private int endRow;
    private int endCol;
    private int dRow;
    private int dCol;
    private int questionOrder;

    public WordPlacement() {}

    public WordPlacement(WordSearchPuzzle puzzle, GeneratedQuestion question, String word,
                         int startRow, int startCol, int endRow, int endCol,
                         int dRow, int dCol, int questionOrder) {
        this.puzzle = puzzle;
        this.question = question;
        this.word = word;
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
        this.dRow = dRow;
        this.dCol = dCol;
        this.questionOrder = questionOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WordSearchPuzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(WordSearchPuzzle puzzle) {
        this.puzzle = puzzle;
    }

    public GeneratedQuestion getQuestion() {
        return question;
    }

    public void setQuestion(GeneratedQuestion question) {
        this.question = question;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public void setStartCol(int startCol) {
        this.startCol = startCol;
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public int getEndCol() {
        return endCol;
    }

    public void setEndCol(int endCol) {
        this.endCol = endCol;
    }

    public int getdRow() {
        return dRow;
    }

    public void setdRow(int dRow) {
        this.dRow = dRow;
    }

    public int getdCol() {
        return dCol;
    }

    public void setdCol(int dCol) {
        this.dCol = dCol;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }
}
