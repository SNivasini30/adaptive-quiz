package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.PdfDocument;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserRole;
import com.quizmind.adaptive_quiz.repository.PdfDocumentRepository;
import com.quizmind.adaptive_quiz.service.AuthService;
import com.quizmind.adaptive_quiz.service.PdfService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class PdfController {

    private final PdfService pdfService;
    private final AuthService authService;
    private final PdfDocumentRepository pdfDocumentRepository;

    @Autowired
    public PdfController(PdfService pdfService, AuthService authService, PdfDocumentRepository pdfDocumentRepository) {
        this.pdfService = pdfService;
        this.authService = authService;
        this.pdfDocumentRepository = pdfDocumentRepository;
    }

    /**
     * POST /api/pdf/upload
     * Accessible by BOTH Teachers and Students.
     * Documents are automatically associated with the authenticated user.
     */
    @PostMapping("/api/pdf/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        // Both TEACHER and STUDENT are allowed to upload

        try {
            PdfDocument doc = pdfService.processPdf(file, user);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", user != null && user.getRole() == UserRole.STUDENT
                    ? "Study material uploaded successfully! You can now practice quizzes and word search."
                    : "PDF successfully uploaded and processed!");
            response.put("documentId", doc.getId());
            response.put("filename", doc.getFilename());
            response.put("fileSize", doc.getFileSize());
            response.put("sentenceCount", doc.getSentenceCount());
            response.put("generatedQuestions", doc.getGeneratedQuestionCount());
            response.put("status", doc.getStatus());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Failed to process PDF: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    /**
     * GET /api/pdf & /api/pdf/documents
     * Teachers see all documents; Students see their own uploaded study materials.
     */
    @GetMapping(value = {"/api/pdf", "/api/pdf/documents"})
    public ResponseEntity<List<PdfDocument>> getAllDocuments(HttpServletRequest request) {
        User user = authService.resolveUser(request);
        return ResponseEntity.ok(pdfService.getDocumentsForUser(user));
    }

    /**
     * GET /api/pdf/my-materials
     * Returns strictly the authenticated student's or teacher's study materials.
     */
    @GetMapping("/api/pdf/my-materials")
    public ResponseEntity<?> getMyMaterials(HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }
        return ResponseEntity.ok(pdfDocumentRepository.findByUploadedByIdOrderByUploadTimestampDesc(user.getId()));
    }

    /**
     * GET /api/pdf/{id}
     * Retrieves document with strict data isolation:
     * Student B cannot inspect Student A's private study document.
     */
    @GetMapping(value = {"/api/pdf/{id}", "/api/pdf/documents/{id}"})
    public ResponseEntity<?> getDocument(@PathVariable Long id, HttpServletRequest request) {
        PdfDocument doc = pdfService.getDocumentById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        User user = authService.resolveUser(request);
        // Student privacy isolation check
        if (doc.getUploadedBy() != null && doc.getUploadedBy().getRole() == UserRole.STUDENT) {
            if (user != null && user.getRole() == UserRole.STUDENT && !doc.getUploadedBy().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied: You cannot view another student's private study material."));
            }
        }

        return ResponseEntity.ok(doc);
    }
}
