package org.scoula.security.controller;

import lombok.extern.log4j.Log4j2;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;

//@Controller
//@RequestMapping("/security")
@Log4j2
class SecurityController {

//    @GetMapping("/member")
//    public void doMember(Principal principal) {
//        log.info("username = " + principal.getName());
////        System.out.println("principal = " + principal);
//    }

    @GetMapping("/member")
    public void doMember(Authentication authentication) {
        UserDetails userDetails = (UserDetails)authentication.getPrincipal();

        log.info("UserDetails = " + userDetails);
        log.info("username = " + userDetails.getUsername());

    }

    @GetMapping("/admin")
    public void doAdmin(@AuthenticationPrincipal CustomUser customUser) {
        MemberVO member = customUser.getMember();
        log.info("username = " + member);
    }



    @GetMapping("/login")
    public void login(){
        log.info("로그인 화면 호출됨.");
    }

    //로그아웃필터가 로그아웃을 성공하고 나서 보여질 페이지
    @GetMapping("/logout")
    public void logout(){
        log.info("로그아웃 화 특정화면 호출됨.");
    }

    @GetMapping("/all") //
    // /security/all요청 --> 메서드에서 return이 없으면 jsp도 이 요청주소를 이용해서 찾음.
     public void doAll(){
        log.info("모두 접근 가능");
    }

//    @GetMapping("/member")
//    public void doMember(){
//        log.info("Member, Admin역할을 가진 id만 접근 가능");
//    }
//
//    @GetMapping("/admin")
//    public void doAdmin(){
//        log.info("Admin역할을 가진 id만 접근 가능");
//    }
}
