package com.enit.satellite_platform.config;

import org.springframework.beans.factory.annotation.Value;
/**
 * Configuration class for WebClient setup in the application.
 * This class configures a WebClient bean with customized settings for handling
 * HTTP requests and responses, particularly focusing on memory buffer configuration.
 *
 * Features:
 * - Configures WebClient with increased memory buffer  for handling large payloads
 * - Uses custom exchange strategies for optimized request/response handling
 * - Provides a singleton WebClient bean for use throughout the application
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;





@Configuration
public class WebClientConfig {

    @Value("${web_client.memory.buffer.size}")
    private int bufferSize;

    /**
     * Creates a WebClient bean configured for handling large payloads.
     * 
     * This method sets up the WebClient with a custom ExchangeStrategies 
     * configuration that increases the maximum in-memory buffer size to 10MB.
     * This is particularly useful for processing large HTTP responses without
     * running into memory issues. The configured WebClient can be injected
     * and used throughout the application for making HTTP requests.
     *
     * @return a configured instance of WebClient
     */
    @Bean
    public WebClient webClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> {
                ClientCodecConfigurer.ClientDefaultCodecs codecs = config.defaultCodecs();
                codecs.maxInMemorySize(bufferSize);
            })
            .build();
        return WebClient.builder()
            .exchangeStrategies(strategies)
            .build();
    }
}
