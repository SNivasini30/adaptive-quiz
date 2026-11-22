package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class GeneratedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String questionText;

    @Column(nullable = false, length = 500)
    private String correctAnswer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options = new ArrayList<>();

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType;

    @Column(length = 2000)
    private String sourceSentence;

    @Column(length = 2000)
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.GENERATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private PdfDocument document;

    private LocalDateTime createdAt;

    public GeneratedQuestion() {
        this.createdAt = LocalDateTime.now();
        this.approvalStatus = ApprovalStatus.GENERATED;
    }

    public GeneratedQuestion(String questionText, String correctAnswer, List<String> options,
                             String topic, Difficulty difficulty, QuestionType questionType,
                             String sourceSentence, String explanation, ApprovalStatus approvalStatus) {
        this();
        this.questionText = questionText;
        this.correctAnswer = correctAnswer;
        if (options != null) {
            this.options = new ArrayList<>(options);
        }
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.sourceSentence = sourceSentence;
        this.explanation = explanation;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.GENERATED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
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

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getSourceSentence() {
        return sourceSentence;
    }

    public void setSourceSentence(String sourceSentence) {
        this.sourceSentence = sourceSentence;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public PdfDocument getDocument() {
        return document;
    }

    public void setDocument(PdfDocument document) {
        this.document = document;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
