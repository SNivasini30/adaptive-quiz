package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.dto.AnswerSubmissionDto;
import com.quizmind.adaptive_quiz.model.dto.QuestionDto;
import com.quizmind.adaptive_quiz.model.dto.QuizFeedbackDto;
import com.quizmind.adaptive_quiz.repository.QuestionRepository;
import com.quizmind.adaptive_quiz.service.AdaptiveQuizService;
import com.quizmind.adaptive_quiz.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class QuizController {

    private final AdaptiveQuizService adaptiveQuizService;
    private final QuestionRepository questionRepository;
    private final AuthService authService;

    @Autowired
    public QuizController(AdaptiveQuizService adaptiveQuizService,
                          QuestionRepository questionRepository,
                          AuthService authService) {
        this.adaptiveQuizService = adaptiveQuizService;
        this.questionRepository = questionRepository;
        this.authService = authService;
    }

    @PostMapping(value = {"/api/quiz/start", "/api/quiz/start/{documentId}"})
    public ResponseEntity<Map<String, Object>> startQuiz(
            @PathVariable(value = "documentId", required = false) Long documentId,
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            @RequestParam(value = "topic", required = false) String topic,
            HttpServletRequest request) {
        User user = authService.resolveUser(request);
        String effectiveStudent = (user != null) ? user.getUsername() : studentName;
        Map<String, Object> session = adaptiveQuizService.startQuizSession(effectiveStudent, topic, documentId);
        return ResponseEntity.ok(session);
    }

    @PostMapping(value = {"/api/quiz/{sessionId}/submit", "/api/quiz/submit"})
    public ResponseEntity<?> submitAnswer(
            @PathVariable(value = "sessionId", required = false) String pathSessionId,
            @RequestBody AnswerSubmissionDto submission,
            HttpServletRequest request) {
        try {
            if (pathSessionId != null && submission.getSessionId() == null) {
                submission.setSessionId(pathSessionId);
            }
            User user = authService.resolveUser(request);
            if (user != null && (submission.getStudentName() == null || submission.getStudentName().equals("Student"))) {
                submission.setStudentName(user.getUsername());
            }
            QuizFeedbackDto feedback = adaptiveQuizService.submitAnswer(submission);
            return ResponseEntity.ok(feedback);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = {"/api/quiz/{sessionId}/next", "/api/quiz/next"})
    public ResponseEntity<?> getNextQuestion(
            @PathVariable(value = "sessionId", required = false) String pathSessionId,
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestParam(value = "sessionId", required = false) String paramSessionId,
            HttpServletRequest request) {
        String effectiveSessionId = pathSessionId != null ? pathSessionId : paramSessionId;
        User user = authService.resolveUser(request);
        String effectiveStudent = (user != null) ? user.getUsername() : studentName;
        QuestionDto nextQ = adaptiveQuizService.getNextAdaptiveQuestion(effectiveStudent, topic, effectiveSessionId);
        if (nextQ == null) {
            return ResponseEntity.ok(Map.of("completed", true, "message", "All questions in this session have been completed!"));
        }
        return ResponseEntity.ok(nextQ);
    }

    @GetMapping("/api/quiz/{sessionId}/result")
    public ResponseEntity<?> getQuizResult(@PathVariable("sessionId") String sessionId) {
        Map<String, Object> result = adaptiveQuizService.getSessionResult(sessionId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/quiz/topics")
    public ResponseEntity<List<String>> getAvailableTopics() {
        List<String> topics = questionRepository.findDistinctApprovedTopics();
        if (topics.isEmpty()) {
            topics = questionRepository.findAllDistinctTopics();
        }
        return ResponseEntity.ok(topics);
    }
}
