package com.smartged.document.dto;

import com.smartged.document.DocumentEntity;
import com.smartged.document.DocumentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DocumentResponse {
	public UUID id;
	public String filename;
	public long sizeBytes;
	public DocumentStatus status;
	public Instant uploadedAt;
	public Instant processedAt;
	public String documentType;
	public String country;
	public List<String> autoTags;

	public static DocumentResponse from(DocumentEntity e) {
		DocumentResponse r = new DocumentResponse();
		r.id = e.getId();
		r.filename = e.getFilename();
		r.sizeBytes = e.getSizeBytes();
		r.status = e.getStatus();
		r.uploadedAt = e.getUploadedAt();
		r.processedAt = e.getProcessedAt();
		r.documentType = e.getDocumentType();
		r.country = e.getCountry();
		r.autoTags = e.getAutoTags();
		return r;
	}
}


