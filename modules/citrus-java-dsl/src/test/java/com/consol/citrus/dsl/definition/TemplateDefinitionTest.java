/*
 * Copyright 2006-2012 the original author or authors.
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

package com.consol.citrus.dsl.definition;

import com.consol.citrus.TestAction;
import com.consol.citrus.actions.EchoAction;
import com.consol.citrus.actions.SleepAction;
import com.consol.citrus.container.*;
import com.consol.citrus.report.TestActionListeners;
import com.consol.citrus.testng.AbstractTestNGUnitTest;
import org.easymock.EasyMock;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;

public class TemplateDefinitionTest extends AbstractTestNGUnitTest {
    
    private ApplicationContext applicationContextMock = EasyMock.createMock(ApplicationContext.class);
    
    @Test
    public void testTemplateBuilder() {
        Template rootTemplate = new Template();
        rootTemplate.setName("fooTemplate");
        
        List<TestAction> actions = new ArrayList<TestAction>();
        actions.add(new EchoAction());
        actions.add(new SleepAction());
        rootTemplate.setActions(actions);
        
        reset(applicationContextMock);

        expect(applicationContextMock.getBean("fooTemplate", Template.class)).andReturn(rootTemplate).once();
        expect(applicationContextMock.getBean(TestActionListeners.class)).andReturn(new TestActionListeners()).once();
        expect(applicationContextMock.getBeansOfType(SequenceBeforeTest.class)).andReturn(new HashMap<String, SequenceBeforeTest>()).once();
        expect(applicationContextMock.getBeansOfType(SequenceAfterTest.class)).andReturn(new HashMap<String, SequenceAfterTest>()).once();

        replay(applicationContextMock);

        MockBuilder builder = new MockBuilder(applicationContextMock) {
            @Override
            public void configure() {
                template("fooTemplate")
                    .parameter("param", "foo")
                    .parameter("text", "Citrus rocks!");
            }
        };

        builder.execute();
        
        assertEquals(builder.testCase().getActions().size(), 1);
        assertEquals(builder.testCase().getActions().get(0).getClass(), Template.class);
        assertEquals(builder.testCase().getActions().get(0).getName(), "fooTemplate");
        
        Template container = (Template)builder.testCase().getActions().get(0);
        assertEquals(container.isGlobalContext(), true);
        assertEquals(container.getParameter().toString(), "{param=foo, text=Citrus rocks!}");
        assertEquals(container.getActions().size(), 2);
        assertEquals(container.getActions().get(0).getClass(), EchoAction.class);
        assertEquals(container.getActions().get(1).getClass(), SleepAction.class);
        
        verify(applicationContextMock);
    }
    
    @Test
    public void testTemplateBuilderGlobalContext() {
        Template rootTemplate = new Template();
        rootTemplate.setName("fooTemplate");
        
        List<TestAction> actions = new ArrayList<TestAction>();
        actions.add(new EchoAction());
        rootTemplate.setActions(actions);
        
        reset(applicationContextMock);

        expect(applicationContextMock.getBean("fooTemplate", Template.class)).andReturn(rootTemplate).once();
        expect(applicationContextMock.getBean(TestActionListeners.class)).andReturn(new TestActionListeners()).once();
        expect(applicationContextMock.getBeansOfType(SequenceBeforeTest.class)).andReturn(new HashMap<String, SequenceBeforeTest>()).once();
        expect(applicationContextMock.getBeansOfType(SequenceAfterTest.class)).andReturn(new HashMap<String, SequenceAfterTest>()).once();

        replay(applicationContextMock);

        MockBuilder builder = new MockBuilder(applicationContextMock) {
            @Override
            public void configure() {
                template("fooTemplate")
                    .globalContext(false);
            }
        };

        builder.execute();
        
        assertEquals(builder.testCase().getActions().size(), 1);
        assertEquals(builder.testCase().getActions().get(0).getClass(), Template.class);
        assertEquals(builder.testCase().getActions().get(0).getName(), "fooTemplate");
        
        Template container = (Template)builder.testCase().getActions().get(0);
        assertEquals(container.isGlobalContext(), false);
        assertEquals(container.getParameter().size(), 0L);
        assertEquals(container.getActions().size(), 1);
        assertEquals(container.getActions().get(0).getClass(), EchoAction.class);
        
        verify(applicationContextMock);
    }
}
