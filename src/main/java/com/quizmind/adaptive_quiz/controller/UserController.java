package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.dto.*;
import com.quizmind.adaptive_quiz.service.AuthService;
import com.quizmind.adaptive_quiz.service.ReportService;
import com.quizmind.adaptive_quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    private final ReportService reportService;

    @Autowired
    public UserController(AuthService authService, UserService userService, ReportService reportService) {
        this.authService = authService;
        this.userService = userService;
        this.reportService = reportService;
    }

    private User requireAuth(HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user == null) {
            throw new SecurityException("Unauthorized: Please log in to access this resource.");
        }
        return user;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            UserProfileDto profile = userService.getProfile(user);
            return ResponseEntity.ok(profile);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest updateReq, HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            UserProfileDto updated = userService.updateProfile(user, updateReq);
            return ResponseEntity.ok(updated);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest passReq, HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            Map<String, Object> res = userService.changePassword(user, passReq);
            return ResponseEntity.ok(res);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getProgress(HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            Map<String, Object> progress = userService.getUserProgress(user);
            return ResponseEntity.ok(progress);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            Map<String, Object> history = userService.getUserHistory(user);
            return ResponseEntity.ok(history);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/active-sessions")
    public ResponseEntity<?> getActiveSessions(HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            List<LearningSessionDto> sessions = userService.getActiveSessions(user);
            return ResponseEntity.ok(sessions);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = {"/report/pdf", "/performance/pdf"}, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> downloadReportPdf(HttpServletRequest request) {
        try {
            User user = requireAuth(request);
            byte[] pdfBytes = reportService.generateStudentPerformanceReportPdf(user);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "SmartQuiz_Report_" + user.getUsername() + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report PDF: " + e.getMessage()));
        }
    }
}
