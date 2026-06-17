package com.overmild.mugs.repositories;

import com.overmild.mugs.models.Mug;
import com.overmild.mugs.models.MugHistoryEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MugRepository {
    Optional<Mug> findById(String mugId);

    List<MugHistoryEntry> findHistoryByMugId(String mugId);

    List<Mug> findByOwnerUserId(String ownerUserId);

    void createMug(Mug mug);

    boolean updateMugByOwner(
            String mugId,
            String ownerUserId,
            LocalDate purchaseDate,
            LocalDate renewalDate,
            BigDecimal cost,
            BigDecimal sizeOunces
    );

    boolean deleteMugByOwner(String mugId, String ownerUserId);

    void addHistoryEntry(MugHistoryEntry historyEntry);
}
