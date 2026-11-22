package com.quizmind.adaptive_quiz.model.dto;

public class WordClueDto {
    private String word;
    private String clue;
    private String topic;
    private Long questionId;
    private boolean found;

    public WordClueDto() {}

    public WordClueDto(String word, String clue, String topic, Long questionId) {
        this.word = word;
        this.clue = clue;
        this.topic = topic;
        this.questionId = questionId;
        this.found = false;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getClue() {
        return clue;
    }

    public void setClue(String clue) {
        this.clue = clue;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }
}
