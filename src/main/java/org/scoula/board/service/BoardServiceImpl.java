package org.scoula.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.board.domain.BoardAttachmentVO;
import org.scoula.board.domain.BoardVO;
import org.scoula.board.dto.BoardDTO;
import org.scoula.board.mapper.BoardMapper;
import org.scoula.common.pagination.Page;
import org.scoula.common.pagination.PageRequest;
import org.scoula.common.util.UploadFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service //스프링 시작할 때 스캔해서 싱글톤으로 만들어줌.
@RequiredArgsConstructor //생성자호출할 때 Autowired해줌.
@Log4j2
public class BoardServiceImpl implements BoardService{

//    @Autowired
    final private BoardMapper mapper;

    private final static String BASE_DIR = "c:/upload/board";

    @Override
    public List<BoardDTO> getList() {

        //전처리하고
        //db처리해달라고 요청
        //db처리 싱글톤 객체 여기에서 써야함.
        return mapper.getList().stream().map(BoardDTO::of).toList();
        //List<BoardVO> --> Stream<BoardVO> --> Stream<BoardDTO> --> List<BoardDTO>
    }

    @Override
    public BoardDTO get(Long no) {
        log.info("서비스의 get() 호출됨...");
        return BoardDTO.of(mapper.get(no));
    }

    @Transactional
    @Override
    public BoardDTO create(BoardDTO board) {
        //1. 게시판 글쓰기에 넣었던 내용 vo로 바꿔서 db처리해달라고 요청.
        BoardVO vo = board.toVo();
        mapper.create(vo); //게시판no

        //2. upload()메서드 호출
        // 첨부파일 꺼내서 있으면
        // 2-1)서버컴퓨터에 파일만들어서 옮겨주고
        // 2-2)db의 첨부파일 테이블에 insert해서 넣음.
        List<MultipartFile> files = board.getFiles();
        if(files != null && !files.isEmpty()) {
            //2-1, 2-2처리하면 됨.
            upload(vo.getNo(), files);
        }
        return get(vo.getNo());
    }

    private void upload(Long bno, List<MultipartFile> files){
        //2-1 : 서버컴퓨터에 파일생성해서 넣고
        //2-2 : db처리 하고
        for(MultipartFile file : files) {
            if(file.isEmpty()) continue;
            try {
                String uploadPath = UploadFiles.upload(BASE_DIR, file);
                BoardAttachmentVO attach = BoardAttachmentVO.of(file, bno, uploadPath);
                mapper.createAttachment(attach); //bno 컬럼에 넣어주어야함.
            }catch(IOException e){
                throw new RuntimeException(e);   // @Transactional에서 감지, 자동 rollback
            }
        }
    }

    @Override
    public BoardDTO update(BoardDTO board) {
        log.info("update......" + board);
        mapper.update(board.toVo());
        return get(board.getNo());
    }

    @Override
    public BoardDTO delete(Long no) {
        log.info("delete...." + no);
        BoardDTO board = get(no);
        mapper.delete(no);
        return board;
    }

    @Override
    public BoardAttachmentVO getAttachment(Long no) {
        return mapper.getAttachment(no);
    }

    @Override
    public boolean deleteAttachment(Long no) {
        return mapper.deleteAttachment(no) == 1;
    }

    @Override
    public Page<BoardDTO> getPage(PageRequest pageRequest) {

        return null;
    }
}
