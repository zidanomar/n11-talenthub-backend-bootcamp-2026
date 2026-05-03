package com.example.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuthHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        String userId = jwt.getToken().getSubject();
                        String roles = extractRealmRoles(jwt);
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Roles", roles)
                                .build();
                        return exchange.mutate().request(mutatedRequest).build();
                    }
                    return exchange;
                })
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private String extractRealmRoles(JwtAuthenticationToken jwt) {
        Object realmAccess = jwt.getToken().getClaim("realm_access");

        if (!(realmAccess instanceof Map<?, ?> claims)) {
            return "";
        }

        Object roles = claims.get("roles");

        if (!(roles instanceof List<?> roleList)) {
            return "";
        }

        return roleList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
