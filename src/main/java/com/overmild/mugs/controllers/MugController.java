package com.overmild.mugs.controllers;

import com.overmild.mugs.model.*;
import com.overmild.mugs.services.MugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@Tag(name = "Mugs", description = "Operations for managing mugs and mug history")
public class MugController {

    private final MugService mugService;

    @GetMapping("/api/mugs/{mugId}")
    @Operation(summary = "Get mug by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mug returned",
                    content = @Content(schema = @Schema(implementation = MugResponse.class))),
            @ApiResponse(responseCode = "404", description = "Mug not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public MugResponse getMugById(@PathVariable @NotBlank String mugId) {
        log.info("Received request for mug id={}", mugId);
        return MugResponse.from(mugService.getMugById(mugId));
    }

    @GetMapping("/api/mugs/{mugId}/history")
    @Operation(summary = "Get mug history by mug ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mug history returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MugHistoryResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Mug not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<MugHistoryResponse> getMugHistoryByMugId(@PathVariable @NotBlank String mugId) {
        log.info("Received request for mug history id={}", mugId);
        return mugService.getMugHistoryByMugId(mugId).stream()
                .map(MugHistoryResponse::from)
                .toList();
    }

    @GetMapping("/api/mugs/owners/{ownerId}")
    @Operation(summary = "Get mugs by owner")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owner mugs returned",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MugResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<MugResponse> getMugsByOwnerId(@AuthenticationPrincipal Jwt jwt, @PathVariable @NotBlank String ownerId) {
        log.info("Received request for owner mugs ownerId={} requester={}", ownerId, jwt.getSubject());
        return mugService.getMugsByOwnerId(ownerId, jwt.getSubject()).stream()
                .map(MugResponse::from)
                .toList();
    }

    @PostMapping("/api/mugs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create mug")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mug created",
                    content = @Content(schema = @Schema(implementation = MugResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public MugResponse createMug(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateMugRequest request) {
        Mug mug = mugService.createMug(
                jwt.getSubject(),
                request.purchaseDate(),
                request.renewalDate(),
                request.cost(),
                request.sizeOunces()
        );
        return MugResponse.from(mug);
    }

    @PutMapping("/api/mugs/{mugId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Update mug")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mug updated", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Mug not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void updateMug(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotBlank String mugId,
            @Valid @RequestBody UpdateMugRequest request
    ) {
        mugService.updateMug(
                mugId,
                jwt.getSubject(),
                request.purchaseDate(),
                request.renewalDate(),
                request.cost(),
                request.sizeOunces()
        );
    }

    @DeleteMapping("/api/mugs/{mugId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete mug")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mug deleted", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Mug not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void deleteMug(@AuthenticationPrincipal Jwt jwt, @PathVariable @NotBlank String mugId) {
        mugService.deleteMug(mugId, jwt.getSubject());
    }

    @PostMapping("/api/mugs/{mugId}/history")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add mug history entry")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "History entry created",
                    content = @Content(schema = @Schema(implementation = MugHistoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Mug not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public MugHistoryResponse addHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotBlank String mugId,
            @Valid @RequestBody AddMugHistoryRequest request
    ) {
        MugHistoryEntry historyEntry = mugService.addHistoryEntry(mugId, jwt.getSubject(), request.entry());
        return MugHistoryResponse.from(historyEntry);
    }
}
