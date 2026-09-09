package org.scoula.place.mapper;

import org.scoula.place.vo.PlaceVO;
import org.scoula.place.vo.OripaPlaceVO;
import org.scoula.place.vo.OripaPlaceImageVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PlaceMapper {

    List<PlaceVO> findAll();

    List<PlaceVO> searchPlaces(String keyword);

    PlaceVO findById(Long id);

    Long findIdForUpdate(Long id);

    OripaPlaceVO findOripaByPlaceId(Long placeId);

    List<OripaPlaceImageVO> findOripaImagesByPlaceId(Long placeId);

    int deletePlace(Long id);

    int upsertOripa(OripaPlaceVO detail);

    int insertOripaImage(@org.apache.ibatis.annotations.Param("placeId") Long placeId,
                        @org.apache.ibatis.annotations.Param("imageKey") String imageKey,
                        @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);

    int updateOripaImageOrder(@org.apache.ibatis.annotations.Param("placeId") Long placeId,
                             @org.apache.ibatis.annotations.Param("id") Long id,
                             @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);

    int deleteOripaImage(@org.apache.ibatis.annotations.Param("placeId") Long placeId,
                         @org.apache.ibatis.annotations.Param("id") Long id);
}
