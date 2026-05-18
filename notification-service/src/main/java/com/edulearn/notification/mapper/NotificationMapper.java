package com.edulearn.notification.mapper;

import com.edulearn.notification.dto.NotificationRequestDto;
import com.edulearn.notification.dto.NotificationResponseDto;
import com.edulearn.notification.entity.Notification;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "isRead", ignore = true)
    @Mapping(target = "isEmailSent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    Notification toEntity(NotificationRequestDto dto);

    NotificationResponseDto toDto(Notification notificationEntity);

    List<NotificationResponseDto> toDtoList(
            List<Notification> notifications);
}