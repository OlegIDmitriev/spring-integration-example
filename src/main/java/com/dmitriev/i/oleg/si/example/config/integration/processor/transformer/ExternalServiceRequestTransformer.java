package com.dmitriev.i.oleg.si.example.config.integration.processor.transformer;

import com.dmitriev.i.oleg.si.example.config.integration.model.ExternalServiceRequest;
import com.dmitriev.i.oleg.si.example.persitence.entity.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.transformer.AbstractPayloadTransformer;

@Slf4j
public class ExternalServiceRequestTransformer extends AbstractPayloadTransformer<Request, ExternalServiceRequest> {
    @Override
    protected ExternalServiceRequest transformPayload(Request payload) {
        log.info("Transform request entity to external service request");
        var request = new ExternalServiceRequest();
        request.setIntegrationId(payload.getIntegrationId());
        request.setCreatedAt(payload.getCreatedAt());

        return request;
    }

    @Override
    public String getComponentType() {
        return "message-transformer";
    }
}
