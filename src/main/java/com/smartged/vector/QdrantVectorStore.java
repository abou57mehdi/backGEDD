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
		Map<String, Object> point = new HashMap<>();
		point.put("id", documentId.toString() + "_chunk_" + chunkIndex);
		point.put("vector", vector);
		Map<String, Object> payload = new HashMap<>();
		payload.put("document_id", documentId.toString());
		payload.put("chunk_index", chunkIndex);
		payload.put("text", text);
		point.put("payload", payload);
		Map<String, Object> body = new HashMap<>();
		body.put("points", List.of(point));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		restTemplate.postForEntity(url, request, Map.class);
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> search(List<Float> queryVector, int limit) {
		if (!enabled) return List.of();
		String url = baseUrl + "/collections/" + collection + "/points/search";
		Map<String, Object> body = new HashMap<>();
		body.put("vector", queryVector);
		body.put("limit", limit);
		body.put("with_payload", true);
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


