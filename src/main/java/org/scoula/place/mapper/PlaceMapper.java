package org.scoula.place.mapper;

import org.scoula.place.vo.PlaceVO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PlaceMapper {

    List<PlaceVO> findAll();

    List<PlaceVO> searchPlaces(String keyword);
}
