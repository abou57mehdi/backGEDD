package com.smartged.document;

import com.smartged.user.UserEntity;
import com.smartged.user.UserRepository;
import com.smartged.storage.StorageService;
import com.smartged.messaging.DocumentEventPublisher;
import com.smartged.messaging.DocumentUploadedEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final UserRepository userRepository;
	private final StorageService storageService;
	private final DocumentEventPublisher eventPublisher;

	public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, StorageService storageService, DocumentEventPublisher eventPublisher) {
		this.documentRepository = documentRepository;
		this.userRepository = userRepository;
		this.storageService = storageService;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public DocumentEntity createOnUpload(UserDetails principal, MultipartFile file) throws IOException {
		UserEntity user = userRepository.findByUsername(principal.getUsername())
				.orElseThrow(() -> new IllegalStateException("Uploader not found"));

		DocumentEntity doc = new DocumentEntity();
		doc.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
		doc.setSizeBytes(file.getSize());
		doc.setStatus(DocumentStatus.processing);
		doc.setUploadedBy(user);
		// First save to obtain ID then store the file deterministically by documentId
		DocumentEntity saved = documentRepository.save(doc);
		String storagePath = storageService.save(saved.getId(), file);
		saved.setStoragePath(storagePath);
		DocumentEntity withPath = documentRepository.save(saved);
		// Publish event for async processing
		eventPublisher.publish(new DocumentUploadedEvent(withPath.getId(), storagePath, withPath.getFilename(), withPath.getSizeBytes()));
		return withPath;
	}

	@Transactional(readOnly = true)
	public DocumentEntity getById(UUID id) {
		return documentRepository.findById(id).orElseThrow();
	}

	@Transactional(readOnly = true)
	public java.util.List<DocumentEntity> getAll() {
		return documentRepository.findAll();
	}
}


