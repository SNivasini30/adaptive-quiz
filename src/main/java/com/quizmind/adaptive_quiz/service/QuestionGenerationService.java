package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionGenerationService {

    private final QuestionRepository questionRepository;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "but", "if", "because", "as", "what", "which",
            "this", "that", "these", "those", "then", "just", "so", "than", "such",
            "both", "through", "about", "for", "is", "of", "while", "during", "to",
            "from", "in", "out", "on", "off", "over", "under", "again", "further",
            "then", "once", "here", "there", "when", "where", "why", "how", "all",
            "any", "both", "each", "few", "more", "most", "other", "some", "such",
            "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
            "can", "will", "just", "don", "should", "now", "are", "was", "were", "be",
            "been", "being", "have", "has", "had", "having", "do", "does", "did", "doing",
            "it", "its", "they", "them", "their", "we", "us", "our", "you", "your", "he", "him", "she", "her"
    ));

    private static final List<String> FALLBACK_CONCEPTS = Arrays.asList(
            "Encapsulation", "Polymorphism", "Inheritance", "Abstraction",
            "Operating System", "Deadlock", "Virtual Memory", "Paging",
            "Binary Search", "Linked List", "Queue", "Stack", "Recursion",
            "TCP/IP", "DNS", "HTTP Protocol", "Load Balancer", "Thread Synchronization"
    );

    @Autowired
    public QuestionGenerationService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public int generateQuestionsFromSentences(List<String> sentences, PdfDocument document) {
        if (sentences == null || sentences.isEmpty()) {
            return 0;
        }

        // 1. First pass: extract concept candidate pool from all sentences
        List<String> conceptPool = extractConceptPool(sentences);

        Set<String> seenQuestionTexts = new HashSet<>();
        List<GeneratedQuestion> generatedList = new ArrayList<>();

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.length() < 25) continue;

            // Try definition pattern matching
            ConceptMatch match = matchDefinitionPattern(trimmedSentence);

            if (match != null && isValidConcept(match.concept)) {
                String cleanConcept = cleanConceptName(match.concept);
                if (cleanConcept.length() >= 3 && !STOP_WORDS.contains(cleanConcept.toLowerCase())) {
                    String topic = inferTopic(cleanConcept, trimmedSentence);
                    Difficulty difficulty = calculateDifficulty(trimmedSentence);

                    // 1. Generate Fill-In-The-Blank Question
                    String blankQuestionText = createBlankQuestion(trimmedSentence, match.concept);
                    if (blankQuestionText != null && !seenQuestionTexts.contains(blankQuestionText.toLowerCase())) {
                        seenQuestionTexts.add(blankQuestionText.toLowerCase());

                        GeneratedQuestion blankQ = new GeneratedQuestion(
                                blankQuestionText,
                                cleanConcept,
                                Collections.emptyList(),
                                topic,
                                difficulty,
                                QuestionType.FILL_IN_BLANK,
                                trimmedSentence,
                                "From context: " + trimmedSentence,
                                ApprovalStatus.GENERATED
                        );
                        blankQ.setDocument(document);
                        generatedList.add(blankQ);
                    }

                    // 2. Generate Multiple Choice Question (MCQ)
                    List<String> distractors = generateDistractors(cleanConcept, conceptPool, topic);
                    List<String> options = new ArrayList<>();
                    options.add(cleanConcept);
                    options.addAll(distractors);
                    // Shuffle deterministically based on sentence length/hash
                    Collections.shuffle(options, new Random(cleanConcept.hashCode()));

                    String mcqQuestionText = createMcqQuestion(trimmedSentence, match.concept, match.predicate);
                    if (mcqQuestionText != null && !seenQuestionTexts.contains(mcqQuestionText.toLowerCase())) {
                        seenQuestionTexts.add(mcqQuestionText.toLowerCase());

                        GeneratedQuestion mcqQ = new GeneratedQuestion(
                                mcqQuestionText,
                                cleanConcept,
                                options,
                                topic,
                                difficulty,
                                QuestionType.MCQ,
                                trimmedSentence,
                                "Definition: " + trimmedSentence,
                                ApprovalStatus.GENERATED
                        );
                        mcqQ.setDocument(document);
                        generatedList.add(mcqQ);
                    }
                }
            }
        }

        // Save generated questions to database
        if (!generatedList.isEmpty()) {
            questionRepository.saveAll(generatedList);
        }

        return generatedList.size();
    }

    private static class ConceptMatch {
        String concept;
        String predicate;

        ConceptMatch(String concept, String predicate) {
            this.concept = concept;
            this.predicate = predicate;
        }
    }

    private ConceptMatch matchDefinitionPattern(String sentence) {
        // Pattern 1: X is defined as Y / X is known as Y / X is referred to as Y
        Pattern p1 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:is|are)\\s+(?:defined as|known as|referred to as)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(sentence);
        if (m1.find()) {
            return new ConceptMatch(m1.group(1).trim(), m1.group(2).trim());
        }

        // Pattern 2: X refers to Y
        Pattern p2 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+refers to\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(sentence);
        if (m2.find()) {
            return new ConceptMatch(m2.group(1).trim(), m2.group(2).trim());
        }

        // Pattern 3: X is a/an [type/process/concept] of/for/that Y
        Pattern p3 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:is|are)\\s+(?:a|an|the)\\s+([a-z]+(?:\\s+[a-z]+)?\\s+(?:of|for|that|which|to)\\s+.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(sentence);
        if (m3.find()) {
            return new ConceptMatch(m3.group(1).trim(), m3.group(2).trim());
        }

        // Pattern 4: X is used to/for Y
        Pattern p4 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:is|are)\\s+(?:used|utilized|designed)\\s+(?:for|to)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m4 = p4.matcher(sentence);
        if (m4.find()) {
            return new ConceptMatch(m4.group(1).trim(), m4.group(2).trim());
        }

        // Pattern 5: X is/are [noun phrase]
        Pattern p5 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,30}?)\\s+(?:is|are)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m5 = p5.matcher(sentence);
        if (m5.find()) {
            String concept = m5.group(1).trim();
            String predicate = m5.group(2).trim();
            if (concept.split("\\s+").length <= 4 && predicate.length() > 15) {
                return new ConceptMatch(concept, predicate);
            }
        }

        // Pattern 6: Capitalized term followed by provides/manages/enables/implements
        Pattern p6 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,30}?)\\s+(?:provides|manages|enables|implements|ensures|represents)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m6 = p6.matcher(sentence);
        if (m6.find()) {
            return new ConceptMatch(m6.group(1).trim(), m6.group(2).trim());
        }

        return null;
    }

    private String cleanConceptName(String raw) {
        String cleaned = raw.replaceAll("^(The|A|An|In|On|By|Under|For)\\s+", "").trim();
        // Capitalize words nicely
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    sb.append(words[i].substring(1));
                }
                if (i < words.length - 1) sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean isValidConcept(String concept) {
        if (concept == null) return false;
        String trimmed = concept.trim();
        if (trimmed.length() < 3 || trimmed.length() > 35) return false;
        // Don't accept phrases that contain too many words
        if (trimmed.split("\\s+").length > 4) return false;
        // Don't accept generic pronouns or numbers
        if (trimmed.matches(".*\\d{3,}.*")) return false;
        return !STOP_WORDS.contains(trimmed.toLowerCase());
    }

    private String createBlankQuestion(String sentence, String concept) {
        // Replace first occurrence of concept in sentence with "________"
        String regex = "(?i)\\b" + Pattern.quote(concept) + "\\b";
        String replaced = sentence.replaceFirst(regex, "________");
        if (!replaced.contains("________")) {
            // Prepend blank if replacement didn't match word boundary
            replaced = "________ " + sentence.substring(Math.min(sentence.length(), concept.length())).trim();
        }
        return replaced;
    }

    private String createMcqQuestion(String sentence, String concept, String predicate) {
        if (predicate != null && predicate.length() > 10) {
            return "Which concept or term matches: \"" + predicate + "\"?";
        }
        return "Which term completes the statement: \"" + createBlankQuestion(sentence, concept) + "\"?";
    }

    private List<String> extractConceptPool(List<String> sentences) {
        Set<String> pool = new LinkedHashSet<>();
        // Extract capitalized technical terms (2 to 3 words or camelCase)
        Pattern termPattern = Pattern.compile("\\b([A-Z][a-zA-Z0-9]+(?:\\s+[A-Z][a-zA-Z0-9]+)?)\\b");

        for (String s : sentences) {
            Matcher m = termPattern.matcher(s);
            while (m.find()) {
                String term = m.group(1).trim();
                if (term.length() >= 3 && term.length() <= 30 && !STOP_WORDS.contains(term.toLowerCase())) {
                    pool.add(cleanConceptName(term));
                }
            }
        }
        return new ArrayList<>(pool);
    }

    private List<String> generateDistractors(String correctConcept, List<String> conceptPool, String topic) {
        List<String> distractors = new ArrayList<>();
        Set<String> chosen = new HashSet<>();
        chosen.add(correctConcept.toLowerCase());

        // 1. Pick from candidate pool first
        for (String c : conceptPool) {
            if (distractors.size() >= 3) break;
            if (!chosen.contains(c.toLowerCase()) && !c.equalsIgnoreCase(correctConcept) && Math.abs(c.length() - correctConcept.length()) < 15) {
                distractors.add(c);
                chosen.add(c.toLowerCase());
            }
        }

        // 2. Fall back to curated technical concepts if needed
        for (String fallback : FALLBACK_CONCEPTS) {
            if (distractors.size() >= 3) break;
            if (!chosen.contains(fallback.toLowerCase()) && !fallback.equalsIgnoreCase(correctConcept)) {
                distractors.add(fallback);
                chosen.add(fallback.toLowerCase());
            }
        }

        return distractors;
    }

    public String inferTopic(String concept, String sentence) {
        String text = (concept + " " + sentence).toLowerCase();
        if (text.contains("class") || text.contains("object") || text.contains("inheritance") ||
            text.contains("polymorphism") || text.contains("encapsulation") || text.contains("interface") ||
            text.contains("oop") || text.contains("method")) {
            return "Object-Oriented Programming";
        }
        if (text.contains("process") || text.contains("thread") || text.contains("deadlock") ||
            text.contains("paging") || text.contains("memory") || text.contains("cpu") ||
            text.contains("scheduling") || text.contains("operating system")) {
            return "Operating Systems";
        }
        if (text.contains("network") || text.contains("packet") || text.contains("tcp") ||
            text.contains("ip") || text.contains("router") || text.contains("protocol") ||
            text.contains("dns") || text.contains("http")) {
            return "Computer Networks";
        }
        if (text.contains("array") || text.contains("linked list") || text.contains("tree") ||
            text.contains("graph") || text.contains("stack") || text.contains("queue") ||
            text.contains("hash") || text.contains("binary search") || text.contains("algorithm")) {
            return "Data Structures";
        }
        if (text.contains("database") || text.contains("sql") || text.contains("table") ||
            text.contains("query") || text.contains("relation") || text.contains("schema")) {
            return "Database Systems";
        }

        return cleanConceptName(concept);
    }

    public Difficulty calculateDifficulty(String sentence) {
        int wordCount = sentence.split("\\s+").length;
        String lower = sentence.toLowerCase();

        boolean hasComplexTerms = lower.contains("synchronization") || lower.contains("consequently") ||
                lower.contains("architecture") || lower.contains("hierarchical") || lower.contains("asynchronous") ||
                lower.contains("polymorphic") || lower.contains("preemptive");

        if (wordCount > 25 || hasComplexTerms) {
            return Difficulty.HARD;
        } else if (wordCount > 15) {
            return Difficulty.MEDIUM;
        } else {
            return Difficulty.EASY;
        }
    }
}
