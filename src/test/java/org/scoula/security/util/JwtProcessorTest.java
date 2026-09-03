package org.scoula.security.util;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.scoula.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RootConfig.class, SecurityConfig.class })
@Log4j2
class JwtProcessorTest {

    @Autowired
    private JwtProcessor jwtProcessor;

    @Test
    void generateToken() {
        String username = "user0"; //id,pw인증이 성공하면 token을 생성함.
        String token = jwtProcessor.generateToken(username);
        System.out.println(token);
    }

    //jwt : eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1c2VyMCIsImlhdCI6MTc4MjcwMDY3MSwiZXhwIjoxNzgyNzAwOTcxfQ.KnQEqgqoOTwwd7dHvH0QOQ4AUQp0bCNc3Ng1uyDh03fWmH9LX9EMpvdNQsuAUfe1
    @Test
    void getUsername() {
        String token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1c2VyMCIsImlhdCI6MTc4MjcwMTE1OCwiZXhwIjoxNzgyNzAxNDU4fQ.rpOUHf6vYasLbVERljupIM1fgKuKB4gTclTswm9roUalHusyXgLh_5DMrXOSX9z1";
        String username = jwtProcessor.getUsername(token);
        System.out.println(username);
    }

    @Test
    void validateToken() {
        String token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1c2VyMCIsImlhdCI6MTc4MjcwMDY3MSwiZXhwIjoxNzgyNzAwOTcxfQ.KnQEqgqoOTwwd7dHvH0QOQ4AUQp0bCNc3Ng1uyDh03fWmH9LX9EMpvdNQsuAUfe1";
        boolean result = jwtProcessor.validateToken(token);
        System.out.println(result);
    }
}