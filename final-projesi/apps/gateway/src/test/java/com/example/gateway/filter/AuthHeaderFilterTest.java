package com.example.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthHeaderFilterTest {

    private final AuthHeaderFilter filter = new AuthHeaderFilter();

    private Jwt sampleJwt(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", "test@test.com")
                .build();
    }

    @Test
    void filter_withValidJwt_injectsXUserIdHeader() {
        var jwt = sampleJwt("user-123");
        var auth = new JwtAuthenticationToken(jwt);

        var request = MockServerHttpRequest.get("/api/products").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(inv -> {
            var ex = inv.getArgument(0, org.springframework.web.server.ServerWebExchange.class);
            assertThat(ex.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("user-123");
            return Mono.empty();
        });

        var result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void filter_withoutAuthentication_passesThrough() {
        var request = MockServerHttpRequest.get("/api/products").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        var result = filter.filter(exchange, chain);

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withNonJwtAuthentication_passesThrough() {
        var auth = new TestingAuthenticationToken("user", "creds");

        var request = MockServerHttpRequest.get("/api/products").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        var result = filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(exchange);
    }

    @Test
    void getOrder_returnsMinusOne() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
