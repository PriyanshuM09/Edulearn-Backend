package com.edulearn.payment.mapper;

import com.edulearn.payment.dto.SubscriptionRequestDto;
import com.edulearn.payment.dto.SubscriptionResponseDto;
import com.edulearn.payment.entity.Subscription;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubscriptionMapper {

    Subscription toEntity(SubscriptionRequestDto dto);

    SubscriptionResponseDto toDto(Subscription subscription);

    List<SubscriptionResponseDto> toDtoList(List<Subscription> subscriptions);
}