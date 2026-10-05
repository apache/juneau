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
package org.apache.juneau.rest.server;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.stats.*;
import org.junit.jupiter.api.*;

/**
 * Tests that {@link Rest#children() @Rest(children)} sub-resources inherit the {@link Bean @Bean} method results and
 * {@link Bean @Bean} fields declared on their group resource, transitively through every nesting level, while
 * framework-managed beans and the children's own beans stay isolated.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // getBeanStore() returns the context's own (already-owned) BeanStore; the test doesn't own it.
})
class RestContext_ChildBeanInheritance_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	public static class Greeting {
		private final String text;
		public Greeting(String text) { this.text = text; }
		public String getText() { return text; }
	}

	public static class Counter {
		private final String origin;
		public Counter(String origin) { this.origin = origin; }
		public String getOrigin() { return origin; }
	}

	public static class Shout {
		private final String text;
		public Shout(String text) { this.text = text; }
		public String getText() { return text; }
	}

	/** Captures beans injected into a child's {@code @RestInit} hook. */
	public static class Captured {
		public Greeting greeting;
		public Counter counter;
		public Greeting getGreeting() { return greeting; }
		public Counter getCounter() { return counter; }
	}

	@Rest(path="/grandchild")
	public static class GrandChild {}

	@Rest(path="/child", children={GrandChild.class})
	public static class Child {
		static final Captured CAPTURED = new Captured();

		// A child-level @Bean method that composes a bean inherited from the group.
		@Bean public Shout shout(Greeting greeting) { return new Shout(greeting.getText().toUpperCase()); }

		@RestInit public void init(Greeting greeting, Counter counter) {
			CAPTURED.greeting = greeting;
			CAPTURED.counter = counter;
		}
	}

	@Rest(path="/sibling")
	public static class Sibling {
		@Bean public Counter counter() { return new Counter("sibling"); }
	}

	@Rest(path="/override")
	public static class OverridingChild {
		@Bean public Greeting greeting() { return new Greeting("override"); }
	}

	@Rest(children={Child.class, Sibling.class, OverridingChild.class})
	public static class Group {
		static final MethodExecStore GROUP_EXEC_STORE = MethodExecStore.create().build();

		@Bean public Counter counter = new Counter("field");

		@Bean(name="named") public Greeting namedGreeting = new Greeting("named");

		@Bean public Greeting greeting() { return new Greeting("group"); }

		// Framework-managed type: stays local to the group.
		@Bean public MethodExecStore methodExecStore() { return GROUP_EXEC_STORE; }
	}

	private static RestContext group() throws Exception {
		return new RestContext(argsOf(Group.class, Group::new));
	}

	private static RestContext child(RestContext parent, String path) {
		return parent.getRestChildren().asMap().get(path);
	}

	private static <T> T bean(RestContext ctx, Class<T> type) {
		return ctx.getBeanStore().getBean(type).orElse(null);
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - group beans reach children
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_childSeesGroupBeanMethodResult() throws Exception {
		var a = child(group(), "child");
		assertBean(bean(a, Greeting.class), "text", "group");
	}

	@Test void a02_childSeesGroupBeanField() throws Exception {
		var a = child(group(), "child");
		assertBean(bean(a, Counter.class), "origin", "field");
		assertBean(a.getBeanStore().getBean(Greeting.class, "named").orElse(null), "text", "named");
	}

	@Test void a03_childBeanMethodAndRestInitResolveGroupBeans() throws Exception {
		var a = child(group(), "child");
		assertBean(bean(a, Shout.class), "text", "GROUP");
		assertBean(Child.CAPTURED, "greeting{text},counter{origin}", "{group},{field}");
	}

	@Test void a04_grandchildReachesRoot() throws Exception {
		var a = child(child(group(), "child"), "grandchild");
		assertBean(bean(a, Greeting.class), "text", "group");
		assertBean(bean(a, Counter.class), "origin", "field");
		// The intermediate child's own @Bean results are inherited too.
		assertBean(bean(a, Shout.class), "text", "GROUP");
	}

	@Test void a05_sameInstanceSharedWithGroup() throws Exception {
		var a = group();
		assertSame(bean(a, Greeting.class), bean(child(a, "child"), Greeting.class));
		assertSame(bean(a, Counter.class), bean(child(a, "child"), Counter.class));
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - isolation
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_childOwnBeanWinsOverGroupBean() throws Exception {
		var a = group();
		assertBean(bean(child(a, "override"), Greeting.class), "text", "override");
		assertBean(bean(a, Greeting.class), "text", "group");
	}

	@Test void b02_childBeansDoNotLeakToGroupOrSiblings() throws Exception {
		var a = group();
		assertBean(bean(child(a, "sibling"), Counter.class), "origin", "sibling");
		assertBean(bean(child(a, "child"), Counter.class), "origin", "field");
		assertNull(bean(a, Shout.class));
		assertNull(bean(child(a, "sibling"), Shout.class));
	}

	@Test void b03_groupFrameworkBeansStayLocal() throws Exception {
		var a = group();
		assertSame(Group.GROUP_EXEC_STORE, bean(a, MethodExecStore.class));
		assertNotSame(Group.GROUP_EXEC_STORE, bean(child(a, "child"), MethodExecStore.class));
		assertNotSame(Group.GROUP_EXEC_STORE, child(a, "child").getMethodExecStore());
		assertNotSame(a.getRestChildren(), child(a, "child").getRestChildren());
	}
}
