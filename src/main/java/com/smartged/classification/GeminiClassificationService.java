package com.smartged.classification;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GeminiClassificationService {

	private final boolean enabled;
	private final String model;
	private final Client client;

	public GeminiClassificationService(
			@Value("${app.llm.enabled:false}") boolean enabled,
			@Value("${app.llm.gemini.model:gemini-2.5-flash}") String model
	) {
		this.enabled = enabled;
		this.model = model;
		this.client = new Client(); // reads GEMINI_API_KEY from env
	}

	public boolean isEnabled() {
		return enabled;
	}

	@SuppressWarnings("unchecked")
	public ClassificationService.Result classify(String text) {
		if (!enabled) {
			return null;
		}
		String truncated = text;
		if (truncated != null && truncated.length() > 8000) {
			truncated = truncated.substring(0, 8000);
		}
		String prompt = """
		You are an information extraction assistant. Analyze the following document content and return a concise JSON with fields:
		- document_type: string (e.g., Report, Contract, Invoice, Article)
		- country: string or null
		- topics: array of strings (3-8 concise tags)
		- summary: 2-3 sentence summary
		- entities: object with arrays like { organizations:[], people:[], dates:[], amounts:[] } (omit empty arrays)
		
		Document content:
		""".concat(truncated);

		GenerateContentResponse response = client.models.generateContent(
				model,
				prompt,
				null
		);
		String textResp = response.text();
		// Best effort JSON extraction: assume the model returns pure JSON
		ClassificationService.Result result = new ClassificationService.Result();
		try {
			// Simple parse using org.json to avoid adding heavy libs; fallback to heuristics otherwise
			org.json.JSONObject obj = new org.json.JSONObject(textResp);
			result.documentType = obj.optString("document_type", null);
			result.country = obj.optString("country", null);
			result.topics = obj.has("topics") ? obj.getJSONArray("topics").toList().stream().map(Object::toString).toList() : List.of();
			result.summary = obj.optString("summary", null);
			if (obj.has("entities")) {
				result.entitiesJson = obj.getJSONObject("entities").toString();
			}
			return result;
		} catch (Exception ignore) {
			// If the model didn't return raw JSON, attempt to find the first JSON object substring
			try {
				int start = textResp.indexOf('{');
				int end = textResp.lastIndexOf('}');
				if (start >= 0 && end > start) {
					String json = textResp.substring(start, end + 1);
					org.json.JSONObject obj = new org.json.JSONObject(json);
					ClassificationService.Result r = new ClassificationService.Result();
					r.documentType = obj.optString("document_type", null);
					r.country = obj.optString("country", null);
					r.topics = obj.has("topics") ? obj.getJSONArray("topics").toList().stream().map(Object::toString).toList() : List.of();
					r.summary = obj.optString("summary", null);
					if (obj.has("entities")) {
						r.entitiesJson = obj.getJSONObject("entities").toString();
					}
					return r;
				}
			} catch (Exception ignored) { }
			return null;
		}
	}
}


