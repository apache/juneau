/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.springboot;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Tests that {@link Rest#children() @Rest(children)} sub-resources of a {@link SpringRestServlet} group resolve both
 * Spring-context-only beans (via the root's {@link SpringBeanStore}) and the group's own {@link Bean @Bean} methods.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@SuppressWarnings({
	"resource" // getBeanStore() returns the context's own (already-owned) BeanStore; the test doesn't own it.
})
class SpringRestServlet_ChildBeanInheritance_Test extends TestBase {

	public static class SpringOnly {
		public String getOrigin() { return "spring"; }
	}

	public static class GroupBean {
		public String getOrigin() { return "group"; }
	}

	@Rest(path="/grandchild")
	public static class GrandChild {}

	@Rest(path="/child", children={GrandChild.class})
	public static class Child {}

	@Rest(children={Child.class})
	public static class Group extends BasicSpringRestServletGroup {
		private static final long serialVersionUID = 1L;

		@Bean public GroupBean groupBean() { return new GroupBean(); }
	}

	private static AnnotationConfigApplicationContext appContext;

	@BeforeAll static void setUp() {
		appContext = new AnnotationConfigApplicationContext();
		appContext.registerBean(SpringOnly.class);
		appContext.refresh();
	}

	@AfterAll static void tearDown() {
		appContext.close();
	}

	private static RestContext group() throws Exception {
		var group = new Group();
		appContext.getAutowireCapableBeanFactory().autowireBean(group);
		return new RestContext(new RestContext.Args(Group.class, null, null, () -> group, null, null, null, null, null, null));
	}

	private static RestContext child(RestContext parent, String path) {
		return parent.getRestChildren().asMap().get(path);
	}

	private static <T> T bean(RestContext ctx, Class<T> type) {
		return ctx.getBeanStore().getBean(type).orElse(null);
	}

	@Test void a01_childSeesSpringOnlyBean() throws Exception {
		assertBean(bean(child(group(), "child"), SpringOnly.class), "origin", "spring");
	}

	@Test void a02_childSeesGroupBean() throws Exception {
		assertBean(bean(child(group(), "child"), GroupBean.class), "origin", "group");
	}

	@Test void a03_grandchildSeesSpringOnlyAndGroupBeans() throws Exception {
		var a = child(child(group(), "child"), "grandchild");
		assertBean(bean(a, SpringOnly.class), "origin", "spring");
		assertBean(bean(a, GroupBean.class), "origin", "group");
	}
}
