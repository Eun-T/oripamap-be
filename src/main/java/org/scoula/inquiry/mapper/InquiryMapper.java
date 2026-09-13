package org.scoula.inquiry.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.inquiry.vo.InquiryVO;

import java.util.List;

@Mapper
public interface InquiryMapper {

    int insert(InquiryVO inquiry);

    InquiryVO findById(@Param("id") Long id);

    List<InquiryVO> findAllByUserId(@Param("userId") Long userId);

    InquiryVO findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateAnswer(@Param("id") Long id, @Param("answer") String answer);
}
