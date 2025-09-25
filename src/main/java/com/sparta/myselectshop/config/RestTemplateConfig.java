package com.sparta.myselectshop.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig  {

    //RestTemplate은 추가로 설정해줄 것이 많은 편이라 수등으로 빈으로 등록하는 편
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                // RestTemplate 으로 외부 API 호출 시 일정 시간이 지나도 응답이 없을 때
                // 무한 대기 상태 방지를 위해 강제 종료 설정
                .setConnectTimeout(Duration.ofMillis(5000)) // 5초
                .setReadTimeout(Duration.ofMillis(5000)) // 5초
                .build();

        // 강의에서 다룬 아래 .setConnectTimeout과 .setReadTimeout는 DEPRECATED 되었습니다. 위에 작성된 최신 버전의 코드로 대체합니다.
        //.setConnectTimeout(Duration.ofSeconds(5))
        //.setReadTimeout(Duration.ofSeconds(5))
        //.build();
    }
}