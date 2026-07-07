package com.overmild.mugs.controllers;

import com.overmild.mugs.mappers.MugMapper;
import com.overmild.mugs.models.*;
import com.overmild.mugs.models.entities.MugEntity;
import com.overmild.mugs.models.entities.MugHistoryEntity;
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
    private final MugMapper mugMapper;

    @GetMapping("/api/mugs/{mugId}")
    public MugResponse getMugById(@PathVariable @NotBlank String mugId) {
        log.info("Received request for mug id={}", mugId);
        return mugMapper.toMugResponse(mugService.getMugById(mugId));
    }

    @GetMapping("/api/mugs/{mugId}/history")
    public List<MugHistoryResponse> getMugHistoryByMugId(@PathVariable @NotBlank String mugId) {
        log.info("Received request for mug history id={}", mugId);
        return mugService.getMugHistoryByMugId(mugId).stream()
                .map(mugMapper::toMugHistoryResponse)
                .toList();
    }

    @GetMapping("/api/mugs/owners/{ownerId}")
    public List<MugResponse> getMugsByOwnerId(@AuthenticationPrincipal Jwt jwt, @PathVariable @NotBlank String ownerId) {
        log.info("Received request for owner mugs ownerId={} requester={}", ownerId, jwt.getSubject());
        return mugService.getMugsByOwnerId(ownerId, jwt.getSubject()).stream()
                .map(mugMapper::toMugResponse)
                .toList();
    }

    @PostMapping("/api/mugs")
    public MugResponse createMug(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateMugRequest request) {
        MugEntity mugEntity = mugService.createMug(
                jwt.getSubject(),
                request.getPurchaseDate(),
                request.getRenewalDate(),
                request.getCost(),
                request.getSizeOunces()
        );
        return mugMapper.toMugResponse(mugEntity);
    }

    @PutMapping("/api/mugs/{mugId}")
    public MugResponse updateMug(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @NotBlank String mugId,
            @Valid @RequestBody UpdateMugRequest request
    ) {
        return mugMapper.toMugResponse(mugService.updateMug(
                mugId,
                jwt.getSubject(),
                request.getPurchaseDate(),
                request.getRenewalDate(),
                request.getCost(),
                request.getSizeOunces()
        ));
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
        MugHistoryEntity historyEntry = mugService.addHistoryEntry(mugId, jwt.getSubject(), request.getEntry());
        return mugMapper.toMugHistoryResponse(historyEntry);
    }
}
