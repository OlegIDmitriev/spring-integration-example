package com.dmitriev.i.oleg.si.example.config.integration.processor.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

@Slf4j
public class LoggingErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        log.error("Gateway container listener exception: {}", t.getClass().getSimpleName(), t);
    }
}
