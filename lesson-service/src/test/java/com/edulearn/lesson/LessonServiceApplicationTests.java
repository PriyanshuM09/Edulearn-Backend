package com.edulearn.lesson;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Covered by LessonCoverageTest.main; full context startup is too memory-heavy during full reactor coverage runs")
class LessonServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
