package com.overmild.mugs.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "AddMugHistoryRequest", description = "Request payload to add a mug history entry")
public record AddMugHistoryRequest(
        @Schema(example = "Replaced lid and cleaned seal")
        @NotBlank(message = "entry is required")
        String entry
) {
}
