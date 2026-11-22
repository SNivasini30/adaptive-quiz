package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.AccountStatus;
import com.quizmind.adaptive_quiz.model.User;
import com.quizmind.adaptive_quiz.model.UserProfile;
import com.quizmind.adaptive_quiz.model.UserRole;
import com.quizmind.adaptive_quiz.model.dto.*;
import com.quizmind.adaptive_quiz.repository.UserProfileRepository;
import com.quizmind.adaptive_quiz.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    public static final String SESSION_USER_ID = "LOGGED_IN_USER_ID";
    private static final String TEACHER_SECRET = "TEACHER2026";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Map token -> userId
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    @Autowired
    public AuthService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpSession httpSession) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return new AuthResponse(false, "Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().trim().length() < 4) {
            return new AuthResponse(false, "Password must be at least 4 characters long.");
        }

        String username = request.getUsername().trim();

        // 1. Case-insensitive duplicate check
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return new AuthResponse(false, "Username '" + username + "' is already taken. Please choose another username.");
        }

        // 2. Validate Role Assignment
        UserRole targetRole = request.getRole() != null ? request.getRole() : UserRole.STUDENT;
        if (targetRole == UserRole.TEACHER) {
            if (request.getTeacherPasscode() == null || !TEACHER_SECRET.equals(request.getTeacherPasscode().trim())) {
                return new AuthResponse(false, "Invalid teacher verification passcode. Only authorized teachers can register with the TEACHER role.");
            }
        }

        String fullName = (request.getFullName() != null && !request.getFullName().trim().isEmpty())
                ? request.getFullName().trim() : username;
        String email = (request.getEmail() != null && !request.getEmail().trim().isEmpty())
                ? request.getEmail().trim() : null;

        // 3. BCrypt password hashing
        String hashedPassword = passwordEncoder.encode(request.getPassword().trim());

        User newUser = new User(username, hashedPassword, fullName, email, targetRole);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLoginAt(LocalDateTime.now());
        newUser.setStatus(AccountStatus.ACTIVE);
        newUser = userRepository.save(newUser);

        UserProfile profile = new UserProfile(newUser);
        profile = userProfileRepository.save(profile);

        // 4. Create Session & Token
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, newUser.getId());
        if (httpSession != null) {
            httpSession.setAttribute(SESSION_USER_ID, newUser.getId());
        }

        UserProfileDto profileDto = new UserProfileDto(newUser, profile);
        return new AuthResponse(true, token, "Registration successful! Welcome, " + newUser.getFullName() + ".", profileDto);
    }

    @Transactional
    public AuthResponse login(AuthRequest request, HttpSession httpSession) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return new AuthResponse(false, "Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return new AuthResponse(false, "Password is required.");
        }

        String username = request.getUsername().trim();

        // 1. Case-insensitive lookup
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) {
            return new AuthResponse(false, "Invalid username or password.");
        }

        // 2. BCrypt verification
        if (!passwordEncoder.matches(request.getPassword().trim(), user.getPassword())) {
            return new AuthResponse(false, "Invalid username or password.");
        }

        if (user.getStatus() == AccountStatus.SUSPENDED) {
            return new AuthResponse(false, "Account is suspended. Please contact administrator.");
        }

        // 3. Update Last Login Time (never overwrite user progress!)
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> userProfileRepository.save(new UserProfile(user)));

        // 4. Session Token
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, user.getId());
        if (httpSession != null) {
            httpSession.setAttribute(SESSION_USER_ID, user.getId());
        }

        UserProfileDto profileDto = new UserProfileDto(user, profile);
        return new AuthResponse(true, token, "Login successful! Welcome back, " + user.getFullName() + ".", profileDto);
    }

    public void logout(String token, HttpSession httpSession) {
        if (token != null) {
            tokenStore.remove(token);
        }
        if (httpSession != null) {
            httpSession.removeAttribute(SESSION_USER_ID);
            try {
                httpSession.invalidate();
            } catch (IllegalStateException ignored) {}
        }
    }

    public User resolveUser(HttpServletRequest request) {
        if (request == null) return null;

        // 1. Check Header X-Auth-Token
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isEmpty()) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7).trim();
            }
        }

        if (token != null && tokenStore.containsKey(token)) {
            Long userId = tokenStore.get(token);
            return userRepository.findById(userId).orElse(null);
        }

        // 2. Check Session
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute(SESSION_USER_ID);
            if (userId != null) {
                return userRepository.findById(userId).orElse(null);
            }
        }

        return null;
    }

    public User resolveUserByTokenOrSession(String token, HttpSession session) {
        if (token != null && tokenStore.containsKey(token)) {
            Long userId = tokenStore.get(token);
            return userRepository.findById(userId).orElse(null);
        }
        if (session != null) {
            Long userId = (Long) session.getAttribute(SESSION_USER_ID);
            if (userId != null) {
                return userRepository.findById(userId).orElse(null);
            }
        }
        return null;
    }

    public void authenticateAs(User user, String token) {
        if (user != null && token != null) {
            tokenStore.put(token, user.getId());
        }
    }
}
