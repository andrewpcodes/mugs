package com.overmild.mugs.mapper;

import com.overmild.mugs.entity.LocationEntity;
import com.overmild.mugs.model.Location;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    Location toModel(LocationEntity entity);
    LocationEntity toEntity(Location model);
}

