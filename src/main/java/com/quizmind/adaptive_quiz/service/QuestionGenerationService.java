package com.quizmind.adaptive_quiz.service;

import com.quizmind.adaptive_quiz.model.*;
import com.quizmind.adaptive_quiz.repository.PdfDocumentRepository;
import com.quizmind.adaptive_quiz.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QuestionGenerationService {

    private final QuestionRepository questionRepository;
    private final PdfDocumentRepository pdfDocumentRepository;
    private final PdfTextExtractionService pdfTextExtractionService;

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

    private static final List<String> OS_FALLBACKS = Arrays.asList(
            "Operating System", "Process Scheduling", "Virtual Memory", "Deadlock",
            "Paging", "Thread Synchronization", "File System", "Kernel",
            "Interrupt Handling", "Device Driver", "System Call", "Context Switching"
    );

    private static final List<String> OOP_FALLBACKS = Arrays.asList(
            "Encapsulation", "Polymorphism", "Inheritance", "Abstraction",
            "Interface", "Method Overriding", "Class", "Object", "Constructor", "Access Modifier"
    );

    private static final List<String> DSA_FALLBACKS = Arrays.asList(
            "Binary Search", "Linked List", "Queue", "Stack", "Recursion",
            "Binary Tree", "Hash Table", "Graph Traversal", "Sorting Algorithm", "Dynamic Programming"
    );

    private static final List<String> NETWORK_FALLBACKS = Arrays.asList(
            "TCP/IP Protocol", "DNS", "HTTP Protocol", "Router", "Subnet Mask",
            "Packet Switching", "Transport Layer", "Firewall", "Port", "Bandwidth"
    );

    private static final List<String> DBMS_FALLBACKS = Arrays.asList(
            "Relational Database", "SQL Query", "Primary Key", "Foreign Key",
            "Normalization", "Transaction ACID", "Indexing", "Entity Relationship", "Stored Procedure", "Database Schema"
    );

    private static final List<String> GENERAL_FALLBACKS = Arrays.asList(
            "Compiler", "Interpreter", "Algorithm Complexity", "Cache Memory",
            "Software Architecture", "Data Structure", "Network Protocol", "Memory Management"
    );

    @Autowired
    public QuestionGenerationService(QuestionRepository questionRepository,
                                     PdfDocumentRepository pdfDocumentRepository,
                                     PdfTextExtractionService pdfTextExtractionService) {
        this.questionRepository = questionRepository;
        this.pdfDocumentRepository = pdfDocumentRepository;
        this.pdfTextExtractionService = pdfTextExtractionService;
    }

    /**
     * Generates questions for a stored PdfDocument from its extracted text.
     */
    @Transactional
    public List<GeneratedQuestion> generateQuestionsForDocument(PdfDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Document not found.");
        }

        String content = document.getExtractedText();
        if (content == null || content.trim().isEmpty()) {
            content = document.getRawTextPreview();
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Insufficient readable content to generate meaningful questions. Please upload a more detailed PDF.");
        }

        List<String> sentences = pdfTextExtractionService.splitIntoSentences(content);
        if (sentences.size() < 2) {
            throw new IllegalArgumentException("Insufficient readable content to generate meaningful questions. Please upload a more detailed PDF.");
        }

        // Delete any existing questions for this document to avoid duplicates on regeneration
        List<GeneratedQuestion> oldQuestions = questionRepository.findByDocumentId(document.getId());
        if (!oldQuestions.isEmpty()) {
            questionRepository.deleteAll(oldQuestions);
        }

        List<GeneratedQuestion> generated = generateFromSentencesInternal(sentences, document);
        if (generated.isEmpty()) {
            throw new IllegalArgumentException("Insufficient readable content to generate meaningful questions. Please upload a more detailed PDF.");
        }

        document.setGeneratedQuestionCount(generated.size());
        document.setStatus("PROCESSED");
        pdfDocumentRepository.save(document);

        return generated;
    }

    public int generateQuestionsFromSentences(List<String> sentences, PdfDocument document) {
        List<GeneratedQuestion> questions = generateFromSentencesInternal(sentences, document);
        return questions.size();
    }

    /**
     * Internal question generation with strict answer diversity, grammatical correctness,
     * factual grounding, and distractor plausibility.
     */
    private List<GeneratedQuestion> generateFromSentencesInternal(List<String> sentences, PdfDocument document) {
        if (sentences == null || sentences.isEmpty()) {
            return Collections.emptyList();
        }

        // Determine initial approval status:
        // Teacher uploads start as PENDING for teacher review & approval.
        // Student uploads (and unassociated test docs) are immediately APPROVED.
        ApprovalStatus initialStatus = ApprovalStatus.APPROVED;
        if (document != null && document.getUploadedBy() != null && document.getUploadedBy().getRole() == UserRole.TEACHER) {
            initialStatus = ApprovalStatus.PENDING;
        }

        // 1. Scan all sentences and collect concept matches
        Map<String, List<ConceptMatch>> conceptMatches = new LinkedHashMap<>();
        List<String> documentConceptPool = new ArrayList<>();
        Set<String> seenNormConcepts = new HashSet<>();

        for (String rawSentence : sentences) {
            String trimmedSentence = rawSentence.trim();
            if (trimmedSentence.length() < 20) continue;

            ConceptMatch match = matchDefinitionPattern(trimmedSentence);
            if (match != null && isValidConcept(match.concept)) {
                String cleanConcept = cleanConceptName(match.concept);
                String normConcept = normalizeConcept(cleanConcept);

                if (cleanConcept.length() >= 3 && !STOP_WORDS.contains(cleanConcept.toLowerCase())) {
                    match.concept = cleanConcept;
                    conceptMatches.computeIfAbsent(normConcept, k -> new ArrayList<>()).add(match);

                    if (!seenNormConcepts.contains(normConcept)) {
                        seenNormConcepts.add(normConcept);
                        documentConceptPool.add(cleanConcept);
                    }
                }
            }
        }

        if (conceptMatches.isEmpty()) {
            return Collections.emptyList();
        }

        List<GeneratedQuestion> generatedList = new ArrayList<>();
        Set<String> seenQuestionSignatures = new HashSet<>();
        int totalConcepts = conceptMatches.size();
        int conceptIndex = 0;

        // 2. Iterate through distinct concepts to guarantee ANSWER DIVERSITY and TOPIC COVERAGE
        for (Map.Entry<String, List<ConceptMatch>> entry : conceptMatches.entrySet()) {
            List<ConceptMatch> matches = entry.getValue();
            if (matches.isEmpty()) continue;

            // Pick the best match for this concept (longest/richest predicate)
            ConceptMatch bestMatch = matches.stream()
                    .max(Comparator.comparingInt(m -> m.predicate != null ? m.predicate.length() : 0))
                    .orElse(matches.get(0));

            String concept = bestMatch.concept;
            String sentence = bestMatch.sourceSentence;
            String predicate = bestMatch.predicate != null ? bestMatch.predicate.replaceAll("[.]+$", "").trim() : "";
            String topic = inferTopic(concept, sentence);
            Difficulty difficulty = calculateDifficulty(sentence);

            // Determine question types to generate for this concept
            // For rich documents (>= 6 distinct concepts): 1 or at most 2 questions per concept, rotating types
            // For documents with fewer concepts (< 6): 2 questions per concept (MCQ + Fill-in-the-blank)
            int questionsForThisConcept = (totalConcepts >= 8) ? 1 : (totalConcepts >= 4 ? 2 : 2);

            // A. Primary Question: Multiple Choice Question (MCQ) if we can form 3 distinct distractors
            List<String> distractors = generateDistractors(concept, documentConceptPool, topic);
            if (distractors.size() == 3) {
                List<String> options = new ArrayList<>();
                options.add(concept);
                options.addAll(distractors);
                Collections.shuffle(options, new Random((long) concept.hashCode() * 31 + conceptIndex));

                String mcqText = createMcqQuestion(sentence, concept, predicate);
                String sig = "MCQ:" + mcqText.toLowerCase();
                if (!seenQuestionSignatures.contains(sig)) {
                    seenQuestionSignatures.add(sig);

                    GeneratedQuestion mcqQ = new GeneratedQuestion(
                            mcqText,
                            concept,
                            options,
                            topic,
                            difficulty,
                            QuestionType.MCQ,
                            sentence,
                            "Based on source text: " + sentence,
                            initialStatus
                    );
                    mcqQ.setDocument(document);
                    generatedList.add(mcqQ);
                }
            }

            // B. Secondary Question: Fill-In-The-Blank Question
            if (questionsForThisConcept > 1 || distractors.size() < 3) {
                String blankText = createBlankQuestion(sentence, concept);
                String sig = "BLANK:" + blankText.toLowerCase();
                if (!seenQuestionSignatures.contains(sig)) {
                    seenQuestionSignatures.add(sig);

                    GeneratedQuestion blankQ = new GeneratedQuestion(
                            blankText,
                            concept,
                            Collections.emptyList(),
                            topic,
                            difficulty,
                            QuestionType.FILL_IN_BLANK,
                            sentence,
                            "Context: " + sentence,
                            initialStatus
                    );
                    blankQ.setDocument(document);
                    generatedList.add(blankQ);
                }
            }

            // C. If the document has very few concepts, also add Definition or Concept ID for full practice
            if (totalConcepts < 4 && predicate.length() > 15) {
                String defText = createDefinitionQuestion(concept, predicate);
                String sig = "DEF:" + defText.toLowerCase();
                if (!seenQuestionSignatures.contains(sig)) {
                    seenQuestionSignatures.add(sig);

                    GeneratedQuestion defQ = new GeneratedQuestion(
                            defText,
                            concept,
                            Collections.emptyList(),
                            topic,
                            difficulty,
                            QuestionType.DEFINITION,
                            sentence,
                            "Definition: " + sentence,
                            initialStatus
                    );
                    defQ.setDocument(document);
                    generatedList.add(defQ);
                }
            }

            conceptIndex++;
        }

        // Save generated questions to repository
        if (!generatedList.isEmpty()) {
            return questionRepository.saveAll(generatedList);
        }
        return Collections.emptyList();
    }

    private static class ConceptMatch {
        String concept;
        String predicate;
        String sourceSentence;

        ConceptMatch(String concept, String predicate, String sourceSentence) {
            this.concept = concept;
            this.predicate = predicate;
            this.sourceSentence = sourceSentence;
        }
    }

    private ConceptMatch matchDefinitionPattern(String sentence) {
        String s = sentence.trim();

        // Pattern 1: X is defined as Y / X is known as Y / X is referred to as Y
        Pattern p1 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:is|are)\\s+(?:defined as|known as|referred to as)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(s);
        if (m1.find()) {
            return new ConceptMatch(m1.group(1).trim(), m1.group(2).trim(), s);
        }

        // Pattern 2: X refers to Y
        Pattern p2 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+refers to\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(s);
        if (m2.find()) {
            return new ConceptMatch(m2.group(1).trim(), m2.group(2).trim(), s);
        }

        // Pattern 3: X is a/an [type/process/concept] of/for/that/which Y
        Pattern p3 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:is|are)\\s+(?:a|an|the)\\s+([a-z]+(?:\\s+[a-z]+)?\\s+(?:of|for|that|which|to)\\s+.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(s);
        if (m3.find()) {
            return new ConceptMatch(m3.group(1).trim(), m3.group(2).trim(), s);
        }

        // Pattern 4: X is used to/for / designed to/for / responsible for Y
        Pattern p4 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:is|are)\\s+(?:used|utilized|designed|responsible)\\s+(?:for|to)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m4 = p4.matcher(s);
        if (m4.find()) {
            return new ConceptMatch(m4.group(1).trim(), m4.group(2).trim(), s);
        }

        // Pattern 5: X consists of / comprises / is composed of Y
        Pattern p5 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,35}?)\\s+(?:consists of|comprises|is composed of)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m5 = p5.matcher(s);
        if (m5.find()) {
            return new ConceptMatch(m5.group(1).trim(), m5.group(2).trim(), s);
        }

        // Pattern 6: Capitalized term followed by provides/manages/enables/implements/ensures/represents/allows
        Pattern p6 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,30}?)\\s+(?:provides|manages|enables|implements|ensures|represents|allows|serves as|acts as)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m6 = p6.matcher(s);
        if (m6.find()) {
            return new ConceptMatch(m6.group(1).trim(), m6.group(2).trim(), s);
        }

        // Pattern 7: X is/are [noun phrase]
        Pattern p7 = Pattern.compile("^([A-Z][A-Za-z0-9\\s\\-]{1,30}?)\\s+(?:is|are)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m7 = p7.matcher(s);
        if (m7.find()) {
            String concept = m7.group(1).trim();
            String predicate = m7.group(2).trim();
            if (concept.split("\\s+").length <= 4 && predicate.length() > 15) {
                return new ConceptMatch(concept, predicate, s);
            }
        }

        return null;
    }

    private String cleanConceptName(String raw) {
        String cleaned = raw.replaceAll("(?i)^(The|A|An|In|On|By|Under|For|With)\\s+", "").trim();
        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                sb.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    sb.append(words[i].substring(1).toLowerCase());
                }
                if (i < words.length - 1) sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String normalizeConcept(String concept) {
        if (concept == null) return "";
        return concept.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private boolean isValidConcept(String concept) {
        if (concept == null) return false;
        String trimmed = concept.trim();
        if (trimmed.length() < 3 || trimmed.length() > 35) return false;
        if (trimmed.split("\\s+").length > 4) return false;
        if (trimmed.matches(".*\\d{3,}.*")) return false;
        return !STOP_WORDS.contains(trimmed.toLowerCase());
    }

    private String createBlankQuestion(String sentence, String concept) {
        String regex = "(?i)\\b" + Pattern.quote(concept) + "\\b";
        String replaced = sentence.replaceFirst(regex, "______");
        if (!replaced.contains("______")) {
            replaced = "______ " + sentence.substring(Math.min(sentence.length(), concept.length())).trim();
        }
        return replaced;
    }

    private String createDefinitionQuestion(String concept, String predicate) {
        if (predicate != null && predicate.length() > 10) {
            String cleanPred = predicate.replaceAll("[.]+$", "").trim();
            return "Which concept or term is defined as: \"" + cleanPred + "\"?";
        }
        return "What is the primary role of " + cleanConceptName(concept) + "?";
    }

    private String createMcqQuestion(String sentence, String concept, String predicate) {
        if (predicate != null && predicate.length() > 12) {
            return "Which concept or term matches the description: \"" + predicate + "\"?";
        }
        return "Which term correctly completes the statement: \"" + createBlankQuestion(sentence, concept) + "\"?";
    }

    /**
     * Generates 3 distinct, plausible, domain-relevant distractors.
     * Prioritizes other distinct concepts extracted from the same document first,
     * then supplements from curated domain fallbacks.
     */
    private List<String> generateDistractors(String correctConcept, List<String> documentConceptPool, String topic) {
        List<String> distractors = new ArrayList<>();
        Set<String> chosenNormalized = new HashSet<>();

        String correctNorm = normalizeConcept(correctConcept);
        chosenNormalized.add(correctNorm);

        // 1. Pick from candidate concepts in the same document
        if (documentConceptPool != null) {
            for (String candidate : documentConceptPool) {
                if (distractors.size() >= 3) break;
                String candNorm = normalizeConcept(candidate);
                if (!chosenNormalized.contains(candNorm) && !candidate.equalsIgnoreCase(correctConcept)) {
                    // Avoid partial substring confusion (e.g., "System" vs "Operating System")
                    if (!candNorm.contains(correctNorm) && !correctNorm.contains(candNorm)) {
                        distractors.add(candidate);
                        chosenNormalized.add(candNorm);
                    }
                }
            }
        }

        // 2. Supplement from domain-matched fallback pool
        List<String> topicFallbacks = getFallbacksForTopic(topic);
        for (String fallback : topicFallbacks) {
            if (distractors.size() >= 3) break;
            String fbNorm = normalizeConcept(fallback);
            if (!chosenNormalized.contains(fbNorm) && !fallback.equalsIgnoreCase(correctConcept)) {
                if (!fbNorm.contains(correctNorm) && !correctNorm.contains(fbNorm)) {
                    distractors.add(fallback);
                    chosenNormalized.add(fbNorm);
                }
            }
        }

        // 3. Supplement from general fallbacks if still < 3
        for (String general : GENERAL_FALLBACKS) {
            if (distractors.size() >= 3) break;
            String genNorm = normalizeConcept(general);
            if (!chosenNormalized.contains(genNorm) && !general.equalsIgnoreCase(correctConcept)) {
                if (!genNorm.contains(correctNorm) && !correctNorm.contains(genNorm)) {
                    distractors.add(general);
                    chosenNormalized.add(genNorm);
                }
            }
        }

        return distractors;
    }

    private List<String> getFallbacksForTopic(String topic) {
        if (topic == null) return GENERAL_FALLBACKS;
        String t = topic.toLowerCase();
        if (t.contains("operating") || t.contains("system")) return OS_FALLBACKS;
        if (t.contains("object") || t.contains("oop")) return OOP_FALLBACKS;
        if (t.contains("data") || t.contains("algorithm")) return DSA_FALLBACKS;
        if (t.contains("network")) return NETWORK_FALLBACKS;
        if (t.contains("database") || t.contains("sql")) return DBMS_FALLBACKS;
        return GENERAL_FALLBACKS;
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

