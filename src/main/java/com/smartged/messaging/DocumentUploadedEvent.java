package com.smartged.messaging;

import java.io.Serializable;
import java.util.UUID;

public record DocumentUploadedEvent(
		UUID documentId,
		String storagePath,
		String filename,
		long sizeBytes
) implements Serializable {}


