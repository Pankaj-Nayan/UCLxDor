package com.Bd.or.Ballondor.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({FootballApiProperties.class, PredictionProperties.class})
public class AppConfig {
    @Bean
    RestClient footballRestClient(RestClient.Builder builder, FootballApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        return builder.baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
    }
}
