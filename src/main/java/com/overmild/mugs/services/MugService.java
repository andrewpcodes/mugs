package com.overmild.mugs.services;

import com.overmild.mugs.models.entities.MugEntity;
import com.overmild.mugs.models.entities.MugFillEntity;
import com.overmild.mugs.models.entities.MugHistoryEntity;
import com.overmild.mugs.repositories.MugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MugService {

    private static final int NICKNAME_MAX_LENGTH = 50;
    /** No place on Earth is further ahead of UTC, so no user's local date is later than this zone's. */
    private static final ZoneOffset LATEST_ZONE = ZoneOffset.ofHours(14);

    private final MugRepository mugRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<MugEntity> getAllMugs() {
        return mugRepository.getAllMugs()
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public MugEntity getMugById(String mugId) {
        return mugRepository.findById(mugId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));
    }
    @Transactional(readOnly = true)
    public List<MugHistoryEntity> getMugHistoryByMugId(String mugId) {
        if (mugRepository.findById(mugId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found");
        }
        return mugRepository.findHistoryByMugId(mugId);
    }

    @Transactional(readOnly = true)
    public List<MugEntity> getMugsByOwnerId(String ownerId, String requesterUserId) {
        if (!ownerId.equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only read mugs for your own ownerId");
        }
        return mugRepository.findByOwnerUserId(ownerId);
    }

    @Transactional
    public MugEntity createMug(String creatorUserId, LocalDate purchaseDate, LocalDate renewalDate, BigDecimal cost, BigDecimal sizeOunces) {
        Instant now = Instant.now(clock);
        MugEntity mugEntity = new MugEntity(
                UUID.randomUUID().toString(),
                creatorUserId,
                purchaseDate,
                renewalDate,
                cost,
                sizeOunces,
                now,
                now,
                null,
                0
        );
        mugRepository.createMug(mugEntity);
        return mugEntity;
    }

    @Transactional
    public MugEntity updateMug(
            String mugId,
            String requesterUserId,
            LocalDate purchaseDate,
            LocalDate renewalDate,
            BigDecimal cost,
            BigDecimal sizeOunces,
            String nickname
    ) {
        // Absent (null) keeps the stored nickname; empty or blank clears it.
        boolean keepNickname = nickname == null;
        String normalizedNickname = keepNickname || nickname.isBlank() ? null : nickname.strip();
        if (normalizedNickname != null && normalizedNickname.length() > NICKNAME_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nickname must be at most 50 characters after trimming");
        }

        Optional<MugEntity> updated = mugRepository.updateMugByOwner(
                mugId,
                requesterUserId,
                purchaseDate,
                renewalDate,
                cost,
                sizeOunces,
                normalizedNickname,
                keepNickname,
                Instant.now(clock)
        );

        if (updated.isEmpty()) {
            throw notFoundOrForbidden(mugId, requesterUserId);
        }

        return updated.get();
    }

    @Transactional
    public void deleteMug(String mugId, String requesterUserId) {
        boolean deleted = mugRepository.deleteMugByOwner(mugId, requesterUserId);
        if (!deleted) {
            throw notFoundOrForbidden(mugId, requesterUserId);
        }
    }

    @Transactional
    public MugHistoryEntity addHistoryEntry(String mugId, String requesterUserId, String entry, BigDecimal price) {

        MugEntity mugEntity = mugRepository.findById(mugId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));

        if (!mugEntity.ownerUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the mug creator can add mug history");
        }

        MugHistoryEntity historyEntry = new MugHistoryEntity(
                UUID.randomUUID().toString(),
                mugId,
                requesterUserId,
                entry,
                Instant.now(clock),
                price
        );

        mugRepository.addHistoryEntry(historyEntry);
        return historyEntry;
    }

    @Transactional
    public MugFillEntity addFill(String mugId, String requesterUserId, BigDecimal amountOunces, LocalDate fillDate) {
        if (amountOunces == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amountOunces is required");
        }
        if (fillDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fillDate is required");
        }
        MugEntity mugEntity = mugRepository.findById(mugId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));

        if (!mugEntity.ownerUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the mug creator can add fills");
        }
        if (amountOunces.compareTo(mugEntity.sizeOunces()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amountOunces must not exceed the mug's sizeOunces");
        }

        if (fillDate.isAfter(LocalDate.now(clock.withZone(LATEST_ZONE)))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fillDate must not be in the future");
        }

        MugFillEntity fill = new MugFillEntity(
                UUID.randomUUID().toString(),
                mugId,
                requesterUserId,
                fillDate,
                amountOunces,
                Instant.now(clock)
        );
        mugRepository.addFill(fill);
        return fill;
    }

    private ResponseStatusException notFoundOrForbidden(String mugId, String requesterUserId) {
        return mugRepository.findById(mugId)
                .map(mug -> mug.ownerUserId().equals(requesterUserId)
                        ? new ResponseStatusException(HttpStatus.CONFLICT, "Mug state changed, retry request")
                        : new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the mug creator can modify the mug"))
                .orElseGet(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));
    }
}
