package com.example.gateway.filter;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RouteNotFoundFilter implements WebFilter, Ordered {

    private final RouteLocator routeLocator;

    public RouteNotFoundFilter(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars")
                || path.startsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        return routeLocator.getRoutes()
                .filterWhen(route -> Mono.from(route.getPredicate().apply(exchange)))
                .hasElements()
                .flatMap(hasRoute -> {
                    if (!hasRoute) {
                        exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
