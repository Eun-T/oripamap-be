package org.scoula.board.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.board.domain.BoardAttachmentVO;
import org.scoula.board.domain.BoardVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//브라우저 <------> 컨트롤러(dto, 첨부파일받아서 저장할 list, 게시판첨부파일 보낼때 list)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "게시글 DTO")
public class BoardDTO {
    private Long no;
    @ApiModelProperty(value = "제목")
    private String title;
    @ApiModelProperty(value = "글 본문")
    private String content;
    @ApiModelProperty(value = "작성자")
    private String writer;
    @ApiModelProperty(value = "등록일")
    private Date regDate;
    @ApiModelProperty(value = "수정일")
    private Date updateDate;

    //브라우저 첨부파일 여러개 --> 컨트롤러(dto)
    //List<MultipartFile>
    List<MultipartFile> files = new ArrayList<>();

    //브라우저 <-- 컨트롤러(dto), db(board+첨부파일 테이블 join한 결과)
    //List<첨부파일vo>
    private List<BoardAttachmentVO> attaches;


    //dto --> vo
    public BoardVO toVo(){
        return BoardVO.builder()
                .no(no)
                .title(title)
                .content(content)
                .writer(writer)
                .attaches(attaches)
                .regDate(regDate)
                .updateDate(updateDate)
                .build();
    }

    //vo --> dto
    public static BoardDTO of(BoardVO vo) {
        //BoardDTO boardDTO = new BoardDTO();
        //vo에 있는 것을 꺼내서 dto에 넣어야함.
       return vo == null ? null : BoardDTO.builder()
               .no(vo.getNo())
               .title(vo.getTitle())
               .content(vo.getContent())
               .writer(vo.getWriter())
               .attaches(vo.getAttaches())
               .regDate(vo.getRegDate())
               .updateDate(vo.getUpdateDate())
               .build();
    }
}
