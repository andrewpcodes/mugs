package com.overmild.mugs.controllers;

import com.overmild.mugs.models.*;
import com.overmild.mugs.services.MugService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
public class MugController {

    private final MugService mugService;

    @GetMapping("/api/mugs")
    public List<MugResponse> getMugs() {
        log.info("Received request for all mugs");
        return mugService.getAllMugs();
    }

    @GetMapping("/api/mugs/{mugId}")
    public MugResponse getMugById(@PathVariable @NotBlank String mugId) {
        log.info("Received request for mug id={}", mugId);
        return mugService.getMugById(mugId);
    }

    @GetMapping("/api/mugs/{mugId}/history")
    public List<MugHistoryResponse> getMugHistoryByMugId(@PathVariable @NotBlank String mugId) {
        log.info("Received request for mug history id={}", mugId);
        return mugService.getMugHistoryByMugId(mugId);
    }

    @GetMapping("/api/mugs/owners/{ownerId}")
    public List<MugResponse> getMugsByOwnerId(@AuthenticationPrincipal Jwt jwt, @PathVariable @NotBlank String ownerId) {
        log.info("Received request for owner mugs ownerId={} requester={}", ownerId, jwt.getSubject());
        return mugService.getMugsByOwnerId(ownerId, jwt.getSubject());
    }

    @PostMapping("/api/mugs")
    public MugResponse createMug(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateMugRequest request) {
        return mugService.createMug(
                jwt.getSubject(),
                request.getPurchaseDate(),
                request.getRenewalDate(),
                request.getCost(),
                request.getSizeOunces()
        );
    }

    @PutMapping("/api/mugs/{mugId}")
    public MugResponse updateMug(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotBlank String mugId,
            @Valid @RequestBody UpdateMugRequest request
    ) {
        return mugService.updateMug(
                mugId,
                jwt.getSubject(),
                request.getPurchaseDate(),
                request.getRenewalDate(),
                request.getCost(),
                request.getSizeOunces(),
                request.getNickname()
        );
    }

    @DeleteMapping("/api/mugs/{mugId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMug(@AuthenticationPrincipal Jwt jwt, @PathVariable @NotBlank String mugId) {
        mugService.deleteMug(mugId, jwt.getSubject());
    }

    @PostMapping("/api/mugs/{mugId}/history")
    @ResponseStatus(HttpStatus.CREATED)
    public MugHistoryResponse addHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotBlank String mugId,
            @Valid @RequestBody AddMugHistoryRequest request
    ) {
        return mugService.addHistoryEntry(mugId, jwt.getSubject(), request.getEntry(), request.getPrice());
    }

    @PostMapping("/api/mugs/{mugId}/fills")
    @ResponseStatus(HttpStatus.CREATED)
    public MugFillResponse addFill(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotBlank String mugId,
            @Valid @RequestBody AddMugFillRequest request
    ) {
        log.info("Received request to add fill mugId={} requester={}", mugId, jwt.getSubject());
        return mugService.addFill(mugId, jwt.getSubject(), request.getAmountOunces(), request.getFillDate());
    }
}
