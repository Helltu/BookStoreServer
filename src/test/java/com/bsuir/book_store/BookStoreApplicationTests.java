package com.bsuir.book_store;

import com.bsuir.book_store.catalog.infrastructure.elastic.BookElasticRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class BookStoreApplicationTests {

	@MockitoBean
	private BookElasticRepository bookElasticRepository;

	@Test
	void contextLoads() {
	}

}