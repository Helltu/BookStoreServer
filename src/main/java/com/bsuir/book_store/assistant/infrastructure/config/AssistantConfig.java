package com.bsuir.book_store.assistant.infrastructure.config;

import com.bsuir.book_store.assistant.application.BookStoreAssistant;
import com.bsuir.book_store.assistant.infrastructure.tools.CatalogAiTools;
import com.bsuir.book_store.assistant.infrastructure.tools.OrderAiTools;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AssistantConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return chatId -> MessageWindowChatMemory.withMaxMessages(10);
    }

    @Bean
    public BookStoreAssistant bookStoreAssistant(
            ChatLanguageModel chatLanguageModel,
            CatalogAiTools catalogAiTools,
            OrderAiTools orderAiTools,
            ChatMemoryProvider chatMemoryProvider
    ) {
        return AiServices.builder(BookStoreAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(catalogAiTools, orderAiTools)
                .build();
    }
}