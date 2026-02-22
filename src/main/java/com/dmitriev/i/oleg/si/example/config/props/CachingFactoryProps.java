package com.dmitriev.i.oleg.si.example.config.props;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Valid
@Getter
@Setter
@ConfigurationProperties("caching-factory")
public class CachingFactoryProps {
    private boolean cacheConsumers;
    private boolean cacheProducers;
    private boolean reconnectOnExceptions;
    @Positive
    private int sessionCacheSize;
}
