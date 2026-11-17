package com.overmild.mugs.models.entities;

import java.math.BigDecimal;
import java.time.Instant;

public record MugHistoryEntity(
        String id,
        String mugId,
        String addedByUserId,
        String entry,
        Instant createdAt,
        BigDecimal price
) {
}
