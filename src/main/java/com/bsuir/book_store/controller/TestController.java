package com.bsuir.book_store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import com.bsuir.book_store.security.annotations.IsManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/hello-user")
    public ResponseEntity<String> sayHelloUser() {
        return ResponseEntity.ok("Hello, Authenticated User!");
    }

    @GetMapping("/hello-manager")
    @IsManager
    public ResponseEntity<String> sayHelloManager() {
        return ResponseEntity.ok("Hello, Manager!");
    }
}