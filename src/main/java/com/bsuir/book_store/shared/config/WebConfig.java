package com.bsuir.book_store.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, MultipartFile>() {
            @Override
            public MultipartFile convert(String source) {
                return null; // Тихо игнорируем пустые строки от Swagger UI
            }
        });
    }
}