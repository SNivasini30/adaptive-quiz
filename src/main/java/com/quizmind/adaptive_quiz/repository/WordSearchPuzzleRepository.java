package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.WordSearchPuzzle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WordSearchPuzzleRepository extends JpaRepository<WordSearchPuzzle, Long> {
    Optional<WordSearchPuzzle> findFirstByOrderByCreatedAtDesc();
    List<WordSearchPuzzle> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
