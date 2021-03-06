/*
 * Copyright 2006-2010 the original author or authors.
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

package com.consol.citrus.junit;

import com.consol.citrus.config.CitrusSpringConfig;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.context.TestContextFactory;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Abstract base test implementation for test cases that rather use JUnit testing framework. Class also provides 
 * test listener support and loads the root application context files for Citrus.
 * 
 * @author Christoph Deppisch
 */
@TestExecutionListeners({TestSuiteAwareExecutionListener.class})
@ContextConfiguration(classes = CitrusSpringConfig.class)
public abstract class AbstractJUnit4CitrusTest extends AbstractJUnit4SpringContextTests {
    /** Logger */
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private TestContextFactory testContextFactory;
    
    /** Delegate test execution to this executor */
    private JUnitTestExecutor testExecutor;
    
    /**
     * Execute the test case.
     */
    protected void executeTest() {
        testExecutor  = createExecutor();
        testExecutor.executeTest(prepareTestContext(createTestContext()));
    }
    
    /**
     * Prepares the test context.
     *
     * Provides a hook for test context modifications before the test gets executed.
     *
     * @param testContext the test context.
     * @return the (prepared) test context.
     */
    protected TestContext prepareTestContext(final TestContext testContext) {
        return testContext;
    }
    
    /**
     * Creates a new test context.
     * @return the new citrus test context.
     * @throws Exception on error.
     */
    protected TestContext createTestContext() {
        try {
            return testContextFactory.getObject();
        } catch (Exception e) {
            throw new CitrusRuntimeException("Failed to create test context", e);
        }
    }

    /**
     * Creates the JUnit test executor.
     * @return
     */
    protected JUnitTestExecutor createExecutor() {
        return new JUnitTestExecutor(applicationContext, getClass());
    }
}
