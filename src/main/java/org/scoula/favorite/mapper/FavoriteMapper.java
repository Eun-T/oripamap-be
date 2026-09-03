package org.scoula.favorite.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper {

    int insertFavorite(
            @Param("userId") Long userId,
            @Param("placeId") Long placeId
    );

    int deleteFavorite(
            @Param("userId") Long userId,
            @Param("placeId") Long placeId
    );

    int existsFavorite(
            @Param("userId") Long userId,
            @Param("placeId") Long placeId
    );
}