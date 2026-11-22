package com.quizmind.adaptive_quiz.controller;

import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.dto.AuthRequest;
import com.quizmind.adaptive_quiz.model.dto.AuthResponse;
import com.quizmind.adaptive_quiz.model.dto.RegisterRequest;
import com.quizmind.adaptive_quiz.model.dto.UserProfileDto;
import com.quizmind.adaptive_quiz.service.AuthService;
import com.quizmind.adaptive_quiz.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Autowired
    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request, HttpSession session) {
        AuthResponse response = authService.register(request, session);
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, HttpSession session) {
        AuthResponse response = authService.login(request, session);
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "X-Auth-Token", required = false) String tokenHeader,
            HttpServletRequest request,
            HttpSession session) {
        String token = tokenHeader;
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            }
        }
        authService.logout(token, session);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        User user = authService.resolveUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "error", "User is not logged in."));
        }
        UserProfileDto profile = userService.getProfile(user);
        return ResponseEntity.ok(profile);
    }
}
