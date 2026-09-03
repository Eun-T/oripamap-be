package org.scoula.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardVO {
    private Long no;
    private String title;
    private String content;
    private String writer;
    private List<BoardAttachmentVO> attaches; //첨부파일 테이블, join필요함.
    //게시판은 다 보이고, 첨부파일 있을 때만 오른쪽에 붙여서 조인해줘.
    //left outer join이 필요함.
    private Date regDate;
    private Date updateDate;
}
