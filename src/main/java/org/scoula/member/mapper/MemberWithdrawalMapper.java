package org.scoula.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberWithdrawalMapper {
    Long findUserIdForUpdate(@Param("userId") Long userId);

    List<String> findCommentImageKeysByUserId(@Param("userId") Long userId);

    int detachOtherUsersReplies(@Param("userId") Long userId);

    int deleteCommentsByUserId(@Param("userId") Long userId);

    int deleteFavoritesByUserId(@Param("userId") Long userId);

    int deleteInquiriesByUserId(@Param("userId") Long userId);

    int deleteEditRequestsByUserId(@Param("userId") Long userId);

    int deleteRefreshTokensByUserId(@Param("userId") Long userId);

    int deleteUser(@Param("userId") Long userId);
}
