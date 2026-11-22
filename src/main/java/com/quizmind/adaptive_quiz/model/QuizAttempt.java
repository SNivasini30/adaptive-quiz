package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String studentName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id")
    private GeneratedQuestion question;

    private String questionText;

    private String selectedAnswer;

    private String correctAnswer;

    private boolean correct;

    private String topic;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    private int responseTimeSec;

    @Enumerated(EnumType.STRING)
    private Confidence confidence;

    private LocalDateTime attemptTimestamp;

    private String sessionId;

    public QuizAttempt() {
        this.attemptTimestamp = LocalDateTime.now();
    }

    public QuizAttempt(String studentName, GeneratedQuestion question, String selectedAnswer,
                       boolean correct, String topic, Difficulty difficulty,
                       int responseTimeSec, Confidence confidence, String sessionId) {
        this();
        this.studentName = studentName;
        this.question = question;
        this.questionText = question != null ? question.getQuestionText() : "";
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = question != null ? question.getCorrectAnswer() : "";
        this.correct = correct;
        this.topic = topic;
        this.difficulty = difficulty;
        this.responseTimeSec = responseTimeSec;
        this.confidence = confidence != null ? confidence : Confidence.MEDIUM;
        this.sessionId = sessionId;
    }

    public QuizAttempt(User user, GeneratedQuestion question, String selectedAnswer,
                       boolean correct, String topic, Difficulty difficulty,
                       int responseTimeSec, Confidence confidence, String sessionId) {
        this(user != null ? user.getUsername() : "Student", question, selectedAnswer, correct, topic, difficulty, responseTimeSec, confidence, sessionId);
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public GeneratedQuestion getQuestion() {
        return question;
    }

    public void setQuestion(GeneratedQuestion question) {
        this.question = question;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
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

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getResponseTimeSec() {
        return responseTimeSec;
    }

    public void setResponseTimeSec(int responseTimeSec) {
        this.responseTimeSec = responseTimeSec;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getAttemptTimestamp() {
        return attemptTimestamp;
    }

    public void setAttemptTimestamp(LocalDateTime attemptTimestamp) {
        this.attemptTimestamp = attemptTimestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
