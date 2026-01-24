package com.overmild.mugs.mapper;

import com.overmild.mugs.entity.LocationEntity;
import com.overmild.mugs.model.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    Location toModel(LocationEntity entity);

    @Mapping(target = "mugs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    LocationEntity toEntity(Location model);
}

