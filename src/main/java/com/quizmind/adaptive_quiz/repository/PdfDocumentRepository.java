package com.quizmind.adaptive_quiz.repository;

import com.quizmind.adaptive_quiz.model.PdfDocument;
import com.quizmind.adaptive_quiz.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {
    List<PdfDocument> findAllByOrderByUploadTimestampDesc();
    List<PdfDocument> findByUploadedByIdOrderByUploadTimestampDesc(Long userId);
    List<PdfDocument> findByUploadedBy_RoleOrderByUploadTimestampDesc(UserRole role);
    boolean existsByFilenameAndFileSize(String filename, Long fileSize);
    Optional<PdfDocument> findByFilenameAndFileSize(String filename, Long fileSize);
    boolean existsByFilenameAndFileSizeAndUploadedById(String filename, Long fileSize, Long userId);
    boolean existsByFilenameAndFileSizeAndUploadedByIsNull(String filename, Long fileSize);
}
