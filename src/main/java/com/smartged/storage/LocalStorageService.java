package com.smartged.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

	private final Path rootDir;

	public LocalStorageService(@Value("${app.storage.local.root:./data/storage}") String rootDir) throws IOException {
		this.rootDir = Path.of(rootDir).toAbsolutePath().normalize();
		Files.createDirectories(this.rootDir);
	}

	@Override
	public String save(UUID documentId, MultipartFile file) throws IOException {
		String original = file.getOriginalFilename();
		String ext = "";
		if (StringUtils.hasText(original) && original.contains(".")) {
			ext = original.substring(original.lastIndexOf('.'));
		}
		String filename = documentId.toString() + ext;
		Path target = this.rootDir.resolve(filename).normalize();
		Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		return target.toString();
	}
}


