package com.smartged.notify;

import com.smartged.document.DocumentEntity;
import com.smartged.document.dto.StatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

	private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter register(UUID documentId) {
		SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());
		emitters.computeIfAbsent(documentId, k -> new ArrayList<>()).add(emitter);
		emitter.onCompletion(() -> remove(documentId, emitter));
		emitter.onTimeout(() -> remove(documentId, emitter));
		return emitter;
	}

	public void notifyStatus(DocumentEntity doc) {
		var list = emitters.get(doc.getId());
		if (list == null || list.isEmpty()) return;
		var payload = StatusResponse.from(doc);
		List<SseEmitter> toRemove = new ArrayList<>();
		for (SseEmitter emitter : list) {
			try {
				emitter.send(SseEmitter.event().name("document.processed").data(payload));
				// Optionally complete on terminal states
				if (payload.status != null && (payload.status.name().equals("ready") || payload.status.name().equals("failed"))) {
					emitter.complete();
					toRemove.add(emitter);
				}
			} catch (IOException e) {
				emitter.completeWithError(e);
				toRemove.add(emitter);
			}
		}
		list.removeAll(toRemove);
		if (list.isEmpty()) {
			emitters.remove(doc.getId());
		}
	}

	private void remove(UUID documentId, SseEmitter emitter) {
		var list = emitters.get(documentId);
		if (list == null) return;
		list.remove(emitter);
		if (list.isEmpty()) emitters.remove(documentId);
	}
}


