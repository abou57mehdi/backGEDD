package com.smartged.search;

import com.smartged.vector.EmbeddingService;
import com.smartged.vector.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class SearchService {

	private final boolean keywordEnabled;
	private final String esUrl;
	private final String esIndex;
	private final int keywordLimit;
	private final int vectorLimit;
	private final int smartTopK;
	private final EmbeddingService embeddingService;
	private final QdrantVectorStore vectorStore;
	private final RestTemplate restTemplate = new RestTemplate();

	public SearchService(
			@Value("${app.search.enabled:false}") boolean keywordEnabled,
			@Value("${app.search.elasticsearch.url:http://localhost:9200}") String esUrl,
			@Value("${app.search.elasticsearch.index:documents}") String esIndex,
			@Value("${app.search.keyword.limit:15}") int keywordLimit,
			@Value("${app.search.vector.limit:20}") int vectorLimit,
			@Value("${app.search.smart.topk:5}") int smartTopK,
			EmbeddingService embeddingService,
			QdrantVectorStore vectorStore
	) {
		this.keywordEnabled = keywordEnabled;
		this.esUrl = esUrl;
		this.esIndex = esIndex;
		this.keywordLimit = keywordLimit;
		this.vectorLimit = vectorLimit;
		this.smartTopK = smartTopK;
		this.embeddingService = embeddingService;
		this.vectorStore = vectorStore;
	}

	public Map<String, Object> keywordSearch(String query) {
		if (!keywordEnabled) {
			return Map.of("hits", List.of());
		}
		try {
			String url = esUrl + "/" + esIndex + "/_search";
			Map<String, Object> mm = new HashMap<>();
			mm.put("query", query);
			mm.put("fields", List.of("full_text", "topics", "country"));
			Map<String, Object> body = new HashMap<>();
			body.put("query", Map.of("multi_match", mm));
			body.put("size", keywordLimit);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			Map<String, Object> result = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
			return result != null ? result : Map.of("hits", List.of());
		} catch (Exception e) {
			System.err.println("Error in keyword search: " + e.getMessage());
			return Map.of("hits", List.of());
		}
	}

	public Map<String, Object> smartSearch(String query) {
		try {
			List<Float> qvec = embeddingService.embed(query);
			List<Map<String, Object>> vectorResults = vectorStore.search(qvec, vectorLimit);
			if (vectorResults == null) vectorResults = List.of();

			Map<String, Object> keywordResp = keywordSearch(query);
			List<Map<String, Object>> keywordResults = extractEsHits(keywordResp);
			if (keywordResults == null) keywordResults = List.of();

			List<Map<String, Object>> merged = rrfMerge(vectorResults, keywordResults);
			List<Map<String, Object>> topk = merged.size() > 0 ?
				merged.subList(0, Math.min(smartTopK, merged.size())) :
				List.of();

			Map<String, Object> answer = Map.of(
					"text", "Smart search results generated. Enable LLM to produce answers.",
					"confidence", 0.0
			);
			return Map.of(
					"search_type", "smart_search_with_rag",
					"query", query,
					"ai_answer", answer,
					"source_documents", topk
			);
		} catch (Exception e) {
			System.err.println("Error in smart search: " + e.getMessage());
			e.printStackTrace();
			return Map.of(
					"search_type", "smart_search_with_rag",
					"query", query,
					"ai_answer", Map.of("text", "Error occurred during search", "confidence", 0.0),
					"source_documents", List.of(),
					"error", e.getMessage()
			);
		}
	}

	private List<Map<String, Object>> extractEsHits(Map<String, Object> esResp) {
		if (esResp == null) return List.of();
		Object hitsObj = esResp.get("hits");
		if (hitsObj == null || !(hitsObj instanceof Map<?, ?> hits)) return List.of();
		Object arr = hits.get("hits");
		if (arr == null || !(arr instanceof List<?> list)) return List.of();
		List<Map<String, Object>> results = new ArrayList<>();
		for (Object h : list) {
			if (h instanceof Map<?, ?> hm) {
				Object srcObj = hm.get("_source");
				if (srcObj instanceof Map<?, ?> src) {
					Map<String, Object> m = new HashMap<>();
					m.put("document_id", src.getOrDefault("id", null));
					m.put("filename", src.getOrDefault("filename", null));
					m.put("text", src.getOrDefault("full_text", null));
					results.add(m);
				}
			}
		}
		return results;
	}

	private List<Map<String, Object>> rrfMerge(List<Map<String, Object>> vectorResults, List<Map<String, Object>> keywordResults) {
		Map<String, Double> scores = new HashMap<>();
		Map<String, Map<String, Object>> docMap = new HashMap<>();
		int k = 60;
		if (vectorResults != null) {
			for (int i = 0; i < vectorResults.size(); i++) {
				Map<String, Object> r = vectorResults.get(i);
				if (r == null) continue;

				Object payloadObj = r.get("payload");
				String docId = null;
				String filename = null;
				String text = null;

				if (payloadObj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> payload = (Map<String, Object>) payloadObj;
					docId = payload.get("document_id") != null ? String.valueOf(payload.get("document_id")) : null;
					filename = payload.get("filename") != null ? String.valueOf(payload.get("filename")) : null;
					text = payload.get("text") != null ? String.valueOf(payload.get("text")) : null;
				}

				if (docId == null) continue;
				scores.put(docId, scores.getOrDefault(docId, 0.0) + 1.0 / (k + i + 1));
				Map<String, Object> doc = new HashMap<>();
				doc.put("document_id", docId);
				doc.put("filename", filename);
				doc.put("text", text);
				docMap.putIfAbsent(docId, doc);
			}
		}
		if (keywordResults != null) {
			for (int i = 0; i < keywordResults.size(); i++) {
				Map<String, Object> r = keywordResults.get(i);
				String docId = r != null && r.get("document_id") != null ? String.valueOf(r.get("document_id")) : null;
				if (docId == null) continue;
				scores.put(docId, scores.getOrDefault(docId, 0.0) + 1.0 / (k + i + 1));
				docMap.putIfAbsent(docId, r);
			}
		}
		if (scores.isEmpty()) return List.of();

		List<Map.Entry<String, Double>> sorted = new ArrayList<>(scores.entrySet());
		sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
		List<Map<String, Object>> merged = new ArrayList<>();
		for (var e : sorted) {
			if (e.getKey() != null && docMap.containsKey(e.getKey())) {
				Map<String, Object> base = new HashMap<>(docMap.get(e.getKey()));
				base.put("rrf_score", e.getValue());
				merged.add(base);
			}
		}
		return merged;
	}
}


