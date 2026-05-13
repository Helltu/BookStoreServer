package com.bsuir.book_store.shared.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

public interface StorageService {
    String store(MultipartFile file);
    String store(InputStream inputStream, String filename, String contentType, long size);
    void delete(String url);
}