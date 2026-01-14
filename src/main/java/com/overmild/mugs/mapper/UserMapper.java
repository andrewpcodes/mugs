package com.overmild.mugs.mapper;

import com.overmild.mugs.entity.UserEntity;
import com.overmild.mugs.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {MugMapper.class})
public interface UserMapper {
    User toModel(UserEntity entity);
    UserEntity toEntity(User model);
}
