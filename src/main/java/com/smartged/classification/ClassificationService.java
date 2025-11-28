package com.smartged.classification;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClassificationService {

	public Result classify(String text) {
		Result r = new Result();
		if (text == null) {
			r.documentType = "Unknown";
			r.country = null;
			r.topics = List.of();
			return r;
		}
		String lower = text.toLowerCase();
		// Extremely simple heuristics as placeholder for LLM
		if (lower.contains("contract") || lower.contains("agreement")) r.documentType = "Contract";
		else if (lower.contains("invoice")) r.documentType = "Invoice";
		else if (lower.contains("report")) r.documentType = "Report";
		else r.documentType = "Document";

		if (lower.contains("morocco")) r.country = "Morocco";
		else if (lower.contains("tunisia")) r.country = "Tunisia";
		else if (lower.contains("algeria")) r.country = "Algeria";
		else r.country = null;

		List<String> topics = new ArrayList<>();
		if (lower.contains("unemployment")) topics.add("unemployment");
		if (lower.contains("agriculture")) topics.add("agriculture");
		if (lower.contains("climate")) topics.add("climate change");
		if (lower.contains("energy")) topics.add("energy");
		r.topics = topics;
		return r;
	}

	public static class Result {
		public String documentType;
		public String country;
		public List<String> topics;
		public String summary;
		public String entitiesJson;
	}
}


