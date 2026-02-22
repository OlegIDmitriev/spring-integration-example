package com.dmitriev.i.oleg.si.example.config.integration.processor.transformer;

import com.dmitriev.i.oleg.si.example.config.integration.model.ExternalServiceResponse;
import com.dmitriev.i.oleg.si.example.config.integration.model.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

@Slf4j
public class ExternalServiceResponseTransformer extends AbstractPayloadTransformer<ExternalServiceResponse, ServiceResponse> {
    @Override
    protected ServiceResponse transformPayload(ExternalServiceResponse payload) {
        log.info("Transform external service response to service response");
        var response = new ServiceResponse();
        response.setExternalId(payload.getExternalId());
        return response;
    }

    @Override
    public String getComponentType() {
        return "message-transformer";
    }
}
