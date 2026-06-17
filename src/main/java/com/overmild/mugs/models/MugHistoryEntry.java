package com.overmild.mugs.models;

import java.time.Instant;

public record MugHistoryEntry(
        String id,
        String mugId,
        String addedByUserId,
        String entry,
        Instant createdAt
) {
}
