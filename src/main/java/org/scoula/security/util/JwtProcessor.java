package org.scoula.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Date;

@Component //프로젝트 시작할 때 싱글톤으로 만들고 시작.
public class JwtProcessor {
    static private final long TOKEN_VALID_MILISECOND = 1000L * 60 * 30; // 30 분

    private final Key key;

    public JwtProcessor(@Value("${jwt.secret}") String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 환경변수가 필요합니다.");
        }

        final byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(encodedSecret.trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT_SECRET은 Base64 형식이어야 합니다.", e);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET은 디코딩 후 32바이트 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    //JWT생성
    public String generateToken(Long userId){
        return generateToken(userId, Duration.ofMillis(TOKEN_VALID_MILISECOND));
    }

    public String generateToken(Long userId, Duration validity) {
        Date issuedAt = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(issuedAt)
                .setExpiration(new Date(issuedAt.getTime() + validity.toMillis()))
                .signWith(key)
                .compact();
    }

    //username(id역할하는 클레임) --> 그대로 사용, db검색해도 됨.
    public Long getUserId(String token){
        String subject = Jwts.parserBuilder()
                .setSigningKey(key) //검증용 키 설정
                .build()
                .parseClaimsJws(token) //유효성 검증
                .getBody()
                .getSubject(); //id에 해당하는 클레임 추출
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new MalformedJwtException("JWT subject가 사용자 ID 형식이 아닙니다.");
        }
    }

    //JWT유효성 검증
    public boolean validateToken(String token){
        //유효성 검증할 때 검증용 항목이 많음.
        //유효성 검증할 때 항목별로 예외상황 발생하도록 설정.
        //문제가 생기면 예외가 발생될 예정임.
        Jws<Claims> claims =
                Jwts.parserBuilder()
                .setSigningKey(key) //검증용 키 설정
                .build()
                .parseClaimsJws(token); //유효성 검증
            //- ExpiredJwtException: 유효 시간 만기
            //⁃ UnsupportedJwtException: 지원하지 않은 JWT
            //⁃ MalformedJwtException: 잘못된 JWT 포맷 예외
            //⁃ SignatureException: 서명 불일치 예외
            //⁃ IllegalArgumentException: 잘못된 정보 포함
            //리턴하기 전에 parseClaimsJws()실행 시 예외상황이 발생하면 중단됨.
        return true; //위에서 문제가 생기지 않으면 true리턴.
    }
}
