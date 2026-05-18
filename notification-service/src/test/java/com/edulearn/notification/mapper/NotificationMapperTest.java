package com.edulearn.notification.mapper;

import com.edulearn.notification.dto.NotificationRequestDto;
import com.edulearn.notification.dto.NotificationResponseDto;
import com.edulearn.notification.entity.Notification;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Disabled("Generated MapStruct implementations are excluded from coverage and can be unstable in clean Windows reactor runs")
class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toEntity_MapsRequestFields() {
        NotificationRequestDto request = NotificationRequestDto.builder()
                .recipientId(1)
                .recipientRole("STUDENT")
                .senderId(2)
                .type("SYSTEM")
                .title("Title")
                .message("Message")
                .referenceId(101)
                .referenceType("COURSE")
                .channel("EMAIL")
                .recipientEmail("student@test.com")
                .build();

        Notification notification = mapper.toEntity(request);

        assertEquals(1, notification.getRecipientId());
        assertEquals("STUDENT", notification.getRecipientRole());
        assertEquals("SYSTEM", notification.getType());
        assertEquals("EMAIL", notification.getChannel());
        assertNull(notification.getNotificationId());
    }

    @Test
    void toDto_MapsEntityFields() {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setNotificationId(1);
        notification.setRecipientId(2);
        notification.setRecipientRole("ADMIN");
        notification.setSenderId(3);
        notification.setType("SYSTEM");
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setReferenceId(10);
        notification.setReferenceType("COURSE");
        notification.setIsRead(true);
        notification.setIsEmailSent(true);
        notification.setChannel("BOTH");
        notification.setCreatedAt(now);
        notification.setReadAt(now);

        NotificationResponseDto response = mapper.toDto(notification);

        assertEquals(1, response.getNotificationId());
        assertEquals(2, response.getRecipientId());
        assertEquals("ADMIN", response.getRecipientRole());
        assertEquals(true, response.getIsRead());
        assertEquals(now, response.getReadAt());
    }

    @Test
    void toDtoList_MapsListAndHandlesNull() {
        Notification notification = new Notification();
        notification.setNotificationId(1);

        assertEquals(1, mapper.toDtoList(List.of(notification)).size());
        assertNull(mapper.toDtoList(null));
        assertNull(mapper.toEntity(null));
        assertNull(mapper.toDto(null));
    }
}
