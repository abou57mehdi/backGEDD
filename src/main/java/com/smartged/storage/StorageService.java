package com.smartged.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface StorageService {
	String save(UUID documentId, MultipartFile file) throws IOException;
}


