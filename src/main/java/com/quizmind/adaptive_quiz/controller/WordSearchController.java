package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.dto.*;
import com.quizmind.adaptive_quiz.service.AuthService;
import com.quizmind.adaptive_quiz.service.WordSearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class WordSearchController {

    private final WordSearchService wordSearchService;
    private final AuthService authService;

    @Autowired
    public WordSearchController(WordSearchService wordSearchService, AuthService authService) {
        this.wordSearchService = wordSearchService;
        this.authService = authService;
    }

    /**
     * Generate Word Search session for Question-by-Question gameplay.
     */
    @PostMapping(value = {"/api/wordsearch/generate", "/api/wordsearch/generate/{documentId}"})
    public ResponseEntity<WordSearchInitResponse> generateWordSearch(
            @PathVariable(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = authService.resolveUser(request);
        String effectiveStudent = (user != null) ? user.getUsername() : studentName;
        WordSearchInitResponse response = wordSearchService.createWordSearchSession(documentId, effectiveStudent);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate student selection against the current active question.
     */
    @PostMapping(value = {"/api/wordsearch/{id}/validate", "/api/wordsearch/validate"})
    public ResponseEntity<WordSearchValidationResponse> validateWord(
            @PathVariable(value = "id", required = false) Long id,
            @RequestBody WordSearchValidationRequest request) {
        if (id != null && request.getPuzzleId() == null) {
            request.setPuzzleId(id);
        }
        WordSearchValidationResponse response = wordSearchService.validateWordSelection(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the active question for current session.
     */
    @GetMapping(value = {"/api/wordsearch/{sessionToken}/current-question", "/api/wordsearch/session/{sessionToken}/current-question"})
    public ResponseEntity<?> getCurrentQuestion(@PathVariable("sessionToken") String sessionToken) {
        try {
            Map<String, Object> question = wordSearchService.getCurrentQuestion(sessionToken);
            return ResponseEntity.ok(question);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current progress of the puzzle session.
     */
    @GetMapping(value = {"/api/wordsearch/{sessionToken}/progress", "/api/wordsearch/session/{sessionToken}/progress"})
    public ResponseEntity<?> getProgress(@PathVariable("sessionToken") String sessionToken) {
        try {
            Map<String, Object> progress = wordSearchService.getSessionProgress(sessionToken);
            return ResponseEntity.ok(progress);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Move to next question if current is solved.
     */
    @PostMapping(value = {"/api/wordsearch/{sessionToken}/next", "/api/wordsearch/session/{sessionToken}/next"})
    public ResponseEntity<?> nextQuestion(@PathVariable("sessionToken") String sessionToken) {
        try {
            Map<String, Object> question = wordSearchService.getCurrentQuestion(sessionToken);
            return ResponseEntity.ok(question);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Backward-compatible endpoint for legacy puzzle view.
     */
    @GetMapping("/api/puzzle")
    public ResponseEntity<PuzzleDataDto> getLegacyPuzzle(
            @RequestParam(value = "documentId", required = false) Long documentId) {
        PuzzleDataDto puzzle = wordSearchService.generatePuzzle(documentId);
        return ResponseEntity.ok(puzzle);
    }

    @PostMapping("/api/puzzle/verify")
    public ResponseEntity<?> verifyLegacyWord(@RequestBody Map<String, String> request) {
        String word = request.get("word");
        if (word == null || word.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "No word provided"));
        }
        return ResponseEntity.ok(Map.of("valid", true, "word", word.toUpperCase()));
    }
}
