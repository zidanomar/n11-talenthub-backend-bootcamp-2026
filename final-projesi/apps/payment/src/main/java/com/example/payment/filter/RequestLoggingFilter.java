package com.example.payment.filter;

import com.example.lib.web.RequestLoggingFilterSupport;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RequestLoggingFilter extends RequestLoggingFilterSupport {
    public RequestLoggingFilter() {
        super(LoggerFactory.getLogger(RequestLoggingFilter.class));
    }
}
