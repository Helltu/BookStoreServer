package com.bsuir.book_store.config;

import com.bsuir.book_store.shared.storage.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Configuration
@Profile("test")
public class TestStorageConfig {

    @Bean
    @Primary
    public StorageService storageService() {
        return new StorageService() {
            @Override
            public String store(MultipartFile file) {
                return "http://test/covers/" + file.getOriginalFilename();
            }

            @Override
            public String store(InputStream inputStream, String filename, String contentType, long size) {
                return "http://test/covers/" + filename;
            }

            @Override
            public void delete(String url) {}
        };
    }
}
