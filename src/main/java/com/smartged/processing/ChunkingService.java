package com.smartged.processing;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

	public List<String> splitIntoChunks(String text, int maxChars) {
		List<String> chunks = new ArrayList<>();
		if (text == null || text.isEmpty()) return chunks;
		String[] sentences = text.split("(?<=[\\.\\!\\?])\\s+");
		StringBuilder current = new StringBuilder();
		for (String s : sentences) {
			if (current.length() + s.length() + 1 > maxChars) {
				if (current.length() > 0) {
					chunks.add(current.toString().trim());
					current.setLength(0);
				}
			}
			if (s.length() > maxChars) {
				// Hard split overly long sentences
				int start = 0;
				while (start < s.length()) {
					int end = Math.min(start + maxChars, s.length());
					chunks.add(s.substring(start, end).trim());
					start = end;
				}
			} else {
				current.append(s).append(' ');
			}
		}
		if (current.length() > 0) {
			chunks.add(current.toString().trim());
		}
		return chunks;
	}
}


