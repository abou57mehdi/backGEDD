package com.smartged.search;

import com.smartged.document.DocumentEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchIndexService {

	private final boolean enabled;
	private final String baseUrl;
	private final String indexName;
	private final RestTemplate restTemplate = new RestTemplate();

	public SearchIndexService(
			@Value("${app.search.enabled:false}") boolean enabled,
			@Value("${app.search.elasticsearch.url:http://localhost:9200}") String baseUrl,
			@Value("${app.search.elasticsearch.index:documents}") String indexName
	) {
		this.enabled = enabled;
		this.baseUrl = baseUrl;
		this.indexName = indexName;
	}

	public void index(DocumentEntity doc, String fullText) {
		if (!enabled) return;
		String url = baseUrl + "/" + indexName + "/_doc/" + doc.getId();
		Map<String, Object> body = new HashMap<>();
		body.put("id", doc.getId().toString());
		body.put("filename", doc.getFilename());
		body.put("full_text", fullText);
		body.put("document_type", doc.getDocumentType());
		body.put("country", doc.getCountry());
		body.put("topics", doc.getAutoTags() != null ? doc.getAutoTags() : List.of());
		body.put("uploaded_at", doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : null);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		restTemplate.put(url, new HttpEntity<>(body, headers));
	}
}


