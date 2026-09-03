package org.scoula.board.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardAttachmentVO {
    private Long no;
    //첨부파일 번호, 자동 증가
    private Long bno;
    // FK: Board의 no(*************)
    private String filename;    // 원본 파일명
    private String path;
    // 서버에 저장된파일경로
    private String contentType; // 파일 mime-type
    private Long size;
    // 파일의크기
    private Date regDate;
    // 등록일

    //첨부파일 하나하나의 정보를 가지고 있는 객체에서 추출한 다음 vo에 넣어주어야함.
    //vo내의 필드(멤버변수)에 넣어줄때 방법
    //생성자(변수값, 변수값, ....)
    //set메서드 변수마다 다 호출해서 넣어주어도 됨.
    //builder이용해서 .변수명(값).변수명(값)....
    public static BoardAttachmentVO of(MultipartFile part, Long bno, String path){
        return builder()
                .bno(bno)
                .filename(part.getOriginalFilename())
                .path(path)
                .contentType(part.getContentType())
                .size(part.getSize())
                .build();
    }

}
