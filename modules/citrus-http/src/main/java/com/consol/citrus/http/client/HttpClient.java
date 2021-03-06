/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.http.client;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.endpoint.AbstractEndpoint;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.http.interceptor.LoggingClientInterceptor;
import com.consol.citrus.http.message.HttpMessage;
import com.consol.citrus.message.*;
import com.consol.citrus.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.Arrays;

/**
 * Http client sends messages via Http protocol to some Http server instance, defined by a request endpoint url. Synchronous response
 * messages are cached in local memory and receive operations are able to fetch responses from this cache later on.
 *
 * @author Christoph Deppisch
 * @since 1.4
 */
public class HttpClient extends AbstractEndpoint implements Producer, ReplyConsumer {
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(HttpClient.class);

    /** Store of reply messages */
    private CorrelationManager<Message> replyManager = new DefaultCorrelationManager<Message>();

    /** Retry logger */
    private static final Logger RETRY_LOG = LoggerFactory.getLogger("com.consol.citrus.MessageRetryLogger");

    /**
     * Default constructor initializing endpoint configuration.
     */
    public HttpClient() {
        super(new HttpEndpointConfiguration());
    }

    /**
     * Default constructor using endpoint configuration.
     * @param endpointConfiguration
     */
    protected HttpClient(HttpEndpointConfiguration endpointConfiguration) {
        super(endpointConfiguration);
    }

    @Override
    public HttpEndpointConfiguration getEndpointConfiguration() {
        return (HttpEndpointConfiguration) super.getEndpointConfiguration();
    }

    @Override
    public void send(Message message, TestContext context) {
        if (CollectionUtils.isEmpty(getEndpointConfiguration().getClientInterceptors())) {
            LoggingClientInterceptor loggingClientInterceptor = new LoggingClientInterceptor();
            loggingClientInterceptor.setMessageListener(context.getMessageListeners());

            getEndpointConfiguration().setClientInterceptors(Arrays.<ClientHttpRequestInterceptor>asList(loggingClientInterceptor));
        }

        HttpMessage httpMessage;
        if (message instanceof HttpMessage) {
            httpMessage = (HttpMessage) message;
        } else {
            httpMessage = new HttpMessage(message);
        }

        String correlationKey = getEndpointConfiguration().getCorrelator().getCorrelationKey(httpMessage);
        context.saveCorrelationKey(correlationKey, this);

        String endpointUri;
        if (getEndpointConfiguration().getEndpointUriResolver() != null) {
            endpointUri = getEndpointConfiguration().getEndpointUriResolver().resolveEndpointUri(httpMessage, getEndpointConfiguration().getRequestUrl());
        } else {
            endpointUri = getEndpointConfiguration().getRequestUrl();
        }

        log.info("Sending HTTP message to: '" + endpointUri + "'");

        if (log.isDebugEnabled()) {
            log.debug("Message to be sent:\n" + httpMessage.getPayload().toString());
        }

        HttpMethod method = getEndpointConfiguration().getRequestMethod();
        if (httpMessage.getRequestMethod() != null) {
            method = httpMessage.getRequestMethod();
        }

        HttpEntity<?> requestEntity = getEndpointConfiguration().getMessageConverter().convertOutbound(httpMessage, getEndpointConfiguration());

        getEndpointConfiguration().getRestTemplate().setErrorHandler(new InternalResponseErrorHandler(correlationKey));
        ResponseEntity<?> response = getEndpointConfiguration().getRestTemplate().exchange(endpointUri, method, requestEntity, String.class);

        log.info("HTTP message was successfully sent to endpoint: '" + endpointUri + "'");

        onReplyMessage(correlationKey, getEndpointConfiguration().getMessageConverter().convertInbound(response, getEndpointConfiguration()));
    }

    @Override
    public Message receive(TestContext context) {
        return receive(context.getCorrelationKey(this), context);
    }

    @Override
    public Message receive(String selector, TestContext context) {
        return receive(selector, context, getEndpointConfiguration().getTimeout());
    }

    @Override
    public Message receive(TestContext context, long timeout) {
        return receive(context.getCorrelationKey(this), context, timeout);
    }

    @Override
    public Message receive(String selector, TestContext context, long timeout) {
        long timeLeft = timeout;
        Message message = findReplyMessage(selector);

        while (message == null && timeLeft > 0) {
            timeLeft -= getEndpointConfiguration().getPollingInterval();

            if (RETRY_LOG.isDebugEnabled()) {
                RETRY_LOG.debug("Reply message did not arrive yet - retrying in " + (timeLeft > 0 ? getEndpointConfiguration().getPollingInterval() : getEndpointConfiguration().getPollingInterval() + timeLeft) + "ms");
            }

            try {
                Thread.sleep(timeLeft > 0 ? getEndpointConfiguration().getPollingInterval() : getEndpointConfiguration().getPollingInterval() + timeLeft);
            } catch (InterruptedException e) {
                RETRY_LOG.warn("Thread interrupted while waiting for retry", e);
            }

            message = findReplyMessage(selector);
        }

        if (message == null) {
            throw new ActionTimeoutException("Action timeout while receiving Http response from from server");
        }

        return message;
    }

    /**
     * Handles error response messages constructing a proper response message
     * which will be propagated to the respective reply handler for
     * further processing.
     */
    private class InternalResponseErrorHandler implements ResponseErrorHandler {

        /** Request message associated with this response error handler */
        private String correlationKey;

        /**
         * Default constructor provided with request message
         * associated with this error handler.
         */
        public InternalResponseErrorHandler(String correlationKey) {
            this.correlationKey = correlationKey;
        }

        /**
         * Check for error HTTP status code in response message.
         * Delegates to default Spring implementation.
         */
        public boolean hasError(ClientHttpResponse response) throws IOException {
            return new DefaultResponseErrorHandler().hasError(response);
        }

        /**
         * Handle error response message according to error strategy.
         */
        public void handleError(ClientHttpResponse response) throws IOException {
            if (getEndpointConfiguration().getErrorHandlingStrategy().equals(ErrorHandlingStrategy.PROPAGATE)) {
                Message responseMessage = getEndpointConfiguration().getMessageConverter().convertInbound(
                        new ResponseEntity(response.getBody(), response.getHeaders(), response.getStatusCode()), getEndpointConfiguration());
                onReplyMessage(correlationKey, responseMessage);
            } else if (getEndpointConfiguration().getErrorHandlingStrategy().equals(ErrorHandlingStrategy.THROWS_EXCEPTION)) {
                new DefaultResponseErrorHandler().handleError(response);
            } else {
                throw new CitrusRuntimeException("Unsupported error strategy: " + getEndpointConfiguration().getErrorHandlingStrategy());
            }
        }

    }

    /**
     * Saves reply message with correlation key to local store for later processing.
     * @param correlationKey
     * @param replyMessage the reply message.
     */
    public void onReplyMessage(String correlationKey, Message replyMessage) {
        replyManager.store(correlationKey, replyMessage);
    }

    /**
     * Tries to find reply message for correlation key from local store.
     * @param correlationKey
     * @return
     */
    public Message findReplyMessage(String correlationKey) {
        return replyManager.find(correlationKey);
    }

    /**
     * Creates a message producer for this endpoint for sending messages
     * to this endpoint.
     */
    @Override
    public Producer createProducer() {
        return this;
    }

    /**
     * Creates a message consumer for this endpoint. Consumer receives
     * messages on this endpoint.
     *
     * @return
     */
    @Override
    public SelectiveConsumer createConsumer() {
        return this;
    }

}
