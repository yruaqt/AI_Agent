package com.lanyuan.starter.knowledge;

import com.lanyuan.starter.common.exception.BusinessException;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.common.web.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** 知识文档上传、查询、重新处理和删除的业务边界。 */
@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeFileStorage storage;
    private final KnowledgeDocumentProcessor processor;
    private final CurrentUser currentUser;

    public KnowledgeDocumentService(KnowledgeDocumentRepository documentRepository,
                                    KnowledgeChunkRepository chunkRepository,
                                    KnowledgeFileStorage storage,
                                    KnowledgeDocumentProcessor processor,
                                    CurrentUser currentUser) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.storage = storage;
        this.processor = processor;
        this.currentUser = currentUser;
    }

    public KnowledgeDocument upload(MultipartFile file,
                                    String title,
                                    String sourceOrganization,
                                    LocalDate publishDate,
                                    String region,
                                    String phenology,
                                    String documentType) {
        requireAdmin();
        KnowledgeFileStorage.StoredFile stored = storage.store(file);
        try {
            KnowledgeDocument document = new KnowledgeDocument();
            document.setTitle(title.trim());
            document.setSourceOrganization(sourceOrganization.trim());
            document.setPublishDate(publishDate);
            document.setRegion(blankToNull(region));
            document.setPhenology(blankToNull(phenology));
            document.setDocumentType(documentType.trim());
            document.setOriginalName(stored.originalName());
            document.setFileType(stored.fileType());
            document.setStoragePath(stored.storagePath());
            document.setStatus(DocumentStatus.PENDING);
            KnowledgeDocument saved = documentRepository.saveAndFlush(document);
            processor.processAsync(saved.getId());
            return saved;
        } catch (RuntimeException ex) {
            storage.delete(stored.storagePath());
            throw ex;
        }
    }

    public Page<KnowledgeDocument> list(DocumentStatus status, String keyword, Pageable pageable) {
        requireAdmin();
        return documentRepository.findWithFilters(status, blankToNull(keyword), pageable);
    }

    public KnowledgeDocument detail(Long documentId) {
        requireAdmin();
        return find(documentId);
    }

    public KnowledgeDocument reindex(Long documentId) {
        requireAdmin();
        KnowledgeDocument document = find(documentId);
        if (document.getStatus() == DocumentStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.CONFLICT, "文档正在处理中，请稍后重试");
        }
        document.setStatus(DocumentStatus.PENDING);
        document.setFailureReason(null);
        documentRepository.save(document);
        processor.processAsync(documentId);
        return document;
    }

    public void delete(Long documentId) {
        requireAdmin();
        KnowledgeDocument document = find(documentId);
        if (document.getStatus() == DocumentStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.CONFLICT, "文档正在处理中，暂不能删除");
        }
        chunkRepository.deleteByDocumentId(documentId);
        storage.delete(document.getStoragePath());
        document.setDeleted(true);
        documentRepository.save(document);
    }

    private KnowledgeDocument find(Long documentId) {
        return documentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识文档不存在"));
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可以管理知识库");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
