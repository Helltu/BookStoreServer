package com.bsuir.book_store.catalog.application;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookTaggingService {

    private final ChatLanguageModel chatLanguageModel;

    public List<String> generateTags(String title, String description, Set<String> existingKeywords) {
        try {
            String existing = (existingKeywords != null && !existingKeywords.isEmpty())
                    ? String.join(", ", existingKeywords)
                    : "нет";

            String prompt = String.format(
                    "Сгенерируй 5-7 неявных смысловых тегов, настроений и ассоциаций для книги '%s' с аннотацией: '%s'. " +
                            "Уже существующие теги: [%s]. Сгенерируй ТОЛЬКО НОВЫЕ теги, не дублируй существующие. " +
                            "Верни только теги через запятую, без лишнего текста, без нумерации и без точек в конце.",
                    title, description != null && !description.isBlank() ? description : "Нет описания", existing
            );

            String response = chatLanguageModel.generate(prompt);

            return Arrays.stream(response.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при генерации тегов (LLM) для книги '{}': {}", title, e.getMessage());
            return List.of();
        }
    }

    public String generateDescription(String title, String authors, String genres) {
        try {
            String prompt = String.format(
                    "Сгенерируй привлекательную, грамотную и захватывающую аннотацию (описание) для книги '%s'. " +
                            "Авторы: %s. Жанры: %s. " +
                            "Описание должно состоять из 2-3 абзацев. Не пиши никаких вводных фраз вроде 'Вот описание' или 'Конечно, я помогу', " +
                            "выведи строго только сам готовый текст аннотации.",
                    title, authors, genres
            );
            return chatLanguageModel.generate(prompt).trim();
        } catch (Exception e) {
            log.error("Ошибка при генерации описания (LLM) для книги '{}': {}", title, e.getMessage());
            return null;
        }
    }
}