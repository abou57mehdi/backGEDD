package com.smartged.document;

import com.smartged.document.dto.DocumentResponse;
import com.smartged.document.dto.StatusResponse;
import com.smartged.notify.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {

	private final DocumentService documentService;
	private final NotificationService notificationService;

	public DocumentController(DocumentService documentService, NotificationService notificationService) {
		this.documentService = documentService;
		this.notificationService = notificationService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DocumentResponse> upload(
			@AuthenticationPrincipal UserDetails principal,
			@RequestPart("file") MultipartFile file
	) throws IOException {
		DocumentEntity saved = documentService.createOnUpload(principal, file);
		return ResponseEntity.ok(DocumentResponse.from(saved));
	}

	@GetMapping("/{id}")
	public ResponseEntity<DocumentResponse> getById(@PathVariable("id") UUID id) {
		DocumentEntity e = documentService.getById(id);
		return ResponseEntity.ok(DocumentResponse.from(e));
	}

	@GetMapping("/{id}/status")
	public ResponseEntity<StatusResponse> getStatus(@PathVariable("id") UUID id) {
		DocumentEntity e = documentService.getById(id);
		return ResponseEntity.ok(StatusResponse.from(e));
	}

	@GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamStatus(@PathVariable("id") UUID id) {
		return notificationService.register(id);
	}
}


