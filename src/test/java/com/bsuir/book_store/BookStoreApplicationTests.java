package com.bsuir.book_store;

import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import com.bsuir.book_store.recommendations.infrastructure.BookCoOccurrenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BookStoreApplicationTests {

	@MockitoBean
	private BookElasticRepository bookElasticRepository;

	@MockitoBean
	private BookCoOccurrenceRepository bookCoOccurrenceRepository;

	@MockitoBean
	private JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}

}