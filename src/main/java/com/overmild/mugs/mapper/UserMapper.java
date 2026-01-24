package com.overmild.mugs.mapper;

import com.overmild.mugs.entity.UserEntity;
import com.overmild.mugs.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MugMapper.class})
public interface UserMapper {

    User toModel(UserEntity entity);

    @Mapping(target = "mugs", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    UserEntity toEntity(User model);
}
