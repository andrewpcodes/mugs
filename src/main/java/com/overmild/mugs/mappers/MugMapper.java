package com.overmild.mugs.mappers;

import com.overmild.mugs.models.MugFillResponse;
import com.overmild.mugs.models.MugHistoryResponse;
import com.overmild.mugs.models.MugResponse;
import com.overmild.mugs.models.entities.MugEntity;
import com.overmild.mugs.models.entities.MugFillEntity;
import com.overmild.mugs.models.entities.MugHistoryEntity;
import org.mapstruct.Mapper;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface MugMapper {

    MugResponse toMugResponse(MugEntity mugEntity);

    MugHistoryResponse toMugHistoryResponse(MugHistoryEntity historyEntry);

    MugFillResponse toMugFillResponse(MugFillEntity fill);

    default String map(Instant value) {
        return value == null ? null : value.toString();
    }
}
