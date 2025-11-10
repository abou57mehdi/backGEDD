package com.smartged.search;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

	private final SearchService searchService;

	public SearchController(SearchService searchService) {
		this.searchService = searchService;
	}

	@PostMapping("/keyword")
	public ResponseEntity<Map<String, Object>> keyword(@RequestBody Map<String, String> body) {
		String query = body.getOrDefault("query", "");
		return ResponseEntity.ok(searchService.keywordSearch(query));
	}

	@PostMapping("/smart")
	public ResponseEntity<Map<String, Object>> smart(@RequestBody Map<String, String> body) {
		String query = body.getOrDefault("query", "");
		return ResponseEntity.ok(searchService.smartSearch(query));
	}
}


