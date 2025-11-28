package com.smartged.document;

import com.smartged.user.UserEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {

	@Id
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(nullable = false)
	private String filename;

	@Column(nullable = false)
	private long sizeBytes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DocumentStatus status = DocumentStatus.processing;

	@Column(nullable = false)
	private Instant uploadedAt = Instant.now();

	private Instant processedAt;

	private Long processingDurationSeconds;

	private String documentType;

	private String country;

	@Column(length = 2000)
	private String summary;

	@Column(columnDefinition = "text")
	private String entitiesJson;
	@Column
	private Long extractedChars;

	@Column(length = 1024)
	private String errorMessage;
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "document_auto_tags", joinColumns = @JoinColumn(name = "document_id"))
	@Column(name = "tag")
	private List<String> autoTags = new ArrayList<>();

	@Column(length = 512)
	private String storagePath;

	@ManyToOne(optional = false, fetch = FetchType.EAGER)
	@JoinColumn(name = "uploaded_by_user_id")
	private UserEntity uploadedBy;

	public DocumentEntity() {
		this.id = UUID.randomUUID();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public void setSizeBytes(long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public void setStatus(DocumentStatus status) {
		this.status = status;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(Instant uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public Instant getProcessedAt() {
		return processedAt;
	}

	public void setProcessedAt(Instant processedAt) {
		this.processedAt = processedAt;
	}

	public Long getProcessingDurationSeconds() {
		return processingDurationSeconds;
	}

	public void setProcessingDurationSeconds(Long processingDurationSeconds) {
		this.processingDurationSeconds = processingDurationSeconds;
	}

	public Long getExtractedChars() {
		return extractedChars;
	}

	public void setExtractedChars(Long extractedChars) {
		this.extractedChars = extractedChars;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getEntitiesJson() {
		return entitiesJson;
	}

	public void setEntitiesJson(String entitiesJson) {
		this.entitiesJson = entitiesJson;
	}
	public List<String> getAutoTags() {
		return autoTags;
	}

	public void setAutoTags(List<String> autoTags) {
		this.autoTags = autoTags;
	}

	public UserEntity getUploadedBy() {
		return uploadedBy;
	}

	public void setUploadedBy(UserEntity uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public String getStoragePath() {
		return storagePath;
	}

	public void setStoragePath(String storagePath) {
		this.storagePath = storagePath;
	}
}


