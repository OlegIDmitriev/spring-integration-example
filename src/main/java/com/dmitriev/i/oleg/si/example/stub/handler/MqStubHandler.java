package com.dmitriev.i.oleg.si.example.stub.handler;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;

@RequiredArgsConstructor
public class MqStubHandler extends AbstractReplyProducingMessageHandler {
    private final MqStubber mqStubber;

    @Override
    public @Nullable Object handleRequestMessage(@NonNull Message<?> requestMessage) {
        var headers = requestMessage.getHeaders();
        var queue = headers.get("stubQueue", String.class);
        var requestBody = (String) requestMessage.getPayload();

        return mqStubber.getResponseBody(queue, requestBody, headers);
    }
}
