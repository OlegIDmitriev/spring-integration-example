package com.dmitriev.i.oleg.si.example.config.integration.flow;

import com.dmitriev.i.oleg.si.example.config.integration.processor.gateway.AsyncJmsGateway;
import com.dmitriev.i.oleg.si.example.config.integration.processor.handler.SaveRequestHandler;
import com.dmitriev.i.oleg.si.example.config.integration.processor.transformer.ExternalServiceRequestTransformer;
import com.dmitriev.i.oleg.si.example.config.integration.model.ExternalServiceResponse;
import com.dmitriev.i.oleg.si.example.config.integration.model.ServiceRequest;
import com.dmitriev.i.oleg.si.example.config.integration.processor.transformer.RequestTransformer;
import com.dmitriev.i.oleg.si.example.config.props.Queues;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@RequiredArgsConstructor
public class FlowConfig {
    private final PlatformTransactionManager transactionManager;
    private final ConnectionFactory connectionFactory;
    private final JmsTemplate jmsTemplate;
    private final Queues queues;

    @Bean
    public IntegrationFlow flow() {
        return IntegrationFlow.from(Jms.messageDrivenChannelAdapter(
                        Jms.container(connectionFactory, queues.getServiceIn())
                                .backOff(new ExponentialBackOff())
                                .transactionManager(transactionManager)))
                .log(m -> "Read message from queue %s: %s".formatted(queues.getServiceIn(), m.getPayload()))
                .transform(Transformers.fromJson(ServiceRequest.class))
                .transform(new RequestTransformer())
                .handle(new SaveRequestHandler())
                .transform(new ExternalServiceRequestTransformer())
                .handle(gateway(
                        queues.getExternalServiceReq(),
                        queues.getExternalServiceResp(),
                        ExternalServiceResponse.class))
                .transform(Transformers.toJson())
                .log(m -> "Send response to " + queues.getServiceOut())
                .handle(Jms.outboundAdapter(connectionFactory).destination(queues.getServiceOut()))
                .get()
                ;
    }

    private AsyncJmsGateway gateway(String inQueue, String outQueue, Class<?> responseClass) {
        var gateway = new AsyncJmsGateway();
        gateway.setJmsTemplate(jmsTemplate);
        gateway.setInQueue(inQueue);
        gateway.setOutQueue(outQueue);
        gateway.setSerializeToJson(true);
        gateway.setJsonResponseClass(responseClass);
        gateway.setCorrelationValue("myGateway");

        var properties = new AsyncJmsGateway.ContainerProperties();
        properties.setSessionTransacted(true);
        properties.setTransactionManager(transactionManager);
        gateway.setContainerProperties(properties);

        return gateway;
    }
}
