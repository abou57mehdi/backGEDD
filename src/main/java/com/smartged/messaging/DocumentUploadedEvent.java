package com.smartged.messaging;

import java.io.Serializable;
import java.util.UUID;

public class DocumentUploadedEvent implements Serializable {
	public UUID documentId;
	public String storagePath;
	public String filename;
	public long sizeBytes;

	public DocumentUploadedEvent() {}

	public DocumentUploadedEvent(UUID documentId, String storagePath, String filename, long sizeBytes) {
		this.documentId = documentId;
		this.storagePath = storagePath;
		this.filename = filename;
		this.sizeBytes = sizeBytes;
	}
}


