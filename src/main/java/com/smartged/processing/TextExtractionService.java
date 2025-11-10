package com.smartged.processing;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TextExtractionService {

	private final Tika tika = new Tika();

	public String extractFromPath(String storagePath) throws IOException {
		try (var in = Files.newInputStream(Path.of(storagePath))) {
			return tika.parseToString(in);
		}
	}
}


