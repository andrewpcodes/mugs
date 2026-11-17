package com.overmild.mugs.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MugEntity(
        String id,
        String ownerUserId,
        LocalDate purchaseDate,
        LocalDate renewalDate,
        BigDecimal cost,
        BigDecimal sizeOunces,
        Instant createdAt,
        Instant updatedAt,
        String nickname,
        int fillCount
) {
}
