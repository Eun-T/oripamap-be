package org.scoula.common.pagination;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor(access= AccessLevel.PRIVATE)
//브라우저가 page번호와 amount를 보내면 PageRequest가 받아서 db검색한후
//서버가 응답해야함. 응답용 사용할 예정.
//전체페이지 리스트를 만들어야하므로 page에 대한 정보 + page에 해당하는 게시판 리스트를 함께
//보내주어야함.
public class Page<T> {
    private int totalCount; //전체 게시물 개수, 23개
    private int totalPage; //전체 페이지 수, 23개, 한 페이지당 10개 --> 3개 페이지

    //브라우저에 이것을 포함할 필요가 없으면 json으로 만들 때 제외시켜도 됨.
    @JsonIgnore
    private PageRequest pageRequest; //pageRequest에 있는 amount를 꺼내서 totalPage계산해주어야함.

    //해당 page의 db검색 리스트
    private List<T> list;

    public static <T> Page of(PageRequest pageRequest, int totalCount, List<T> list){
        int totalPage =  (int)Math.ceil((double)totalCount / pageRequest.getAmount());
        // 20/10 --> 2
        // 23/10 --> 자바는 정수/정수는 무조건 결과가 정수 --> 2
        // 23.0/10 --> 자바는 하나 이상이 실수이면 실수/정수, 정수/실수, 실수/실수이면 무조건 결과가 실수 --> 2.3
        //소수점이 있을 때 무조건 하나를 더 올려라!!! Math.ceil() 무조건 올림 <--> Math.floor()
        //Math.round() 반올림
        return new Page(totalCount, totalPage, pageRequest, list);
    }

    //page객체만 만들어도 메서드 호출해서 pageRequest의 값 쉽게 불러서 쓸 수 있게 만들어둠.
    public int getPageNum() { return pageRequest.getPage(); }
    public int getAmount() { return pageRequest.getAmount(); }
}
