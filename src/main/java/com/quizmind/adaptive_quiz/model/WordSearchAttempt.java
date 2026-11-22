package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "word_search_attempts")
public class WordSearchAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "puzzle_id", nullable = false)
    private WordSearchPuzzle puzzle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String studentName;

    private String sessionToken;

    private int currentQuestionIndex = 0;

    private int totalQuestions = 0;

    private int correctCount = 0;

    private int incorrectAttempts = 0;

    private boolean completed = false;

    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime endTime;

    private int durationSeconds = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "word_search_completed_words", joinColumns = @JoinColumn(name = "attempt_id"))
    @Column(name = "word")
    private Set<String> completedWords = new HashSet<>();

    public WordSearchAttempt() {
        this.startTime = LocalDateTime.now();
    }

    public WordSearchAttempt(WordSearchPuzzle puzzle, String studentName, String sessionToken, int totalQuestions) {
        this();
        this.puzzle = puzzle;
        this.studentName = studentName != null ? studentName : "Student";
        this.sessionToken = sessionToken;
        this.totalQuestions = totalQuestions;
    }

    public WordSearchAttempt(WordSearchPuzzle puzzle, User user, String sessionToken, int totalQuestions) {
        this(puzzle, user != null ? user.getUsername() : "Student", sessionToken, totalQuestions);
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null && (this.studentName == null || this.studentName.isEmpty())) {
            this.studentName = user.getUsername();
        }
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
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

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getIncorrectAttempts() {
        return incorrectAttempts;
    }

    public void setIncorrectAttempts(int incorrectAttempts) {
        this.incorrectAttempts = incorrectAttempts;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Set<String> getCompletedWords() {
        return completedWords;
    }

    public void setCompletedWords(Set<String> completedWords) {
        this.completedWords = completedWords;
    }
}
