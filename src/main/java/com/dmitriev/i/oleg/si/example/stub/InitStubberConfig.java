package com.dmitriev.i.oleg.si.example.stub;

import com.dmitriev.i.oleg.si.example.config.props.Queues;
import com.dmitriev.i.oleg.si.example.stub.handler.MqStubber;
import com.dmitriev.i.oleg.si.example.stub.handler.RestStubber;
import com.dmitriev.i.oleg.si.example.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RequestMethod;

@Profile("stub")
@Configuration
@RequiredArgsConstructor
public class InitStubberConfig {
    private final RestStubber restStubber;
    private final MqStubber mqStubber;
    private final Queues queues;

    @EventListener(ApplicationReadyEvent.class)
    public void initMqStubs() {
        mqStubber.stub(
                queues.getExternalServiceReq(),
                null,
                FileUtils.getResourceOrThrow("stub/response.json"),
                "$.[?(@.fieldName == 'fieldValue')]"
        );

        mqStubber.stub(
                queues.getExternalServiceReq(),
                null,
                FileUtils.getResourceOrThrow("stub/response.json")
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initRestStubs() {
        restStubber.stub(
                RequestMethod.GET,
                "api/test",
                null,
                "response.json"
        );

        restStubber.stub(
                RequestMethod.GET,
                "api/test2",
                null
        );
    }
}
