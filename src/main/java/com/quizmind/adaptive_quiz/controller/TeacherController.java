package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.PdfDocument;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserRole;
import com.quizmind.adaptive_quiz.model.dto.StudentSummaryDto;
import com.quizmind.adaptive_quiz.service.AuthService;
import com.quizmind.adaptive_quiz.service.PdfService;
import com.quizmind.adaptive_quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@CrossOrigin(origins = "*")
public class TeacherController {

    private final AuthService authService;
    private final UserService userService;
    private final PdfService pdfService;

    @Autowired
    public TeacherController(AuthService authService, UserService userService, PdfService pdfService) {
        this.authService = authService;
        this.userService = userService;
        this.pdfService = pdfService;
    }

    private User requireTeacherAuth(HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user == null) {
            throw new SecurityException("UNAUTHORIZED");
        }
        if (user.getRole() != UserRole.TEACHER) {
            throw new SecurityException("FORBIDDEN");
        }
        return user;
    }

    @GetMapping("/documents")
    public ResponseEntity<?> getTeacherDocuments(HttpServletRequest request) {
        try {
            requireTeacherAuth(request);
            List<PdfDocument> docs = pdfService.getAllDocuments();
            return ResponseEntity.ok(docs);
        } catch (SecurityException e) {
            if ("UNAUTHORIZED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please log in as teacher."));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Teacher role required."));
        }
    }

    @GetMapping("/students")
    public ResponseEntity<?> getTeacherStudents(HttpServletRequest request) {
        try {
            requireTeacherAuth(request);
            List<StudentSummaryDto> students = userService.getStudentSummariesForTeacher();
            return ResponseEntity.ok(students);
        } catch (SecurityException e) {
            if ("UNAUTHORIZED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please log in as teacher."));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Teacher role required."));
        }
    }

    @GetMapping("/students/{userId}/performance")
    public ResponseEntity<?> getStudentPerformance(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            requireTeacherAuth(request);
            Map<String, Object> performance = userService.getStudentPerformanceForTeacher(userId);
            return ResponseEntity.ok(performance);
        } catch (SecurityException e) {
            if ("UNAUTHORIZED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please log in as teacher."));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied: Teacher role required."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
