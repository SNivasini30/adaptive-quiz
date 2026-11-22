package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.QuizAttempt;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.dto.DashboardStatsDto;
import com.quizmind.adaptive_quiz.model.dto.TopicPerformanceDto;
import com.quizmind.adaptive_quiz.service.AuthService;
import com.quizmind.adaptive_quiz.service.PerformanceAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final PerformanceAnalyticsService analyticsService;
    private final AuthService authService;

    @Autowired
    public AnalyticsController(PerformanceAnalyticsService analyticsService, AuthService authService) {
        this.analyticsService = analyticsService;
        this.authService = authService;
    }

    private User getResolvedUser(HttpServletRequest request) {
        return authService.resolveUser(request);
    }

    // Dashboard Endpoints
    @GetMapping(value = {"/api/dashboard/stats", "/api/analytics/dashboard"})
    public ResponseEntity<DashboardStatsDto> getDashboardStats(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getDashboardStatistics(user));
        }
        return ResponseEntity.ok(analyticsService.getDashboardStatistics(studentName));
    }

    @GetMapping("/api/dashboard/topics")
    public ResponseEntity<List<TopicPerformanceDto>> getDashboardTopics(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getTopicPerformances(user));
        }
        return ResponseEntity.ok(analyticsService.getTopicPerformances(studentName));
    }

    @GetMapping("/api/dashboard/activity")
    public ResponseEntity<List<QuizAttempt>> getDashboardActivity(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getRecentActivity(user));
        }
        return ResponseEntity.ok(analyticsService.getRecentActivity(studentName));
    }

    // Mistake Analyzer Endpoints
    @GetMapping(value = {"/api/mistakes", "/api/analytics/mistakes"})
    public ResponseEntity<List<QuizAttempt>> getMistakes(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getMistakes(user));
        }
        return ResponseEntity.ok(analyticsService.getMistakes(studentName));
    }

    @GetMapping("/api/mistakes/topics")
    public ResponseEntity<Map<String, Object>> getMistakesByTopic(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getMistakesByTopic(user));
        }
        return ResponseEntity.ok(analyticsService.getMistakesByTopic(studentName));
    }

    // Progress Endpoints
    @GetMapping("/api/progress")
    public ResponseEntity<Map<String, Object>> getOverallProgress(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getOverallProgress(user));
        }
        return ResponseEntity.ok(analyticsService.getOverallProgress(studentName));
    }

    @GetMapping("/api/progress/topics")
    public ResponseEntity<List<TopicPerformanceDto>> getProgressTopics(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getTopicPerformances(user));
        }
        return ResponseEntity.ok(analyticsService.getTopicPerformances(studentName));
    }

    @GetMapping("/api/progress/history")
    public ResponseEntity<List<QuizAttempt>> getProgressHistory(
            @RequestParam(value = "studentName", defaultValue = "Student") String studentName,
            HttpServletRequest request) {
        User user = getResolvedUser(request);
        if (user != null) {
            return ResponseEntity.ok(analyticsService.getHistory(user));
        }
        return ResponseEntity.ok(analyticsService.getHistory(studentName));
    }
}
