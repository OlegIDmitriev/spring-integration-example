package com.dmitriev.i.oleg.si.example.config.integration.processor.gateway;

import jakarta.jms.ExceptionListener;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.support.management.ManageableSmartLifecycle;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ObjectUtils;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
@Setter
public class AsyncJmsGateway extends AbstractJmsGateway implements ManageableSmartLifecycle, MessageListener {
    private final Object lifecycleMonitor = new Object();
    private final Object initializeMonitor = new Object();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean wasShutdown = new AtomicBoolean(false);
    private final DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();

    private ErrorHandler errorHandler = new LoggingErrorHandler();
    private ExceptionListener exceptionListener;
    private ContainerProperties containerProperties;

    public AsyncJmsGateway() {
        this.setAsync(true);
    }

    @Override
    protected void doInit() {
        synchronized (this.initializeMonitor) {
            if (this.initialized.get()) {
                return;
            }

            Assert.notNull(this.jmsTemplate, "jms template must not be null");
            Assert.notNull(this.inQueue, "nnQueue must not be null");
            Assert.notNull(this.outQueue, "outQueue must not be null");
            initContainer();
            this.initialized.set(true);
        }
    }


    @Override
    public void start() {
        synchronized (this.lifecycleMonitor) {
            if (!this.active.get()) {
                if (wasShutdown.get()) {
                    this.container.initialize();
                    wasShutdown.set(false);
                }
                this.container.start();
                this.active.set(true);
            }
        }
    }

    @Override
    public void stop() {
        synchronized (this.lifecycleMonitor) {
            this.container.shutdown();
            this.active.set(false);
            this.wasShutdown.set(true);
        }
    }

    @Override
    public boolean isRunning() {
        return this.active.get();
    }

    @Override
    protected @Nullable Object handleRequestMessage(org.springframework.messaging.@NonNull Message<?> requestMessage) {
        if (!this.initialized.get()) {
            afterPropertiesSet();
        }

        log.info("Send message to queue: {}", this.inQueue);

        jmsTemplate.send(inQueue, session -> {
            var jmsMessage = createJmsMessage(requestMessage, session);
            jmsMessage.setJMSReplyTo(resolveDestination(session, this.outQueue));
            return jmsMessage;
        });

        return null;
    }

    @Override
    public void onMessage(Message jmsMessage) {
        log.info("Read message from: {}", this.outQueue);
        sendOutput(convertReply(jmsMessage), null, false);
    }

    private void initContainer() {
        applyContainerProperties();
        container.setConnectionFactory(this.jmsTemplate.getConnectionFactory());
        container.setDestinationName(this.inQueue);
        container.setDestinationResolver(this.destinationResolver);
        if (StringUtils.isNotBlank(this.correlationValue)) {
            container.setMessageSelector("%s = '%s'".formatted(this.correlationHeader, this.correlationValue));
        }
        container.setMessageListener(this);
        container.setBackOff(new ExponentialBackOff());
        container.setObservationRegistry(this.getObservationRegistry());

        if (this.errorHandler != null) {
            container.setErrorHandler(this.errorHandler);
        }
        if (this.exceptionListener != null) {
            container.setExceptionListener(this.exceptionListener);
        }

        container.afterPropertiesSet();
    }

    private void applyContainerProperties() {
        if (this.containerProperties == null) {
            return;
        }

        JavaUtils.INSTANCE
                .acceptIfNotNull(containerProperties.getSessionTransacted(), container::setSessionTransacted)
                .acceptIfNotNull(containerProperties.getCacheLevel(), container::setCacheLevel)
                .acceptIfNotNull(containerProperties.getConcurrentConsumers(), container::setConcurrentConsumers)
                .acceptIfNotNull(containerProperties.getIdleConsumerLimit(), container::setIdleConsumerLimit)
                .acceptIfNotNull(containerProperties.getIdleTaskExecutionLimit(), container::setIdleTaskExecutionLimit)
                .acceptIfNotNull(containerProperties.getMaxConcurrentConsumers(), container::setMaxConcurrentConsumers)
                .acceptIfNotNull(containerProperties.getMaxMessagesPerTask(), container::setMaxMessagesPerTask)
                .acceptIfNotNull(containerProperties.getReceiveTimeout(), container::setReceiveTimeout)
                .acceptIfNotNull(containerProperties.getTaskExecutor(), container::setTaskExecutor)
                .acceptIfNotNull(containerProperties.getTransactionManager(), container::setTransactionManager);

        var acknowledgeMode = containerProperties.getSessionAcknowledgeMode();
        if (acknowledgeMode != null) {
            if (Session.SESSION_TRANSACTED == acknowledgeMode) {
                container.setSessionTransacted(true);
            } else {
                container.setSessionAcknowledgeMode(acknowledgeMode);
            }
        }

        if (containerProperties.getTaskExecutor() != null) {
            // set the beanName so the default TE threads get a meaningful name
            var containerBeanName = StringUtils.isNotBlank(this.getComponentName())
                    ? this.getComponentName()
                    : "AscyncJmsGateway@" + ObjectUtils.getIdentityHexString(this);
            container.setBeanName(containerBeanName + ".replyListener");
        }
    }

    @Getter
    @Setter
    public static class ContainerProperties {
        private Boolean sessionTransacted;
        private Integer sessionAcknowledgeMode;
        private Long receiveTimeout;
        private Long recoveryInterval;
        private Integer cacheLevel;
        private Integer concurrentConsumers;
        private Integer maxConcurrentConsumers;
        private Integer maxMessagesPerTask;
        private Integer idleConsumerLimit;
        private Integer idleTaskExecutionLimit;
        private Executor taskExecutor;
        private PlatformTransactionManager transactionManager;
    }
}
