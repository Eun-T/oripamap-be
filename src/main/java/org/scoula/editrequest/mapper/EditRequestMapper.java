package org.scoula.editrequest.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EditRequestMapper {

    int insertEditRequest(
            @Param("placeId") Long placeId,
            @Param("userId") Long userId,
            @Param("requestTypes") String requestTypes,
            @Param("memo") String memo
    );
}