package com.smartged.classification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.llm.mistral.enabled", havingValue = "true", matchIfMissing = false)
public class MistralClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(MistralClassificationService.class);
    
    private final ChatClient chatClient;

    public MistralClassificationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ClassificationService.Result classify(String text) {
        if (text == null || text.trim().isEmpty()) {
            logger.warn("Empty text provided for classification");
            return null;
        }

        try {
            String truncated = text;
            if (truncated.length() > 30000) { // Mistral has context limits
                truncated = truncated.substring(0, 30000);
            }

            String systemPrompt = """
                You are an information extraction assistant. Analyze the document content and return a JSON object that conforms to the structure of the 'Result' class.
                Your response must be only the raw JSON object with these fields: documentType, country, topics, summary, and entitiesJson.
                Do not include any other text or formatting.
                
                The Result class structure is:
                - documentType: string - the document type
                - country: string - the country identified in the document (or null if not found)
                - topics: array of strings - list of topics
                - summary: string - summary of the document (or null if not provided)
                - entitiesJson: string - JSON string of identified entities (or null if not provided)
                """;

            String userMessage = "Document content:\n" + truncated;

            return chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .entity(ClassificationService.Result.class);
        } catch (Exception e) {
            logger.error("Error calling Mistral AI API", e);
            return null;
        }
    }
}