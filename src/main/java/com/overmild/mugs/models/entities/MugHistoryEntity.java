package com.overmild.mugs.models.entities;

import java.time.Instant;

public record MugHistoryEntity(
        String id,
        String mugId,
        String addedByUserId,
        String entry,
        Instant createdAt
) {
}
