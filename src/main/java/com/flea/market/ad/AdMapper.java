package com.flea.market.ad;

import com.flea.market.ad.dto.AdResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdMapper {

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author.login", target = "authorLogin")
    AdResponse toResponse(Ad ad);
}
