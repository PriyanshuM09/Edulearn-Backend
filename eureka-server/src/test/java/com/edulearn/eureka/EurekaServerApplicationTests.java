package com.edulearn.eureka;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class EurekaServerApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
			String[] args = {"--test"};
			EurekaServerApplication.main(args);
			spring.verify(() -> SpringApplication.run(EurekaServerApplication.class, args));
		}
	}
}
