package com.quizmind.adaptive_quiz.model.dto;

import com.quizmind.adaptive_quiz.model.Confidence;

public class AnswerSubmissionDto {
    private Long questionId;
    private String selectedAnswer;
    private int responseTimeSec = 10;
    private Confidence confidence = Confidence.MEDIUM;
    private String sessionId;
    private String studentName = "Student";

    public AnswerSubmissionDto() {}

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
