package com.smartged.vector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class QdrantVectorStore {

	private final RestTemplate restTemplate = new RestTemplate();
	private final boolean enabled;
	private final String baseUrl;
	private final String collection;

	public QdrantVectorStore(
			@Value("${app.vector.enabled:false}") boolean enabled,
			@Value("${app.vector.qdrant.url:http://localhost:6333}") String baseUrl,
			@Value("${app.vector.qdrant.collection:documents}") String collection
	) {
		this.enabled = enabled;
		this.baseUrl = baseUrl;
		this.collection = collection;
	}

	public void upsertChunk(UUID documentId, int chunkIndex, String text, List<Float> vector) {
		if (!enabled) return;

		String url = baseUrl + "/collections/" + collection + "/points";

		// Build payload
		Map<String, Object> payload = new HashMap<>();
		payload.put("document_id", documentId.toString());
		payload.put("chunk_index", chunkIndex);
		payload.put("text", text);

		// Create a single point in the exact format Qdrant expects (from documentation)
		Map<String, Object> point = new HashMap<>();
		// Use a simple numeric ID to ensure Qdrant compatibility
		// Combine document hash and chunk index into a single positive integer
		long uniqueId = Math.abs(documentId.getMostSignificantBits()) % 1000000L * 1000L + chunkIndex;
		point.put("id", uniqueId);
		point.put("vector", vector);
		point.put("payload", payload);

		// Wrap in points array as shown in the Qdrant documentation
		Map<String, Object> body = new HashMap<>();
		body.put("points", List.of(point));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

		// Use PUT method for upsert operation as per Qdrant documentation
		restTemplate.put(url, request, Map.class);
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> search(List<Float> queryVector, int limit) {
		if (!enabled) return List.of();
		
		String url = baseUrl + "/collections/" + collection + "/points/search";
		
		Map<String, Object> body = new HashMap<>();
		body.put("vector", queryVector);
		body.put("limit", limit);
		body.put("with_payload", true);
		body.put("with_vector", false); // Usually don't need vectors in search results
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		
		Map<?, ?> resp = restTemplate.postForObject(url, request, Map.class);
		
		if (resp == null) return List.of();
		
		Object result = resp.get("result");
		if (result instanceof List<?> list) {
			return (List<Map<String, Object>>) (List<?>) list;
		}
		
		return List.of();
	}
}