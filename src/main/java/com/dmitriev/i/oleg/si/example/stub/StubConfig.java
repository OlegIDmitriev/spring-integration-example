package com.dmitriev.i.oleg.si.example.stub;

import com.dmitriev.i.oleg.si.example.config.props.Queues;
import com.dmitriev.i.oleg.si.example.stub.handler.MqStubHandler;
import com.dmitriev.i.oleg.si.example.stub.handler.MqStubber;
import com.dmitriev.i.oleg.si.example.stub.handler.RestStubber;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.jms.dsl.Jms;

import java.util.List;

@Profile("stub")
@Configuration
@RequiredArgsConstructor
public class StubConfig {
    private final IntegrationFlowContext integrationFlowContext;
    private final ConnectionFactory connectionFactory;
    private final Queues queues;

    @EventListener(ApplicationReadyEvent.class)
    public void initStubFlows() {
        var stubQueues = List.of(
                queues.getExternalServiceReq()
        );

        stubQueues.forEach(queue -> integrationFlowContext.registration(createReadFlow(queue)).register());
    }

    private IntegrationFlow createReadFlow(String queue) {
        return IntegrationFlow.from(
                        Jms.messageDrivenChannelAdapter(
                                Jms.container(connectionFactory, queue)
                        )
                )
                .enrichHeaders(spec -> spec.header("stubQueue", queue, true))
                .to(stubFlow());
    }

    @Bean
    public IntegrationFlow stubFlow() {
        return f -> f
                .handle(new MqStubHandler(mqStubber()))
                .handle(Jms.outboundAdapter(connectionFactory).destinationExpression("headers.replyTo"));
    }

    @Bean
    public MqStubber mqStubber() {
        return new MqStubber("integation_id");
    }

    @Bean
    public RestStubber restStubber() {
        return new RestStubber("integation_id");
    }
}
