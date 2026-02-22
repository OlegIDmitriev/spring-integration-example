package com.dmitriev.i.oleg.si.example.config;

import com.dmitriev.i.oleg.si.example.config.props.ArtemisProps;
import com.dmitriev.i.oleg.si.example.config.props.CachingFactoryProps;
import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@RequiredArgsConstructor
public class BrokerConfig {
    private final ArtemisProps artemisProps;
    private final CachingFactoryProps cachingFactoryProps;

    @Bean
    public ConnectionFactory connectionFactory() {
        var artemisFactory = new ActiveMQConnectionFactory(artemisProps.getUrl(), artemisProps.getUsername(), artemisProps.getPassword());
        var cachingFactory = new CachingConnectionFactory(artemisFactory);
        cachingFactory.setCacheConsumers(cachingFactoryProps.isCacheConsumers());
        cachingFactory.setCacheProducers(cachingFactoryProps.isCacheProducers());
        cachingFactory.setReconnectOnException(cachingFactoryProps.isReconnectOnExceptions());
        cachingFactory.setSessionCacheSize(cachingFactoryProps.getSessionCacheSize());

        return cachingFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        var jmsTemplate = new JmsTemplate(connectionFactory());
        jmsTemplate.setSessionTransacted(true);
        return jmsTemplate;
    }
}
