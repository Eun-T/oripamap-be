package org.scoula.favorite.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.place.vo.PlaceVO;
import java.util.List;

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

    List<PlaceVO> findPlacesByUserId(@Param("userId") Long userId);
}
