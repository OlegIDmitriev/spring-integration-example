package com.dmitriev.i.oleg.si.example.config.props;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Valid
@Getter
@Setter
@ConfigurationProperties("queues")
public class Queues {
    private String serviceIn;
    private String serviceOut;
    private String externalServiceReq;
    private String externalServiceResp;
}
