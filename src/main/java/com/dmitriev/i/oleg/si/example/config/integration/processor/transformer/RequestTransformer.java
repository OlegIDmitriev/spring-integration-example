package com.dmitriev.i.oleg.si.example.config.integration.processor.transformer;

import com.dmitriev.i.oleg.si.example.config.integration.model.ServiceRequest;
import com.dmitriev.i.oleg.si.example.persitence.entity.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

@Slf4j
public class RequestTransformer extends AbstractPayloadTransformer<ServiceRequest, Request> {
    @Override
    protected Request transformPayload(ServiceRequest payload) {
        log.info("Transform service request to request entity");
        var request = new Request();
        request.setIntegrationId(payload.getIntegrationId());

        return request;
    }

    @Override
    public String getComponentType() {
        return "message-transformer";
    }
}
