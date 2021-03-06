/*
 * Copyright 2006-2014 the original author or authors.
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

package com.consol.citrus.ws.config.xml;

import com.consol.citrus.config.util.BeanDefinitionParserUtils;
import com.consol.citrus.config.xml.AbstractEndpointParser;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.endpoint.EndpointConfiguration;
import com.consol.citrus.message.ErrorHandlingStrategy;
import com.consol.citrus.ws.client.WebServiceClient;
import com.consol.citrus.ws.client.WebServiceEndpointConfiguration;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser configures web service client bean definition.
 *
 * @author Christoph Deppisch
 * @since 1.4
 */
public class WebServiceClientParser extends AbstractEndpointParser {

    public static final String MESSAGE_SENDER_ATTRIBUTE = "message-sender";
    public static final String MESSAGE_SENDERS_ATTRIBUTE = "message-senders";
    public static final String REQUEST_URL_ATTRIBUTE = "request-url";

    @Override
    protected void parseEndpointConfiguration(BeanDefinitionBuilder endpointConfiguration, Element element, ParserContext parserContext) {
        super.parseEndpointConfiguration(endpointConfiguration, element, parserContext);

        BeanDefinitionParserUtils.setPropertyValue(endpointConfiguration, element.getAttribute(REQUEST_URL_ATTRIBUTE), "defaultUri");

        if (element.hasAttribute("web-service-template") && (element.hasAttribute("message-factory") ||
                element.hasAttribute("message-sender") ||
                element.hasAttribute("message-senders"))) {
            parserContext.getReaderContext().error("When providing a 'web-service-template' reference, none of " +
                    "'message-factory', '" + "message-sender" +
                    "', or '" + "message-senders" + "' should be set.", element);
        }

        if (!element.hasAttribute("request-url") && !element.hasAttribute("endpoint-resolver")) {
            parserContext.getReaderContext().error(String.format("One of the properties '%s' or '%s' is required!",
                    "request-url", "endpoint-resolver"), element);
        }

        if (element.hasAttribute("message-sender") && element.hasAttribute("message-senders")) {
            parserContext.getReaderContext().error(String.format("When '%s' is set, no '%s' attribute should be provided.",
                    "message-sender", "message-senders"), element);
        }

        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("web-service-template"), "webServiceTemplate");

        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("message-factory"), "messageFactory", "messageFactory");

        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute(MESSAGE_SENDER_ATTRIBUTE), "messageSender");
        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute(MESSAGE_SENDERS_ATTRIBUTE), "messageSenders");

        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("message-converter"), "messageConverter");

        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("interceptors"), "interceptors");
        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("interceptor"), "interceptor");
        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("message-correlator"), "correlator");
        BeanDefinitionParserUtils.setPropertyReference(endpointConfiguration, element.getAttribute("endpoint-resolver"), "endpointResolver");

        if (element.hasAttribute("fault-strategy")) {
            endpointConfiguration.addPropertyValue("errorHandlingStrategy",
                    ErrorHandlingStrategy.fromName(element.getAttribute("fault-strategy")));
        }

        BeanDefinitionParserUtils.setPropertyValue(endpointConfiguration, element.getAttribute("polling-interval"), "pollingInterval");
    }

    @Override
    protected Class<? extends Endpoint> getEndpointClass() {
        return WebServiceClient.class;
    }

    @Override
    protected Class<? extends EndpointConfiguration> getEndpointConfigurationClass() {
        return WebServiceEndpointConfiguration.class;
    }
}
