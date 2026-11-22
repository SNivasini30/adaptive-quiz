package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.ApprovalStatus;
import com.quizmind.adaptive_quiz.model.GeneratedQuestion;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserRole;
import com.quizmind.adaptive_quiz.model.dto.QuestionDto;
import com.quizmind.adaptive_quiz.repository.QuestionRepository;
import com.quizmind.adaptive_quiz.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final AuthService authService;

    @Autowired
    public QuestionController(QuestionRepository questionRepository, AuthService authService) {
        this.questionRepository = questionRepository;
        this.authService = authService;
    }

    private boolean checkTeacherAuth(HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user == null) {
            return false;
        }
        return user.getRole() == UserRole.TEACHER;
    }

    @GetMapping
    public ResponseEntity<List<QuestionDto>> getQuestions(
            @RequestParam(value = "status", required = false) String statusStr,
            @RequestParam(value = "documentId", required = false) Long documentId) {
        List<GeneratedQuestion> list;
        if (documentId != null) {
            if (statusStr != null && !statusStr.equalsIgnoreCase("ALL")) {
                try {
                    ApprovalStatus status = ApprovalStatus.valueOf(statusStr.toUpperCase());
                    list = questionRepository.findByDocumentIdAndApprovalStatus(documentId, status);
                } catch (IllegalArgumentException e) {
                    list = questionRepository.findByDocumentId(documentId);
                }
            } else {
                list = questionRepository.findByDocumentId(documentId);
            }
        } else if (statusStr != null && !statusStr.equalsIgnoreCase("ALL")) {
            try {
                ApprovalStatus status = ApprovalStatus.valueOf(statusStr.toUpperCase());
                list = questionRepository.findByApprovalStatusOrderByIdDesc(status);
            } catch (IllegalArgumentException e) {
                list = questionRepository.findAllByOrderByIdDesc();
            }
        } else {
            list = questionRepository.findAllByOrderByIdDesc();
        }

        List<QuestionDto> dtos = list.stream().map(QuestionDto::new).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        return questionRepository.findById(id)
                .map(q -> ResponseEntity.ok(new QuestionDto(q)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<QuestionDto>> getQuestionsByDocument(@PathVariable Long documentId) {
        List<GeneratedQuestion> list = questionRepository.findByDocumentId(documentId);
        List<QuestionDto> dtos = list.stream().map(QuestionDto::new).toList();
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody QuestionDto updateDto, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user != null && user.getRole() != UserRole.TEACHER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Only teachers can edit questions."));
        }

        return questionRepository.findById(id).map(existing -> {
            if (updateDto.getQuestionText() != null) existing.setQuestionText(updateDto.getQuestionText());
            if (updateDto.getCorrectAnswer() != null) existing.setCorrectAnswer(updateDto.getCorrectAnswer());
            if (updateDto.getTopic() != null) existing.setTopic(updateDto.getTopic());
            if (updateDto.getDifficulty() != null) existing.setDifficulty(updateDto.getDifficulty());
            if (updateDto.getOptions() != null && !updateDto.getOptions().isEmpty()) {
                existing.setOptions(updateDto.getOptions());
            }
            if (updateDto.getExplanation() != null) existing.setExplanation(updateDto.getExplanation());
            if (updateDto.getApprovalStatus() != null) existing.setApprovalStatus(updateDto.getApprovalStatus());

            GeneratedQuestion saved = questionRepository.save(existing);
            return ResponseEntity.ok(new QuestionDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{id}/approve", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> approveQuestion(@PathVariable Long id, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user != null && user.getRole() != UserRole.TEACHER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Only teachers can approve questions."));
        }

        return questionRepository.findById(id).map(q -> {
            q.setApprovalStatus(ApprovalStatus.APPROVED);
            GeneratedQuestion saved = questionRepository.save(q);
            return ResponseEntity.ok(new QuestionDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = "/{id}/reject", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> rejectQuestion(@PathVariable Long id, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user != null && user.getRole() != UserRole.TEACHER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Only teachers can reject questions."));
        }

        return questionRepository.findById(id).map(q -> {
            q.setApprovalStatus(ApprovalStatus.REJECTED);
            GeneratedQuestion saved = questionRepository.save(q);
            return ResponseEntity.ok(new QuestionDto(saved));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batch-approve")
    public ResponseEntity<?> batchApprove(@RequestBody List<Long> ids, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user != null && user.getRole() != UserRole.TEACHER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Only teachers can approve questions."));
        }

        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No IDs provided"));
        }
        List<GeneratedQuestion> questions = questionRepository.findAllById(ids);
        for (GeneratedQuestion q : questions) {
            q.setApprovalStatus(ApprovalStatus.APPROVED);
        }
        questionRepository.saveAll(questions);
        return ResponseEntity.ok(Map.of("message", "Approved " + questions.size() + " questions"));
    }

    @PostMapping("/batch-reject")
    public ResponseEntity<?> batchReject(@RequestBody List<Long> ids, HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user != null && user.getRole() != UserRole.TEACHER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Only teachers can reject questions."));
        }

        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No IDs provided"));
        }
        List<GeneratedQuestion> questions = questionRepository.findAllById(ids);
        for (GeneratedQuestion q : questions) {
            q.setApprovalStatus(ApprovalStatus.REJECTED);
        }
        questionRepository.saveAll(questions);
        return ResponseEntity.ok(Map.of("message", "Rejected " + questions.size() + " questions"));
    }
}
