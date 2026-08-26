package com.atinroy.recallr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// spring-boot-docker-compose is runtime-scoped, so it does not supply the datasource
// during tests. Point at the same container docker-compose.yaml starts.
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:postgresql://localhost:5432/recallr",
		"spring.datasource.username=atinroy",
		"spring.datasource.password=123@tinRoy"
})
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
