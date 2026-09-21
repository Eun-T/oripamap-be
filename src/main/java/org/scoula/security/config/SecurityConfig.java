package org.scoula.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.mybatis.spring.annotation.MapperScan;
import org.scoula.security.filter.AuthenticationErrorFilter;
import org.scoula.security.filter.JwtAuthenticationFilter;
import org.scoula.security.filter.JwtUsernamePasswordAuthenticationFilter;
import org.scoula.security.handler.CustomAccessDeniedHandler;
import org.scoula.security.handler.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@Log4j2
@MapperScan(basePackages = {
        "org.scoula.security.account.mapper",
        "org.scoula.security.refresh.mapper"
})
@ComponentScan(basePackages  = {"org.scoula.security"})
@RequiredArgsConstructor
public class SecurityConfig  extends WebSecurityConfigurerAdapter {
    @Value("${csrf.cookie-domain:}")
    private String csrfCookieDomain;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationErrorFilter authenticationErrorFilter;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 문자셋필터 ==> post방식으로 전달된 데이터 utf-8로 설정해서 받아야 한글깨지지 않음.
    // http의 body에 따라오므로 별도로 설정해주어야함.
    // ip, pw를 입력에 한글 깨지지 않게 설정, 모든 회원가입, 로그인은  post로 보내야함.
    public CharacterEncodingFilter encodingFilter() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        return encodingFilter;
    }

    @Bean
//    JWT 방식은 폼로그인과달리Spring Security의기
//    본인증필터를사용하지않고,
//    클라이언트→ JWT 토큰→ 커스텀필터
//            (OncePerRequestFilter 등) → SecurityContext 직접 설정
    public AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    // corsFilter() 대신 얘씀 나중에 삭제
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("https://oripamap.com");
        config.addAllowedOrigin("https://www.oripamap.com");
        config.addAllowedOrigin("http://192.168.50.146:5173");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

/*    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost"); // Capacitor Android
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }*/

    //시큐리티 적용하고 싶지 않은 요청 주소 등록.
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/assets/**", "/*", "/swagger-ui.html", "/webjars/**", "/swagger-resources/**", "/v2/api-docs");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        if (csrfCookieDomain != null && !csrfCookieDomain.isBlank()) {
            csrfTokenRepository.setCookieDomain(csrfCookieDomain);
        }

        http.cors()
                .configurationSource(corsConfigurationSource());

        http.addFilterBefore(encodingFilter(),
                        CsrfFilter.class)
                .addFilterBefore(authenticationErrorFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        jwtUsernamePasswordAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        http
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);

        http.httpBasic().disable()
                .csrf()
                .csrfTokenRepository(csrfTokenRepository)
                .ignoringAntMatchers(
                        "/api/auth/login",
                        "/api/member",
                        "/api/auth/email-verifications",
                        "/api/auth/email-verifications/verify")
                .ignoringRequestMatchers(new AppAuthCsrfRequestMatcher())
                .and()
                .formLogin().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.authorizeRequests()
                .antMatchers("/api/admin/inquiries/**")
                .hasRole("ADMIN")
                .antMatchers("/api/inquiries/**")
                .authenticated();

        http.authorizeRequests()
                .antMatchers(HttpMethod.GET, "/api/comments/recent")
                .permitAll()
                .antMatchers("/api/users/me")
                .authenticated()
                .antMatchers(HttpMethod.POST,
                        "/api/auth/email-verifications",
                        "/api/auth/email-verifications/verify")
                .permitAll();

    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        log.info("개인 계정 설정- ram에 만들어 테스트(in memory)");

        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());

//        auth.inMemoryAuthentication()
//                .withUser("admin")
////                .password("{noop}1234")
//                .password("$2a$10$CJEer.FckE2NtlHzgKRShemk8iSXL0JLPewdAQYLqDRXjG/PvKan6")
//                .roles("ADMIN", "MEMBER");
//
//        auth.inMemoryAuthentication()
//                .withUser("member")
////                .password("{noop}1234")
//                .password("$2a$10$XyJq1p6ZfcjhFNcYj253yOyAlvcVqeAbcbGOubKAnS1sHkNAzB/Wq")
//                .roles("MEMBER");

    }
}
