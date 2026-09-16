package org.scoula.place.mapper;

import org.scoula.place.vo.PlaceVO;
import org.scoula.place.vo.OripaPlaceVO;
import org.scoula.place.vo.OripaPlaceImageVO;
import org.scoula.place.vo.EventPlaceVO;
import org.scoula.place.vo.EventPlaceImageVO;
import org.scoula.place.vo.EventPlaceImageType;
import org.scoula.place.vo.TagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PlaceMapper {

    List<PlaceVO> findAll();

    List<PlaceVO> searchPlaces(String keyword);

    PlaceVO findById(Long id);

    PlaceVO findByPublicId(@Param("publicId") String publicId);

    List<TagVO> findTagsByPlaceId(Long placeId);

    Long findIdForUpdate(Long id);

    OripaPlaceVO findOripaByPlaceId(Long placeId);

    List<OripaPlaceImageVO> findOripaImagesByPlaceId(Long placeId);

    EventPlaceVO findEventByPlaceId(Long placeId);

    List<EventPlaceImageVO> findEventImagesByPlaceId(Long placeId);

    int deletePlace(Long id);

    int upsertOripa(OripaPlaceVO detail);

    int upsertEvent(EventPlaceVO detail);

    int insertOripaImage(@org.apache.ibatis.annotations.Param("placeId") Long placeId,
                        @org.apache.ibatis.annotations.Param("imageKey") String imageKey,
                        @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);

    int updateOripaImageOrder(@org.apache.ibatis.annotations.Param("placeId") Long placeId,
                             @org.apache.ibatis.annotations.Param("id") Long id,
                             @org.apache.ibatis.annotations.Param("sortOrder") int sortOrder);

    int deleteOripaImage(@org.apache.ibatis.annotations.Param("placeId") Long placeId,
                         @org.apache.ibatis.annotations.Param("id") Long id);

    int insertEventImage(@Param("placeId") Long placeId,
                         @Param("imageKey") String imageKey,
                         @Param("sortOrder") int sortOrder,
                         @Param("imageType") EventPlaceImageType imageType);

    int updateEventImageOrder(@Param("placeId") Long placeId,
                              @Param("id") Long id,
                              @Param("sortOrder") int sortOrder,
                              @Param("imageType") EventPlaceImageType imageType);

    int deleteEventImage(@Param("placeId") Long placeId,
                         @Param("id") Long id);
}
