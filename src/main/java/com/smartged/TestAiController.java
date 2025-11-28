package com.smartged;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.llm.mistral.enabled", havingValue = "true", matchIfMissing = false)
public class TestAiController {

    private final ChatClient chatClient;

    public TestAiController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai/generate")
    public Map<String, String> generate(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();
        return Map.of("response", response);
    }
}
