package com.bsuir.book_store.shared.storage;

import com.bsuir.book_store.shared.exception.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation = Paths.get("uploads");

    public LocalStorageService() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        try {
            if (file.isEmpty()) throw new DomainException("Файл пуст");
            
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path destinationFile = this.rootLocation.resolve(Paths.get(filename)).normalize().toAbsolutePath();
            
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new DomainException("Попытка сохранить файл вне директории uploads");
            }
            
            file.transferTo(destinationFile);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new DomainException("Ошибка при сохранении файла", e);
        }
    }
}