package com.dmitriev.i.oleg.si.example.config.integration.processor.handler;

import com.dmitriev.i.oleg.si.example.persitence.entity.Request;
import com.dmitriev.i.oleg.si.example.persitence.repository.RequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;

@Slf4j
@MessageEndpoint
public class SaveRequestHandler extends AbstractReplyProducingMessageHandler {
    @Autowired
    private RequestRepository requestRepository;

    @Override
    public @Nullable Object handleRequestMessage(@NonNull Message<?> requestMessage) {
        log.info("Save request");
        var request = (Request) requestMessage.getPayload();
        requestRepository.save(request);
        return requestMessage;
    }
}
