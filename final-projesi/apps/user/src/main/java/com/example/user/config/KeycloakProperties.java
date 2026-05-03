package com.example.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String serverUrl;
    private String realm;
    private Admin admin = new Admin();
    private Client client = new Client();

    @Getter @Setter
    public static class Admin {
        private String username;
        private String password;
    }

    @Getter @Setter
    public static class Client {
        private String id;
        private String secret;
    }
}
