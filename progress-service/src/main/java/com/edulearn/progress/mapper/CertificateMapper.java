package com.edulearn.progress.mapper;

import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.entity.Certificate;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE)
public interface CertificateMapper {

    CertificateResponseDto toDto(Certificate certificate);

    List<CertificateResponseDto> toDtoList(List<Certificate> certificates);
}