package com.overmild.mugs.model;

import java.util.List;

public record MugWithHistory(
        Mug mug,
        List<MugHistoryEntry> history
) {
}
