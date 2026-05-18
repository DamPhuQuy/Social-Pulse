package com.socialpulse.app;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires full infrastructure (Redis, PostgreSQL) - run as integration test only")
class AppApplicationTests {

	@Test
	void contextLoads() {
	}
}
