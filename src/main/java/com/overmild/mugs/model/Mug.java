package com.overmild.mugs.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record Mug(
        String id,
        String ownerUserId,
        LocalDate purchaseDate,
        LocalDate renewalDate,
        BigDecimal cost,
        BigDecimal sizeOunces,
        Instant createdAt,
        Instant updatedAt
) {
}
