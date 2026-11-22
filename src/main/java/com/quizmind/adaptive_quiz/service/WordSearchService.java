package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.model.dto.*;
import com.quizmind.adaptive_quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WordSearchService {

    private final QuestionRepository questionRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final WordSearchPuzzleRepository puzzleRepository;
    private final WordSearchAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final LearningSessionRepository learningSessionRepository;

    private static final int[][] DIRECTIONS = {
            {0, 1},   // Horizontal right
            {0, -1},  // Horizontal left
            {1, 0},   // Vertical down
            {-1, 0},  // Vertical up
            {1, 1},   // Diagonal down-right
            {-1, 1},  // Diagonal up-right
            {1, -1},  // Diagonal down-left
            {-1, -1}  // Diagonal up-left
    };

    @Autowired
    public WordSearchService(QuestionRepository questionRepository,
                             PdfDocumentRepository pdfDocumentRepository,
                             WordSearchPuzzleRepository puzzleRepository,
                             WordSearchAttemptRepository attemptRepository,
                             UserRepository userRepository,
                             LearningSessionRepository learningSessionRepository) {
        this.questionRepository = questionRepository;
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.puzzleRepository = puzzleRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.learningSessionRepository = learningSessionRepository;
    }

    /**
     * Interactive Question-by-Question Word Search Initialization.
     * Generates letter grid from approved answers, persists puzzle and session.
     */
    @Transactional
    public WordSearchInitResponse createWordSearchSession(Long documentId, String studentName) {
        PdfDocument document = null;
        List<GeneratedQuestion> candidateQuestions;

        if (documentId != null) {
            document = pdfDocumentRepository.findById(documentId).orElse(null);
            if (document != null && document.getUploadedBy() != null && document.getUploadedBy().getRole() == com.quizmind.adaptive_quiz.model.UserRole.STUDENT) {
                if (studentName != null && !studentName.equalsIgnoreCase(document.getUploadedBy().getUsername())) {
                    throw new IllegalArgumentException("Access denied: You cannot access another student's private study material.");
                }
                candidateQuestions = questionRepository.findByDocumentId(documentId);
            } else {
                candidateQuestions = questionRepository.findByDocumentIdAndApprovalStatus(documentId, ApprovalStatus.APPROVED);
            }
        } else {
            candidateQuestions = questionRepository.findSharedApprovedQuestions();
            if (candidateQuestions.isEmpty()) {
                candidateQuestions = questionRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
            }
        }

        if (candidateQuestions.isEmpty()) {
            throw new IllegalArgumentException("No approved questions are available for this puzzle. Please generate or approve questions first.");
        }

        // Filter and select candidate questions with good single/multi-word concepts
        Map<String, GeneratedQuestion> chosenQuestions = new LinkedHashMap<>();
        for (GeneratedQuestion q : candidateQuestions) {
            String sanitized = sanitizeWord(q.getCorrectAnswer());
            if (sanitized.length() >= 3 && sanitized.length() <= 16 && !chosenQuestions.containsKey(sanitized)) {
                chosenQuestions.put(sanitized, q);
                if (chosenQuestions.size() >= 10) break;
            }
        }

        if (chosenQuestions.isEmpty()) {
            if (documentId != null) {
                throw new IllegalArgumentException("The selected study material has no questions suitable for Word Search (answers must be 3-16 alphabetic characters). You can practice all of these questions in the Normal Quiz!");
            }
            // Seed a fallback question if database has no valid concepts (only for default shared course materials)
            GeneratedQuestion fallback = new GeneratedQuestion(
                    "What is the system software that manages computer hardware and software resources?",
                    "Operating System",
                    List.of("Operating System", "Compiler", "Driver", "Kernel"),
                    "Operating Systems",
                    Difficulty.EASY,
                    QuestionType.MCQ,
                    "An operating system manages computer hardware and software resources.",
                    "An operating system provides an abstraction layer over hardware.",
                    ApprovalStatus.APPROVED
            );
            fallback = questionRepository.save(fallback);
            chosenQuestions.put(sanitizeWord(fallback.getCorrectAnswer()), fallback);
        }

        // Generate grid with all words placed
        List<Map.Entry<String, GeneratedQuestion>> entries = new ArrayList<>(chosenQuestions.entrySet());
        int maxLen = entries.stream().mapToInt(e -> e.getKey().length()).max().orElse(8);
        int initialGridSize = Math.max(12, maxLen + 3);

        GridGenerationResult gridResult = buildGridWithPlacements(entries, initialGridSize);

        // Persist WordSearchPuzzle
        String gridLetters = WordSearchPuzzle.fromGridArray(gridResult.grid, gridResult.size);
        WordSearchPuzzle puzzle = new WordSearchPuzzle(gridResult.size, gridResult.gridLetters, document);
        puzzle.setGridLetters(gridLetters);

        for (PlacedWord pw : gridResult.placements) {
            WordPlacement placement = new WordPlacement(
                    puzzle,
                    pw.question,
                    pw.word,
                    pw.startRow,
                    pw.startCol,
                    pw.endRow,
                    pw.endCol,
                    pw.dRow,
                    pw.dCol,
                    pw.questionOrder
            );
            puzzle.getPlacements().add(placement);
        }

        puzzle = puzzleRepository.save(puzzle);

        // Create new attempt session for the student
        String sessionToken = UUID.randomUUID().toString();
        User user = (studentName != null) ? userRepository.findByUsernameIgnoreCase(studentName.trim()).orElse(null) : null;
        WordSearchAttempt attempt = new WordSearchAttempt(
                puzzle,
                studentName != null ? studentName : "Student",
                sessionToken,
                puzzle.getPlacements().size()
        );
        if (user != null) {
            attempt.setUser(user);
        }
        attempt = attemptRepository.save(attempt);

        if (user != null) {
            LearningSession ls = new LearningSession(sessionToken, user, LearningSession.SessionType.WORD_SEARCH, "Word Search Puzzle", documentId);
            ls.setTotalQuestions(puzzle.getPlacements().size());
            learningSessionRepository.save(ls);
        }

        // Build Response (First Question ONLY, answer strictly hidden)
        WordSearchInitResponse response = new WordSearchInitResponse();
        response.setPuzzleId(puzzle.getId());
        response.setSessionToken(sessionToken);
        response.setSize(puzzle.getSize());
        response.setGrid(puzzle.toGridArray());
        response.setTotalQuestions(puzzle.getPlacements().size());
        response.setCurrentQuestionIndex(0);
        response.setCompletedCount(0);
        response.setCompletedWords(new ArrayList<>());

        if (!puzzle.getPlacements().isEmpty()) {
            GeneratedQuestion firstQ = puzzle.getPlacements().get(0).getQuestion();
            response.setCurrentQuestion(QuestionDto.forStudent(firstQ));
        }

        return response;
    }

    /**
     * Validates student word selection against current question.
     */
    @Transactional
    public WordSearchValidationResponse validateWordSelection(WordSearchValidationRequest request) {
        return validateWordSelection(request, null);
    }

    @Transactional
    public WordSearchValidationResponse validateWordSelection(WordSearchValidationRequest request, User user) {
        WordSearchValidationResponse response = new WordSearchValidationResponse();

        WordSearchAttempt attempt = null;
        if (request.getSessionToken() != null && !request.getSessionToken().trim().isEmpty()) {
            attempt = attemptRepository.findBySessionToken(request.getSessionToken().trim()).orElse(null);
        }
        if (attempt == null && request.getPuzzleId() != null) {
            WordSearchPuzzle p = puzzleRepository.findById(request.getPuzzleId()).orElse(null);
            if (p != null) {
                // Recover or create attempt
                String studentName = (user != null) ? user.getUsername() : "Student";
                attempt = new WordSearchAttempt(p, studentName, UUID.randomUUID().toString(), p.getPlacements().size());
                if (user != null) {
                    attempt.setUser(user);
                }
                attempt = attemptRepository.save(attempt);
            }
        }

        if (attempt == null) {
            response.setCorrect(false);
            response.setMessage("Puzzle session not found. Please regenerate or start a new puzzle.");
            return response;
        }

        // Verify session ownership if authenticated
        if (user != null && attempt.getUser() != null) {
            if (!attempt.getUser().getId().equals(user.getId())) {
                response.setCorrect(false);
                response.setMessage("Access denied: You do not own this puzzle session.");
                return response;
            }
        }

        WordSearchPuzzle puzzle = attempt.getPuzzle();
        List<WordPlacement> placements = puzzle.getPlacements();
        int currentIndex = attempt.getCurrentQuestionIndex();

        response.setTotalQuestions(placements.size());
        response.setCompletedCount(attempt.getCorrectCount());
        response.setCurrentQuestionIndex(currentIndex);

        if (currentIndex >= placements.size() || attempt.isCompleted()) {
            response.setCorrect(false);
            response.setPuzzleCompleted(true);
            response.setMessage("Puzzle is already completed!");
            return response;
        }

        WordPlacement targetPlacement = placements.get(currentIndex);
        String expectedWord = targetPlacement.getWord();

        // Verify question ID if provided by client
        if (request.getQuestionId() != null && targetPlacement.getQuestion() != null) {
            if (!request.getQuestionId().equals(targetPlacement.getQuestion().getId())) {
                response.setCorrect(false);
                response.setMessage("Selection does not correspond to the current active question.");
                return response;
            }
        }

        char[][] grid = puzzle.toGridArray();
        List<CoordinateDto> coords = request.getCoordinates();

        // Reconstruct coordinates from startRow/Col and endRow/Col if coords was omitted
        if ((coords == null || coords.isEmpty()) && request.getStartRow() != null && request.getStartCol() != null
                && request.getEndRow() != null && request.getEndCol() != null) {
            coords = buildStraightLineCoords(request.getStartRow(), request.getStartCol(), request.getEndRow(), request.getEndCol(), grid.length);
        }

        String selectedSanitized = sanitizeWord(request.getSelectedWord());

        if (coords != null && coords.size() >= 2) {
            boolean straightLine = validateStraightLine(coords, grid.length);
            if (!straightLine) {
                response.setCorrect(false);
                response.setMessage("Invalid selection: Letters must form a straight line (horizontal, vertical, or diagonal).");
                attempt.setIncorrectAttempts(attempt.getIncorrectAttempts() + 1);
                attemptRepository.save(attempt);
                return response;
            }

            // Verify characters spelled by coordinates
            StringBuilder spelled = new StringBuilder();
            for (CoordinateDto c : coords) {
                if (c.getRow() >= 0 && c.getRow() < grid.length && c.getCol() >= 0 && c.getCol() < grid.length) {
                    spelled.append(grid[c.getRow()][c.getCol()]);
                }
            }
            selectedSanitized = spelled.toString();
        }

        // Compare with current question's expected answer (forward or reverse straight line)
        boolean isMatch = selectedSanitized.equals(expectedWord)
                || new StringBuilder(selectedSanitized).reverse().toString().equals(expectedWord);

        if (isMatch) {
            // Correct Answer!
            attempt.getCompletedWords().add(expectedWord);
            attempt.setCorrectCount(attempt.getCorrectCount() + 1);

            // Coordinates for the placed word to allow permanent frontend highlighting
            List<CoordinateDto> placedCoords = new ArrayList<>();
            int r = targetPlacement.getStartRow();
            int c = targetPlacement.getStartCol();
            for (int i = 0; i < expectedWord.length(); i++) {
                placedCoords.add(new CoordinateDto(r, c));
                r += targetPlacement.getdRow();
                c += targetPlacement.getdCol();
            }

            response.setCorrect(true);
            response.setMessage("Correct Answer!");
            response.setFoundWord(expectedWord);
            response.setCoordinates(placedCoords);
            response.setQuestionCompleted(true);

            int nextIndex = currentIndex + 1;
            attempt.setCurrentQuestionIndex(nextIndex);

            if (nextIndex >= placements.size()) {
                // All questions completed!
                attempt.setCompleted(true);
                attempt.setEndTime(LocalDateTime.now());
                int duration = (int) Duration.between(attempt.getStartTime(), attempt.getEndTime()).getSeconds();
                attempt.setDurationSeconds(Math.max(1, duration));

                response.setPuzzleCompleted(true);
                response.setNextQuestion(null);

                int totalAttempts = attempt.getCorrectCount() + attempt.getIncorrectAttempts();
                double accuracy = totalAttempts > 0 ? (attempt.getCorrectCount() * 100.0 / totalAttempts) : 100.0;

                Map<String, Object> stats = new HashMap<>();
                stats.put("totalQuestions", placements.size());
                stats.put("correctAnswers", attempt.getCorrectCount());
                stats.put("incorrectAttempts", attempt.getIncorrectAttempts());
                stats.put("durationSeconds", attempt.getDurationSeconds());
                stats.put("accuracy", Math.round(accuracy * 10.0) / 10.0);
                stats.put("completionPercentage", 100.0);
                response.setCompletionStats(stats);
            } else {
                response.setPuzzleCompleted(false);
                GeneratedQuestion nextQ = placements.get(nextIndex).getQuestion();
                response.setNextQuestion(QuestionDto.forStudent(nextQ));
            }

            attemptRepository.save(attempt);
            response.setCurrentQuestionIndex(nextIndex);
            response.setCompletedCount(attempt.getCorrectCount());

            // Sync LearningSession
            if (attempt.getSessionToken() != null) {
                Optional<LearningSession> optLs = learningSessionRepository.findBySessionId(attempt.getSessionToken());
                if (optLs.isPresent()) {
                    LearningSession ls = optLs.get();
                    ls.setCompletedQuestions(nextIndex);
                    ls.setScore(attempt.getCorrectCount() * 10);
                    ls.setIncorrectAttempts(attempt.getIncorrectAttempts());
                    ls.setLastActiveTime(LocalDateTime.now());
                    if (attempt.isCompleted()) {
                        ls.setCompleted(true);
                        ls.setEndTime(attempt.getEndTime());
                        ls.setDurationSeconds(attempt.getDurationSeconds());
                    }
                    learningSessionRepository.save(ls);
                }
            }

        } else {
            // Wrong Answer!
            attempt.setIncorrectAttempts(attempt.getIncorrectAttempts() + 1);
            attemptRepository.save(attempt);

            response.setCorrect(false);
            response.setMessage("Wrong Answer! Try Again.");
            response.setQuestionCompleted(false);
            response.setPuzzleCompleted(false);
            // DO NOT include correct answer!
        }

        return response;
    }

    /**
     * Get Current Active Question for a Word Search Session.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentQuestion(String sessionToken) {
        WordSearchAttempt attempt = attemptRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionToken));

        List<WordPlacement> placements = attempt.getPuzzle().getPlacements();
        int index = attempt.getCurrentQuestionIndex();

        Map<String, Object> map = new HashMap<>();
        map.put("sessionToken", sessionToken);
        map.put("totalQuestions", placements.size());
        map.put("currentQuestionIndex", index);
        map.put("completedCount", attempt.getCorrectCount());
        map.put("completedWords", new ArrayList<>(attempt.getCompletedWords()));
        map.put("puzzleCompleted", attempt.isCompleted() || index >= placements.size());

        if (index < placements.size() && !attempt.isCompleted()) {
            GeneratedQuestion q = placements.get(index).getQuestion();
            map.put("question", QuestionDto.forStudent(q));
            map.put("questionNumber", index + 1);
        } else {
            map.put("question", null);
        }

        return map;
    }

    /**
     * Get Progress of Word Search Session.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSessionProgress(String sessionToken) {
        WordSearchAttempt attempt = attemptRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionToken));

        Map<String, Object> map = new HashMap<>();
        map.put("sessionToken", sessionToken);
        map.put("totalQuestions", attempt.getTotalQuestions());
        map.put("currentQuestionIndex", attempt.getCurrentQuestionIndex());
        map.put("correctCount", attempt.getCorrectCount());
        map.put("incorrectAttempts", attempt.getIncorrectAttempts());
        map.put("completedWords", new ArrayList<>(attempt.getCompletedWords()));
        map.put("completed", attempt.isCompleted());
        map.put("durationSeconds", attempt.getDurationSeconds());
        return map;
    }

    /**
     * Legacy / Direct Puzzle Generator for backward compatibility.
     */
    public PuzzleDataDto generatePuzzle(Long documentId) {
        WordSearchInitResponse init = createWordSearchSession(documentId, "Student");
        List<WordClueDto> clues = new ArrayList<>();
        WordSearchPuzzle p = puzzleRepository.findById(init.getPuzzleId()).orElse(null);
        if (p != null) {
            for (WordPlacement pl : p.getPlacements()) {
                clues.add(new WordClueDto(pl.getWord(), pl.getQuestion().getQuestionText(), pl.getQuestion().getTopic(), pl.getQuestion().getId()));
            }
        }
        return new PuzzleDataDto(init.getSize(), init.getGrid(), clues);
    }

    // ==========================================
    // GRID GENERATION ENGINE
    // ==========================================

    private static class PlacedWord {
        String word;
        GeneratedQuestion question;
        int startRow;
        int startCol;
        int endRow;
        int endCol;
        int dRow;
        int dCol;
        int questionOrder;
    }

    private static class GridGenerationResult {
        int size;
        char[][] grid;
        String gridLetters;
        List<PlacedWord> placements;
    }

    private GridGenerationResult buildGridWithPlacements(List<Map.Entry<String, GeneratedQuestion>> entries, int startSize) {
        int currentSize = startSize;
        Random rng = new Random();

        while (currentSize <= 28) {
            char[][] grid = new char[currentSize][currentSize];
            List<PlacedWord> placements = new ArrayList<>();
            boolean allPlaced = true;

            int order = 0;
            for (Map.Entry<String, GeneratedQuestion> entry : entries) {
                String word = entry.getKey();
                PlacedWord placed = tryPlaceWordWithCoordinates(grid, currentSize, word, entry.getValue(), order++, rng);
                if (placed == null) {
                    allPlaced = false;
                    break;
                }
                placements.add(placed);
            }

            if (allPlaced) {
                // Fill empty cells with random uppercase English letters 'A'-'Z'
                for (int r = 0; r < currentSize; r++) {
                    for (int c = 0; c < currentSize; c++) {
                        if (grid[r][c] == '\0') {
                            grid[r][c] = (char) ('A' + rng.nextInt(26));
                        }
                    }
                }
                GridGenerationResult result = new GridGenerationResult();
                result.size = currentSize;
                result.grid = grid;
                result.gridLetters = WordSearchPuzzle.fromGridArray(grid, currentSize);
                result.placements = placements;
                return result;
            }

            currentSize += 2; // Dynamic expansion
        }

        // Emergency fallback grid
        GridGenerationResult fallback = new GridGenerationResult();
        fallback.size = startSize;
        fallback.grid = new char[startSize][startSize];
        for (int r = 0; r < startSize; r++) {
            for (int c = 0; c < startSize; c++) {
                fallback.grid[r][c] = (char) ('A' + rng.nextInt(26));
            }
        }
        fallback.gridLetters = WordSearchPuzzle.fromGridArray(fallback.grid, startSize);
        fallback.placements = new ArrayList<>();
        return fallback;
    }

    private PlacedWord tryPlaceWordWithCoordinates(char[][] grid, int size, String word, GeneratedQuestion question, int order, Random rng) {
        int maxAttempts = 300;
        int wordLen = word.length();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int dirIndex = rng.nextInt(DIRECTIONS.length);
            int dRow = DIRECTIONS[dirIndex][0];
            int dCol = DIRECTIONS[dirIndex][1];

            int startRow = rng.nextInt(size);
            int startCol = rng.nextInt(size);

            int endRow = startRow + (wordLen - 1) * dRow;
            int endCol = startCol + (wordLen - 1) * dCol;

            if (endRow < 0 || endRow >= size || endCol < 0 || endCol >= size) {
                continue;
            }

            boolean canPlace = true;
            for (int i = 0; i < wordLen; i++) {
                int r = startRow + i * dRow;
                int c = startCol + i * dCol;
                char existing = grid[r][c];
                if (existing != '\0' && existing != word.charAt(i)) {
                    canPlace = false;
                    break;
                }
            }

            if (canPlace) {
                for (int i = 0; i < wordLen; i++) {
                    int r = startRow + i * dRow;
                    int c = startCol + i * dCol;
                    grid[r][c] = word.charAt(i);
                }

                PlacedWord pw = new PlacedWord();
                pw.word = word;
                pw.question = question;
                pw.startRow = startRow;
                pw.startCol = startCol;
                pw.endRow = endRow;
                pw.endCol = endCol;
                pw.dRow = dRow;
                pw.dCol = dCol;
                pw.questionOrder = order;
                return pw;
            }
        }
        return null;
    }

    private boolean validateStraightLine(List<CoordinateDto> coords, int gridSize) {
        if (coords.size() < 2) return true;

        CoordinateDto first = coords.get(0);
        CoordinateDto second = coords.get(1);

        int dRow = Integer.compare(second.getRow(), first.getRow());
        int dCol = Integer.compare(second.getCol(), first.getCol());

        if (dRow == 0 && dCol == 0) return false;

        for (int i = 1; i < coords.size(); i++) {
            CoordinateDto prev = coords.get(i - 1);
            CoordinateDto curr = coords.get(i);

            if (curr.getRow() < 0 || curr.getRow() >= gridSize || curr.getCol() < 0 || curr.getCol() >= gridSize) {
                return false;
            }
            if (curr.getRow() - prev.getRow() != dRow || curr.getCol() - prev.getCol() != dCol) {
                return false;
            }
        }
        return true;
    }

    private List<CoordinateDto> buildStraightLineCoords(int r1, int c1, int r2, int c2, int gridSize) {
        int dr = r2 - r1;
        int dc = c2 - c1;
        int absR = Math.abs(dr);
        int absC = Math.abs(dc);

        if (absR != 0 && absC != 0 && absR != absC) {
            return Collections.emptyList();
        }

        int stepR = Integer.compare(dr, 0);
        int stepC = Integer.compare(dc, 0);
        int steps = Math.max(absR, absC);

        List<CoordinateDto> list = new ArrayList<>();
        int curR = r1;
        int curC = c1;
        for (int i = 0; i <= steps; i++) {
            if (curR < 0 || curR >= gridSize || curC < 0 || curC >= gridSize) {
                return Collections.emptyList();
            }
            list.add(new CoordinateDto(curR, curC));
            curR += stepR;
            curC += stepC;
        }
        return list;
    }

    private String sanitizeWord(String answer) {
        if (answer == null) return "";
        return answer.replaceAll("[^a-zA-Z]", "").toUpperCase();
    }
}
