package com.overmild.mugs.services;

import com.overmild.mugs.models.entities.MugEntity;
import com.overmild.mugs.models.entities.MugHistoryEntity;
import com.overmild.mugs.repositories.MugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MugService {

    private final MugRepository mugRepository;

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
        Instant now = Instant.now();
        MugEntity mugEntity = new MugEntity(
                UUID.randomUUID().toString(),
                creatorUserId,
                purchaseDate,
                renewalDate,
                cost,
                sizeOunces,
                now,
                now
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
            BigDecimal sizeOunces
    ) {
        Optional<MugEntity> updated = mugRepository.updateMugByOwner(
                mugId,
                requesterUserId,
                purchaseDate,
                renewalDate,
                cost,
                sizeOunces
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
    public MugHistoryEntity addHistoryEntry(String mugId, String requesterUserId, String entry) {

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
                Instant.now()
        );

        mugRepository.addHistoryEntry(historyEntry);
        return historyEntry;
    }

    private ResponseStatusException notFoundOrForbidden(String mugId, String requesterUserId) {
        return mugRepository.findById(mugId)
                .map(mug -> mug.ownerUserId().equals(requesterUserId)
                        ? new ResponseStatusException(HttpStatus.CONFLICT, "Mug state changed, retry request")
                        : new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the mug creator can modify the mug"))
                .orElseGet(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));
    }
}
