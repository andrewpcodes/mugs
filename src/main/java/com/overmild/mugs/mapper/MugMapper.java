package com.overmild.mugs.mapper;

import com.overmild.mugs.entity.MugEntity;
import com.overmild.mugs.model.Mug;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {LocationMapper.class, UserMapper.class})
public interface MugMapper {
    Mug toModel(MugEntity entity);
    MugEntity toEntity(Mug model);
}


