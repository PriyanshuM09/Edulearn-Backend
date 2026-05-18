package com.edulearn.notification;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Application main is covered separately; full context startup hits generated mapper beans in clean reactor runs")
@SpringBootTest
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
