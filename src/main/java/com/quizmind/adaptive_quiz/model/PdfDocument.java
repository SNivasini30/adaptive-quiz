package com.quizmind.adaptive_quiz.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pdf_documents")
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Column(nullable = false)
    private String filename;

    private Long fileSize;

    private int sentenceCount;

    private int generatedQuestionCount;

    @Column(length = 100000)
    private String rawTextPreview;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String extractedText;

    private String status;

    private LocalDateTime uploadTimestamp;

    public PdfDocument() {
        this.uploadTimestamp = LocalDateTime.now();
    }

    public PdfDocument(String filename, Long fileSize, int sentenceCount, String rawTextPreview, String status) {
        this();
        this.filename = filename;
        this.fileSize = fileSize;
        this.sentenceCount = sentenceCount;
        this.rawTextPreview = rawTextPreview;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public int getSentenceCount() {
        return sentenceCount;
    }

    public void setSentenceCount(int sentenceCount) {
        this.sentenceCount = sentenceCount;
    }

    public int getGeneratedQuestionCount() {
        return generatedQuestionCount;
    }

    public void setGeneratedQuestionCount(int generatedQuestionCount) {
        this.generatedQuestionCount = generatedQuestionCount;
    }

    public String getRawTextPreview() {
        return rawTextPreview;
    }

    public void setRawTextPreview(String rawTextPreview) {
        this.rawTextPreview = rawTextPreview;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
}
