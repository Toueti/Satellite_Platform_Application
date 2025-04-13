package com.enit.satellite_platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // Configure exchange strategies with increased memory buffer
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> {
                ClientCodecConfigurer.ClientDefaultCodecs codecs = config.defaultCodecs();
                // Set buffer size to 10MB (10 * 1024 * 1024 bytes)
                codecs.maxInMemorySize(10 * 1024 * 1024);
            })
            .build();

        return WebClient.builder()
            .exchangeStrategies(strategies)
            .build();
    }
}
