package com.overmild.mugs.repositories;

import com.overmild.mugs.models.entities.MugEntity;
import com.overmild.mugs.models.entities.MugFillEntity;
import com.overmild.mugs.models.entities.MugHistoryEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MugRepository {
    Optional<List<MugEntity>> getAllMugs();

    Optional<MugEntity> findById(String mugId);

    List<MugHistoryEntity> findHistoryByMugId(String mugId);

    List<MugEntity> findByOwnerUserId(String ownerUserId);

    void createMug(MugEntity mugEntity);

    Optional<MugEntity> updateMugByOwner(
            String mugId,
            String ownerUserId,
            LocalDate purchaseDate,
            LocalDate renewalDate,
            BigDecimal cost,
            BigDecimal sizeOunces,
            String nickname,
            boolean keepNickname,
            Instant updatedAt
    );

    boolean deleteMugByOwner(String mugId, String ownerUserId);

    void addHistoryEntry(MugHistoryEntity historyEntry);

    void addFill(MugFillEntity fill);
}
