package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "word_search_puzzles")
public class WordSearchPuzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int size;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String gridLetters; // Flattened or comma/newline separated characters

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private PdfDocument document;

    @OneToMany(mappedBy = "puzzle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("questionOrder ASC")
    private List<WordPlacement> placements = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    public WordSearchPuzzle() {}

    public WordSearchPuzzle(int size, String gridLetters, PdfDocument document) {
        this.size = size;
        this.gridLetters = gridLetters;
        this.document = document;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getGridLetters() {
        return gridLetters;
    }

    public void setGridLetters(String gridLetters) {
        this.gridLetters = gridLetters;
    }

    public PdfDocument getDocument() {
        return document;
    }

    public void setDocument(PdfDocument document) {
        this.document = document;
    }

    public List<WordPlacement> getPlacements() {
        return placements;
    }

    public void setPlacements(List<WordPlacement> placements) {
        this.placements = placements;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public char[][] toGridArray() {
        char[][] grid = new char[size][size];
        if (gridLetters == null || gridLetters.length() < size * size) {
            return grid;
        }
        int idx = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                grid[r][c] = gridLetters.charAt(idx++);
            }
        }
        return grid;
    }

    public static String fromGridArray(char[][] grid, int size) {
        StringBuilder sb = new StringBuilder(size * size);
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                sb.append(grid[r][c]);
            }
        }
        return sb.toString();
    }
}
