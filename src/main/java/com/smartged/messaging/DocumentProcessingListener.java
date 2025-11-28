package com.smartged.messaging;

import com.smartged.document.DocumentEntity;
import com.smartged.document.DocumentRepository;
import com.smartged.document.DocumentStatus;
import com.smartged.classification.ClassificationService;
import com.smartged.classification.MistralClassificationService;
import com.smartged.search.SearchIndexService;
import com.smartged.processing.TextExtractionService;
import com.smartged.processing.ChunkingService;
import com.smartged.vector.EmbeddingService;
import com.smartged.vector.QdrantVectorStore;
import com.smartged.notify.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class DocumentProcessingListener {

	private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingListener.class);
	private final DocumentRepository documentRepository;
	private final TextExtractionService textExtractionService;
	private final ChunkingService chunkingService;
	private final EmbeddingService embeddingService;
	private final QdrantVectorStore vectorStore;
	private final ClassificationService classificationService;
	private final MistralClassificationService mistralClassificationService;
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
			Optional<MistralClassificationService> mistralClassificationService,
			NotificationService notificationService
	) {
		this.documentRepository = documentRepository;
		this.textExtractionService = textExtractionService;
		this.chunkingService = chunkingService;
		this.embeddingService = embeddingService;
		this.vectorStore = vectorStore;
		this.classificationService = classificationService;
		this.searchIndexService = searchIndexService;
		this.mistralClassificationService = mistralClassificationService.orElse(null);
		this.notificationService = notificationService;
	}

	@RabbitListener(queues = "#{documentUploadedQueue.name}")
	@Transactional
	public void onUploaded(DocumentUploadedEvent event) {
		logger.info("Received document for processing: {}", event.documentId());
		DocumentEntity doc = documentRepository.findById(event.documentId()).orElse(null);
		if (doc == null) {
			logger.warn("Document with id {} not found in database, skipping.", event.documentId());
			return;
		}
		Instant start = Instant.now();
		try {
			// Service 1: Text Extraction
			logger.info("Document {} - Starting text extraction service", doc.getId());
			String text = textExtractionService.extractFromPath(event.storagePath());
			long length = text != null ? text.length() : 0L;
			doc.setExtractedChars(length);
			logger.info("Document {} - Text extraction completed ({} chars)", doc.getId(), length);

			// Service 2: Classification
			logger.info("Document {} - Starting classification service", doc.getId());
			// Classification: prefer Mistral if enabled, fallback to heuristics
			var llm = mistralClassificationService != null ? mistralClassificationService.classify(text) : null;
			var cls = llm != null ? llm : classificationService.classify(text);
			doc.setDocumentType(cls.documentType);
			doc.setCountry(cls.country);
			doc.setAutoTags(cls.topics);
			if (llm != null) {
				doc.setSummary(llm.summary);
				doc.setEntitiesJson(llm.entitiesJson);
			}
			logger.info("Document {} - Classification completed (Type: {}, Topics: {})", doc.getId(), cls.documentType, cls.topics.size());

			// Service 3: Chunking
			logger.info("Document {} - Starting chunking service", doc.getId());
			var chunks = chunkingService.splitIntoChunks(text, 800);
			logger.info("Document {} - Chunking completed ({} chunks)", doc.getId(), chunks.size());

			// Service 4: Embedding & Vector Storage (if enabled)
			logger.info("Document {} - Starting embedding service", doc.getId());
			// Note: We'll call embeddingService.embed() and vectorStore.upsertChunk() which internally check if enabled
			logger.info("Document {} - Starting vector storage service (Qdrant)", doc.getId());
			for (int i = 0; i < chunks.size(); i++) {
				var chunk = chunks.get(i);
				var vector = embeddingService.embed(chunk);
				vectorStore.upsertChunk(doc.getId(), i, chunk, vector);
			}
			logger.info("Document {} - Embedding and vector storage completed", doc.getId());

			// Service 5: Search Indexing
			logger.info("Document {} - Starting search indexing service (Elasticsearch)", doc.getId());
			searchIndexService.index(doc, text);
			logger.info("Document {} - Search indexing completed", doc.getId());

			// Finalize document
			doc.setStatus(DocumentStatus.ready);
			doc.setProcessedAt(Instant.now());
			doc.setProcessingDurationSeconds(Duration.between(start, doc.getProcessedAt()).toSeconds());
			documentRepository.save(doc);
			logger.info("Document {} - Processing completed successfully in {} seconds", doc.getId(), doc.getProcessingDurationSeconds());
			notificationService.notifyStatus(doc);
		} catch (Exception e) {
			logger.error("Document {} - Processing failed at step: {}", doc.getId(), getCurrentProcessingStep(e), e);
			doc.setStatus(DocumentStatus.failed);
			doc.setErrorMessage(e.getMessage());
			doc.setProcessedAt(Instant.now());
			doc.setProcessingDurationSeconds(Duration.between(start, doc.getProcessedAt()).toSeconds());
			documentRepository.save(doc);
			notificationService.notifyStatus(doc);
		}
	}

	private String getCurrentProcessingStep(Exception e) {
		// This is a simple way to track where the error occurred based on the stack trace
		StackTraceElement[] stack = e.getStackTrace();
		for (StackTraceElement element : stack) {
			String className = element.getClassName();
			if (className.contains("TextExtractionService")) {
				return "Text Extraction";
			} else if (className.contains("MistralClassificationService") || className.contains("ClassificationService")) {
				return "Classification";
			} else if (className.contains("ChunkingService")) {
				return "Chunking";
			} else if (className.contains("EmbeddingService")) {
				return "Embedding";
			} else if (className.contains("QdrantVectorStore")) {
				return "Vector Storage";
			} else if (className.contains("SearchIndexService")) {
				return "Search Indexing";
			}
		}
		return "Unknown";
	}
}


