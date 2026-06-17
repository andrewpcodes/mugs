package com.overmild.mugs.services;

import com.overmild.mugs.models.Mug;
import com.overmild.mugs.models.MugHistoryEntry;
import com.overmild.mugs.repositories.MugRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MugService {
    private final MugRepository mugRepository;

    public MugService(MugRepository mugRepository) {
        this.mugRepository = mugRepository;
    }

    @Transactional(readOnly = true)
    public Mug getMugById(String mugId) {
        return mugRepository.findById(mugId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));
    }

    @Transactional(readOnly = true)
    public List<MugHistoryEntry> getMugHistoryByMugId(String mugId) {
        if (mugRepository.findById(mugId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found");
        }
        return mugRepository.findHistoryByMugId(mugId);
    }

    @Transactional(readOnly = true)
    public List<Mug> getMugsByOwnerId(String ownerId, String requesterUserId) {
        if (!ownerId.equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Can only read mugs for your own ownerId");
        }
        return mugRepository.findByOwnerUserId(ownerId);
    }

    @Transactional
    public Mug createMug(String creatorUserId, LocalDate purchaseDate, LocalDate renewalDate, BigDecimal cost, BigDecimal sizeOunces) {
        Instant now = Instant.now();
        Mug mug = new Mug(
                UUID.randomUUID().toString(),
                creatorUserId,
                purchaseDate,
                renewalDate,
                cost,
                sizeOunces,
                now,
                now
        );
        mugRepository.createMug(mug);
        return mug;
    }

    @Transactional
    public void updateMug(
            String mugId,
            String requesterUserId,
            LocalDate purchaseDate,
            LocalDate renewalDate,
            BigDecimal cost,
            BigDecimal sizeOunces
    ) {
        boolean updated = mugRepository.updateMugByOwner(
                mugId,
                requesterUserId,
                purchaseDate,
                renewalDate,
                cost,
                sizeOunces
        );
        if (!updated) {
            throw notFoundOrForbidden(mugId, requesterUserId);
        }
    }

    @Transactional
    public void deleteMug(String mugId, String requesterUserId) {
        boolean deleted = mugRepository.deleteMugByOwner(mugId, requesterUserId);
        if (!deleted) {
            throw notFoundOrForbidden(mugId, requesterUserId);
        }
    }

    @Transactional
    public MugHistoryEntry addHistoryEntry(String mugId, String requesterUserId, String entry) {
        Mug mug = mugRepository.findById(mugId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mug not found"));
        if (!mug.ownerUserId().equals(requesterUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the mug creator can add mug history");
        }

        MugHistoryEntry historyEntry = new MugHistoryEntry(
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
