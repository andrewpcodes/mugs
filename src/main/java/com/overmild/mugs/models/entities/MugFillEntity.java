package com.overmild.mugs.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MugFillEntity(
        String id,
        String mugId,
        String filledByUserId,
        LocalDate fillDate,
        BigDecimal amountOunces,
        Instant createdAt
) {
}
