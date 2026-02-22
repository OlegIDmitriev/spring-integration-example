package com.dmitriev.i.oleg.si.example.config.integration.processor.gateway;

import com.dmitriev.i.oleg.si.example.util.JsonUtils;
import jakarta.jms.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Getter
@Setter
public abstract class AbstractJmsGateway extends AbstractReplyProducingMessageHandler {
    protected final DestinationResolver destinationResolver = new DynamicDestinationResolver();


    protected String correlationHeader = "gatewayId";
    protected String correlationValue = UUID.randomUUID().toString();
    protected String replyToHeader = "replyTo";
    protected MessageConverter messageConverter = new SimpleMessageConverter();
    protected JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

    protected ConnectionFactory connectionFactory;
    protected JmsTemplate jmsTemplate;
    protected String inQueue;
    protected String outQueue;
    protected boolean serializeToJson;
    protected Class<?> jsonResponseClass;
    protected TypeReference<?> jsonResponseTypeRef;

    @Override
    public String getComponentType() {return "jms:outgoing-gateway";}

    protected Message createJmsMessage(org.springframework.messaging.Message<?> springMessage, Session session) throws JMSException {
        Message jmsMessage = null;

        if (this.serializeToJson) {
            var json = JsonUtils.toJson(springMessage.getPayload());
            jmsMessage = session.createTextMessage(json);
        } else {
            jmsMessage = this.messageConverter.toMessage(springMessage.getPayload(), session);
        }

        this.headerMapper.fromHeaders(springMessage.getHeaders(), jmsMessage);
        jmsMessage.setStringProperty(this.replyToHeader, this.outQueue);
        jmsMessage.setStringProperty(this.correlationHeader, this.correlationValue);
        logBody(jmsMessage);

        return jmsMessage;
    }

    protected Destination resolveDestination(Session session, String destinationName) throws JMSException {
        return this.destinationResolver.resolveDestinationName(session, destinationName, false);
    }

    @SneakyThrows
    protected org.springframework.messaging.Message<Object> convertReply(Message jmsMessage) {
        logBody(jmsMessage);
        jmsMessage.setJMSReplyTo(null);
        jmsMessage.setJMSCorrelationID(null);

        Map<String, Object> jmsReplyHeaders = this.headerMapper.toHeaders(jmsMessage);
        jmsReplyHeaders.remove(this.correlationHeader);
        jmsReplyHeaders.remove(this.replyToHeader);

        return MessageBuilder
                .withPayload(getResponseBody(jmsMessage))
                .copyHeaders(jmsReplyHeaders)
                .build();
    }

    protected Object getResponseBody(Message message) throws JMSException {
        if (message instanceof TextMessage && serializeToJson) {
            var body = ((TextMessage) message).getText();
            return JsonUtils.fromJson(body, jsonResponseClass);
        } else {
            if (message instanceof BytesMessage bytesMessage) {
                bytesMessage.reset();
            }

            return this.messageConverter.fromMessage(message);
        }
    }

    protected void logBody(Message message) {
        if (!log.isDebugEnabled()) return;

        try {
            var logBody = switch (message) {
                case TextMessage tm -> tm.getText();
                case BytesMessage bm -> getBodyString(bm);
                default -> message;
            };
            log.debug("Message: {}", logBody);
        } catch (JMSException e) {
            log.warn("Can't get message body", e);
        }
    }

    private String getBodyString(BytesMessage bytesMessage) throws JMSException {
        bytesMessage.reset();
        byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
        bytesMessage.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
