package com.bsuir.book_store.assistant.application;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

public interface BookStoreAssistant {

    String chat(@MemoryId String username, @UserMessage String userMessage);
}