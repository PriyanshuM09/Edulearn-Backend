package com.edulearn.payment.mapper;

import com.edulearn.payment.dto.PaymentRequestDto;
import com.edulearn.payment.dto.PaymentResponseDto;
import com.edulearn.payment.entity.Payment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentMapper {

    Payment toEntity(PaymentRequestDto dto);

    PaymentResponseDto toDto(Payment payment);

    List<PaymentResponseDto> toDtoList(List<Payment> payments);
}