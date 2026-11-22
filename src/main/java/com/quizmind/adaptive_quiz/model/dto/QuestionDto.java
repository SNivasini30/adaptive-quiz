package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.ApprovalStatus;
import com.quizmind.adaptive_quiz.model.Difficulty;
import com.quizmind.adaptive_quiz.model.GeneratedQuestion;
import com.quizmind.adaptive_quiz.model.QuestionType;

import java.util.ArrayList;
import java.util.List;

public class QuestionDto {
    private Long id;
    private String questionText;
    private String correctAnswer;
    private List<String> options = new ArrayList<>();
    private String topic;
    private Difficulty difficulty;
    private QuestionType questionType;
    private String sourceSentence;
    private String explanation;
    private ApprovalStatus approvalStatus;

    public QuestionDto() {}

    public QuestionDto(GeneratedQuestion q) {
        if (q != null) {
            this.id = q.getId();
            this.questionText = q.getQuestionText();
            this.correctAnswer = q.getCorrectAnswer();
            this.options = q.getOptions() != null ? new ArrayList<>(q.getOptions()) : new ArrayList<>();
            this.topic = q.getTopic();
            this.difficulty = q.getDifficulty();
            this.questionType = q.getQuestionType();
            this.sourceSentence = q.getSourceSentence();
            this.explanation = q.getExplanation();
            this.approvalStatus = q.getApprovalStatus();
        }
    }

    public static QuestionDto forStudent(GeneratedQuestion q) {
        if (q == null) return null;
        QuestionDto dto = new QuestionDto(q);
        dto.setCorrectAnswer(null); // Never leak answer to student
        return dto;
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
}
