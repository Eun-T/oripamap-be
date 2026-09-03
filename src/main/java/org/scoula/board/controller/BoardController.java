package org.scoula.board.controller;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.board.dto.BoardDTO;
import org.scoula.board.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@Controller //jsp, json(data)바로 전달 가능
@RestController //json로만 전달 가능
@RequestMapping("/api/board")
@Log4j2
@RequiredArgsConstructor
@Api(tags = "게시글 관리")
public class BoardController {

    //@Autowired
    //컨트롤러 클래스에서 사용할 서비스 싱글톤객체를 찾아서 넣어주세요.
    //필요한 라이브러리에 이 클래스가 의존적이다.(dependency, 의존성)
    //의존성 찾아서 넣어야함.(의존성 주입, dependency injection)
    //DI
    //예전에는 프로그래머가 new로 객체만들고, 어디있는지 찾고 했음.
    //스프링이 프로그램이 지금은 함.
    //프로그래밍 제어를 프로그래머가 안하고 스프링같은 프레임워크가 해서
    //프로그래밍 제어가 역전되었음.
    //제어의 역전(Inversion of Control, IoC)
    final private BoardService service; //주소주입(di)

    @ApiOperation(value = "게시글목록", notes = "게시글목록을얻는API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "성공적으로요청이처리되었습니다.", response = BoardDTO.class),
            @ApiResponse(code = 400, message = "잘못된요청입니다."),
            @ApiResponse(code = 500, message = "서버에서오류가발생했습니다.")
    })
    @GetMapping("") // /api/board
    public List<BoardDTO> getList() {
        return service.getList(); //List<BoardDTO>을 json으로 바꾸어서 전달
    }

    @ApiOperation(value = "상세정보얻기", notes = "게시글상제정보를얻는API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "성공적으로요청이처리되었습니다.", response = BoardDTO.class),
            @ApiResponse(code = 400, message = "잘못된요청입니다."),
            @ApiResponse(code = 500, message = "서버에서오류가발생했습니다.")
    })
    @GetMapping("/{no}") // /api/board/1
    public ResponseEntity<BoardDTO> get(
            @ApiParam(value = "게시글 ID", required = true, example = "1")
            @PathVariable Long no) {
        return ResponseEntity.ok(service.get(no));
    }

    @ApiOperation(value = "게시글생성", notes = "게시글생성API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "성공적으로요청이처리되었습니다.", response = BoardDTO.class),
            @ApiResponse(code = 400, message = "잘못된요청입니다."),
            @ApiResponse(code = 500, message = "서버에서오류가발생했습니다.")
    })
    @PostMapping("")
    public ResponseEntity<BoardDTO> create(
            @ApiParam(value = "게시글객체", required = true)
            BoardDTO board) {
        return ResponseEntity.ok(service.create(board));
    }

    @ApiOperation(value = "게시글수정", notes = "게시글수정API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "성공적으로요청이처리되었습니다.", response = BoardDTO.class),
            @ApiResponse(code = 400, message = "잘못된요청입니다."),
            @ApiResponse(code = 500, message = "서버에서오류가발생했습니다.")
    })
    @PutMapping("/{no}")
    public ResponseEntity<BoardDTO> update(
            @ApiParam(value = "게시글ID", required = true, example = "1")
            @PathVariable Long no,
            @ApiParam(value = "게시글객체", required = true)
            BoardDTO board) {
        return ResponseEntity.ok(service.update(board));
    }


    @ApiOperation(value = "게시글삭제", notes = "게시글삭제API")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "성공적으로요청이처리되었습니다."),
            @ApiResponse(code = 400, message = "잘못된요청입니다."),
            @ApiResponse(code = 500, message = "서버에서오류가발생했습니다.")
    })
    @DeleteMapping("/{no}")
    public ResponseEntity<BoardDTO> delete(
            @ApiParam(value = "게시글ID", required = true, example = "1")
            @PathVariable Long no) {
        return ResponseEntity.ok(service.delete(no));
    }

    @DeleteMapping("/deleteAttachment/{no}")
    public ResponseEntity<Boolean> deleteAttachment(
            @PathVariable Long no) throws Exception {
        return ResponseEntity.ok(service.deleteAttachment(no));
    }


//    @GetMapping("/test") // /WEB-INF/views/test.jsp
//    public void test(){
//        System.out.println("test");
//    }
//
//    @GetMapping("/test2") // dto --> json으로 바꾸어서 바로 응답
//    public @ResponseBody test2(){
//        System.out.println("test");
//        BoardDTO dto = new BoardDTO();
//        return dto;
//    }
}
