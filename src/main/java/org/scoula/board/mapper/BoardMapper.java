package org.scoula.board.mapper;

import org.scoula.board.domain.BoardAttachmentVO;
import org.scoula.board.domain.BoardVO;
import org.scoula.common.pagination.PageRequest;

import java.util.List;

public interface BoardMapper {

    //db필요한 작업 추상메서드(구현을 안하는 메서드, 불완전한 메서드)
    //전체 게시판 리스트를 db로 검색해줘. -->
    //row하나당 BoardVO에 넣어주고
    //많은 BoardVO목록은 List로 묶어줘.
    public abstract List<BoardVO> getList();

    BoardVO get(Long no);

    void create(BoardVO board);

    public int update(BoardVO board);

    public int delete(Long no);

    void createAttachment(BoardAttachmentVO attach);
    List<BoardAttachmentVO> getAttachmentList(Long bno);
    BoardAttachmentVO getAttachment(Long no);
    int deleteAttachment(Long no);

    int getTotalCount();

    List<BoardVO> getPage(PageRequest pageRequest);
}
