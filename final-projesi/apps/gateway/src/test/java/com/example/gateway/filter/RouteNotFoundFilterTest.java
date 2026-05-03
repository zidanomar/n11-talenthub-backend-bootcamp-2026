package com.example.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RouteNotFoundFilterTest {

    private RouteLocator locatorWithMatchingRoute() {
        var locator = mock(RouteLocator.class);
        var route = mock(Route.class, RETURNS_DEEP_STUBS);
        when(route.getPredicate().apply(any(ServerWebExchange.class))).thenReturn(Mono.just(true));
        when(locator.getRoutes()).thenReturn(Flux.just(route));
        return locator;
    }

    private RouteLocator emptyLocator() {
        var locator = mock(RouteLocator.class);
        when(locator.getRoutes()).thenReturn(Flux.empty());
        return locator;
    }

    @Test
    void filter_knownRoute_passesThrough() {
        var filter = new RouteNotFoundFilter(locatorWithMatchingRoute());
        var request = MockServerHttpRequest.get("/api/products").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_unknownRoute_returns404() {
        var filter = new RouteNotFoundFilter(emptyLocator());
        var request = MockServerHttpRequest.get("/unknown/path").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(WebFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_actuatorPath_skipsRouteCheckAndPassesThrough() {
        var locator = mock(RouteLocator.class);
        var filter = new RouteNotFoundFilter(locator);
        var request = MockServerHttpRequest.get("/actuator/health").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(locator, never()).getRoutes();
        verify(chain).filter(exchange);
    }

    @Test
    void filter_swaggerUiPath_skipsRouteCheck() {
        var locator = mock(RouteLocator.class);
        var filter = new RouteNotFoundFilter(locator);
        var request = MockServerHttpRequest.get("/swagger-ui/index.html").build();
        var exchange = MockServerWebExchange.from(request);

        var chain = mock(WebFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(locator, never()).getRoutes();
    }

    @Test
    void getOrder_returnsHighestPrecedence() {
        var filter = new RouteNotFoundFilter(mock(RouteLocator.class));
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
