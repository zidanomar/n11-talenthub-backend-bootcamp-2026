package com.example.lib.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

public abstract class RequestLoggingFilterSupport extends OncePerRequestFilter {
    private final Logger log;

    protected RequestLoggingFilterSupport(Logger log) {
        this.log = log;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            log.info("{} {} {} {}ms",
                    request.getMethod(),
                    buildUri(request),
                    response.getStatus(),
                    System.currentTimeMillis() - start);
        }
    }

    private String buildUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? uri + "?" + query : uri;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }
}
