package com.overmild.mugs.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MugHistoryResponse", description = "Response payload representing a mug history entry")
public record MugHistoryResponse(
        @Schema(example = "4b551322-7834-4218-bf7f-aa7d27c0f6b1")
        String id,
        @Schema(example = "f95af6c7-c9c5-44c9-95a4-8520d9f77d84")
        String mugId,
        @Schema(example = "1d20f53f-2c6d-49d1-8f36-8eaf2d9f9939")
        String addedByUserId,
        @Schema(example = "Replaced lid and cleaned seal")
        String entry,
        @Schema(example = "2026-06-16T20:03:11.843827Z")
        String createdAt
) {
    public static MugHistoryResponse from(MugHistoryEntry historyEntry) {
        return new MugHistoryResponse(
                historyEntry.id(),
                historyEntry.mugId(),
                historyEntry.addedByUserId(),
                historyEntry.entry(),
                historyEntry.createdAt().toString()
        );
    }
}
