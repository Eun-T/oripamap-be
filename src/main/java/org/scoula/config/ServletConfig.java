package org.scoula.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@EnableWebMvc
@ComponentScan(basePackages = {
        "org.scoula.controller",
        "org.scoula.exception",
        "org.scoula.security.controller",
        "org.scoula.place.controller",
        "org.scoula.favorite.controller",
        "org.scoula.member.controller",
        "org.scoula.comment.controller",
        "org.scoula.editrequest.controller",
})
public class ServletConfig implements WebMvcConfigurer {
    //스프링 내부에서 사용하는 서블릿(jsp)와 관련된 설정하는 파일.

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/resources/index.html");
    }

    //Servlet 3.0 이상 파일 업로드 사용시
    @Bean //메서드를 호출했을 때 싱글톤빈을 만들어 리턴해주세요.
    public MultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //jsp에서 사용할 프론트용 자원들(js, css, img위치와 접근 주소 설정)
        registry
                .addResourceHandler("/resources/**")
                // url이 /resources/로 시작하는 모든 경로
                .addResourceLocations("/resources/");

        //vue로 작업한 내용을 나중에 tomcat밑에 build해서 넣을 예정.
        //vue에서는 css, img등의 파일이 assets폴더 아래 있음.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("/resources/assets/");

        // Swagger UI 리소스를 위한 핸들러 설정
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // Swagger WebJar 리소스 설정
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        // Swagger 리소스 설정
        registry.addResourceHandler("/swagger-resources/**")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/v2/api-docs")
                .addResourceLocations("classpath:/META-INF/resources/");
    }
    // webapp/resources/경로로 매핑

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        //서버의 결과를 넣을 jsp위치와 전체 경로를 결정
        InternalResourceViewResolver bean = new InternalResourceViewResolver();
        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/views/");
        bean.setSuffix(".jsp");
        registry.viewResolver(bean);
    }
}
