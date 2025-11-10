package com.smartged.messaging;

import com.smartged.document.DocumentEntity;
import com.smartged.document.DocumentRepository;
import com.smartged.document.DocumentStatus;
import com.smartged.classification.ClassificationService;
import com.smartged.classification.GeminiClassificationService;
import com.smartged.search.SearchIndexService;
import com.smartged.processing.TextExtractionService;
import com.smartged.processing.ChunkingService;
import com.smartged.vector.EmbeddingService;
import com.smartged.vector.QdrantVectorStore;
import com.smartged.notify.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
public class DocumentProcessingListener {

	private final DocumentRepository documentRepository;
	private final TextExtractionService textExtractionService;
	private final ChunkingService chunkingService;
	private final EmbeddingService embeddingService;
	private final QdrantVectorStore vectorStore;
	private final ClassificationService classificationService;
	private final GeminiClassificationService geminiClassificationService;
	private final SearchIndexService searchIndexService;
	private final NotificationService notificationService;

	public DocumentProcessingListener(
			DocumentRepository documentRepository,
			TextExtractionService textExtractionService,
			ChunkingService chunkingService,
			EmbeddingService embeddingService,
			QdrantVectorStore vectorStore,
			ClassificationService classificationService,
			SearchIndexService searchIndexService,
			GeminiClassificationService geminiClassificationService,
			NotificationService notificationService
	) {
		this.documentRepository = documentRepository;
		this.textExtractionService = textExtractionService;
		this.chunkingService = chunkingService;
		this.embeddingService = embeddingService;
		this.vectorStore = vectorStore;
		this.classificationService = classificationService;
		this.searchIndexService = searchIndexService;
		this.geminiClassificationService = geminiClassificationService;
		this.notificationService = notificationService;
	}

	@RabbitListener(queues = "#{documentUploadedQueue.name}")
	@Transactional
	public void onUploaded(DocumentUploadedEvent event) {
		DocumentEntity doc = documentRepository.findById(event.documentId).orElse(null);
		if (doc == null) {
			return;
		}
		Instant start = Instant.now();
		try {
			String text = textExtractionService.extractFromPath(event.storagePath);
			long length = text != null ? text.length() : 0L;
			doc.setExtractedChars(length);
			// Classification: prefer Gemini if enabled, fallback to heuristics
			var llm = geminiClassificationService.classify(text);
			var cls = llm != null ? llm : classificationService.classify(text);
			doc.setDocumentType(cls.documentType);
			doc.setCountry(cls.country);
			doc.setAutoTags(cls.topics);
			if (llm != null) {
				doc.setSummary(llm.summary);
				doc.setEntitiesJson(llm.entitiesJson);
			}
			// Chunk and embed (store to vector DB if enabled)
			var chunks = chunkingService.splitIntoChunks(text, 800);
			for (int i = 0; i < chunks.size(); i++) {
				var chunk = chunks.get(i);
				var vector = embeddingService.embed(chunk);
				vectorStore.upsertChunk(doc.getId(), i, chunk, vector);
			}
			// Keyword indexing (if enabled)
			searchIndexService.index(doc, text);
			doc.setStatus(DocumentStatus.ready);
			doc.setProcessedAt(Instant.now());
			doc.setProcessingDurationSeconds(Duration.between(start, doc.getProcessedAt()).toSeconds());
			documentRepository.save(doc);
			notificationService.notifyStatus(doc);
		} catch (Exception e) {
			doc.setStatus(DocumentStatus.failed);
			doc.setErrorMessage(e.getMessage());
			doc.setProcessedAt(Instant.now());
			doc.setProcessingDurationSeconds(Duration.between(start, doc.getProcessedAt()).toSeconds());
			documentRepository.save(doc);
			notificationService.notifyStatus(doc);
		}
	}
}


