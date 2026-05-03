package com.example.payment.config;

import com.iyzipay.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IyzipayConfig {

    @Value("${iyzipay.api-key:}")
    private String apiKey;

    @Value("${iyzipay.secret-key:}")
    private String secretKey;

    @Value("${iyzipay.base-url:https://sandbox-api.iyzipay.com}")
    private String baseUrl;

    @Bean
    public Options iyzipayOptions() {
        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        options.setBaseUrl(baseUrl);
        return options;
    }
}
