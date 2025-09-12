package com.project.korex.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record RemittanceResponseDto(
        Long id,
        String customerName,
        BigDecimal amount,
        String currency,
        List<FileDto> documents,
        String status
) {}
