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

    @PostMapping("/api/pdf/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user != null && user.getRole() != UserRole.TEACHER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "error", "Access denied: Only teachers can upload and process PDF documents."));
        }

        try {
            PdfDocument doc = pdfService.processPdf(file);
            if (user != null) {
                doc.setUploadedBy(user);
                pdfDocumentRepository.save(doc);
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "PDF successfully processed!");
            response.put("documentId", doc.getId());
            response.put("filename", doc.getFilename());
            response.put("fileSize", doc.getFileSize());
            response.put("sentenceCount", doc.getSentenceCount());
            response.put("generatedQuestions", doc.getGeneratedQuestionCount());
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

    @GetMapping(value = {"/api/pdf", "/api/pdf/documents"})
    public ResponseEntity<List<PdfDocument>> getAllDocuments() {
        return ResponseEntity.ok(pdfService.getAllDocuments());
    }

    @GetMapping(value = {"/api/pdf/{id}", "/api/pdf/documents/{id}"})
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        PdfDocument doc = pdfService.getDocumentById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }
}
