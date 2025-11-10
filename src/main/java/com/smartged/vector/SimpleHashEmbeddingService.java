package com.smartged.vector;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

@Service
public class SimpleHashEmbeddingService implements EmbeddingService {

	private static final int DIM = 128;

	@Override
	public List<Float> embed(String text) {
		// Simple, deterministic hash-based embedding for scaffolding purposes
		byte[] bytes = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
		long h1 = 1125899906842597L; // prime
		for (byte b : bytes) {
			h1 = 31 * h1 + b;
		}
		RandomGenerator rng = RandomGenerator.of("L64X128MixRandom").jump(h1);
		List<Float> vec = new ArrayList<>(DIM);
		for (int i = 0; i < DIM; i++) {
			vec.add((float) (rng.nextDouble() * 2 - 1));
		}
		return vec;
	}
}


