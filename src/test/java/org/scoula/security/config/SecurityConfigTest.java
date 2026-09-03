package org.scoula.security.config;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.scoula.config.RootConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RootConfig.class,
        SecurityConfig.class
})
@Log4j2
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder pwEncoder;


    @Test
    void passwordEncoder() {
        String str = "1234";

        String encode1 = pwEncoder.encode(str); //암호화1
        String encode2 = pwEncoder.encode(str); //암호화2

        System.out.println("1234암호화 1 " + encode1);
        System.out.println("1234암호화 2 " + encode2);

        System.out.println(pwEncoder.matches(str, encode1));
        System.out.println(pwEncoder.matches(str, encode2));
    }
}