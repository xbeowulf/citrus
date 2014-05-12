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

package com.consol.citrus.vertx.endpoint;

import com.consol.citrus.endpoint.AbstractEndpoint;
import com.consol.citrus.messaging.Consumer;
import com.consol.citrus.messaging.Producer;
import com.consol.citrus.vertx.factory.CachingVertxInstanceManager;
import com.consol.citrus.vertx.factory.VertxInstanceManager;

/**
 * @author Christoph Deppisch
 * @since 1.4.1
 */
public class VertxEndpoint extends AbstractEndpoint {

    /** Vert.x instance */
    private VertxInstanceManager vertxInstanceManager = new CachingVertxInstanceManager();

    /**
     * Default constructor initializing endpoint configuration.
     */
    public VertxEndpoint() {
        this(new VertxEndpointConfiguration());
    }

    /**
     * Default constructor using endpoint configuration.
     *
     * @param endpointConfiguration
     */
    public VertxEndpoint(VertxEndpointConfiguration endpointConfiguration) {
        super(endpointConfiguration);
    }

    @Override
    public Producer createProducer() {
        return new VertxProducer();
    }

    @Override
    public Consumer createConsumer() {
        return new VertxConsumer(vertxInstanceManager.newInstance(getEndpointConfiguration()), getEndpointConfiguration(), getMessageListener());
    }

    @Override
    public VertxEndpointConfiguration getEndpointConfiguration() {
        return (VertxEndpointConfiguration) super.getEndpointConfiguration();
    }

    /**
     * Gets the vert.x instance factory.
     * @return
     */
    public VertxInstanceManager getVertxInstanceManager() {
        return vertxInstanceManager;
    }

    /**
     * Sets the vert.x instance factory.
     * @param vertxInstanceManager
     */
    public void setVertxInstanceManager(VertxInstanceManager vertxInstanceManager) {
        this.vertxInstanceManager = vertxInstanceManager;
    }
}
