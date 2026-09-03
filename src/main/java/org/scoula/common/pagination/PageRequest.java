package org.scoula.common.pagination;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access= AccessLevel.PRIVATE)
public class PageRequest {
    private int page; //요청하는 페이지 번호
    private int amount; //한 페이지에 보여줄 개수

    public PageRequest(){
        page = 1;
        amount = 10;
    }

//    private PageRequest(int page, int amount){
//        this.page = page;
//        this.amount = amount;
//    }

    public static PageRequest of(int page, int amount){
        return new PageRequest(page, amount); //파라메터있는 생성자를 이용한 객체생성할 수 있음.
    }

    public int getOffset(){
        return (page - 1) * amount;
    }
}
