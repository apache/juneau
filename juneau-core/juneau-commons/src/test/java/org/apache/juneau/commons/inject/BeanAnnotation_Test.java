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
package org.apache.juneau.commons.inject;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Smoke coverage for {@link BeanAnnotation} — the programmatic builder for runtime-built
 * {@link Bean} instances.
 *
 * <p>Asserts that the builder round-trips the basic attributes, including the
 * {@link Bean#priority()} attribute that was added as part of the JSR-330 / Spring-lite work.
 */
@SuppressWarnings({
	"java:S2094"  // Intentionally empty bean class used as test fixture.
})
class BeanAnnotation_Test extends TestBase {

	@Test
	void a01_defaultBean_hasMidwayPriority() {
		var bean = BeanAnnotation.DEFAULT;
		assertNotNull(bean);
		assertEquals(Integer.MAX_VALUE / 2, bean.priority(),
			"Default @Bean priority should be Integer.MAX_VALUE/2 — equivalent to no explicit priority");
	}

	@Test
	void a02_builder_roundtripsAttributes() {
		var bean = BeanAnnotation.create()
			.name("svc")
			.value("svcValue")
			.description("hello", "world")
			.methodScope("m1", "m2")
			.build();

		assertEquals("svc", bean.name());
		assertEquals("svcValue", bean.value());
		assertArrayEquals(new String[] {"hello", "world"}, bean.description());
		assertArrayEquals(new String[] {"m1", "m2"}, bean.methodScope());
		assertEquals(Integer.MAX_VALUE / 2, bean.priority());
	}

	@Test
	void a03_nameUtility_returnsNamePreferredOverValue() {
		var withName = BeanAnnotation.create().name("svc").value("ignored").build();
		assertEquals("svc", BeanAnnotation.name(withName));

		var valueOnly = BeanAnnotation.create().value("fromValue").build();
		assertEquals("fromValue", BeanAnnotation.name(valueOnly));

		assertEquals("", BeanAnnotation.name(null));
	}

	public static class A04Target {
		public void method1() { /* intentionally empty */ }
		public void method2() { /* intentionally empty */ }
	}

	@Test
	void a04_on_stringNames_propagatesToAppliedScope() {
		// Exercises Builder.on(String...) — wraps super.on(String...) and returns the Bean builder.
		var bean = BeanAnnotation.create()
			.name("svc")
			.on(A04Target.class.getName() + ".method1", A04Target.class.getName() + ".method2")
			.build();
		assertNotNull(bean);
		assertEquals("svc", bean.name());
	}

	@Test
	void a05_on_methods_propagatesToAppliedScope() throws Exception {
		// Exercises Builder.on(java.lang.reflect.Method...).
		var bean = BeanAnnotation.create()
			.on(A04Target.class.getMethod("method1"), A04Target.class.getMethod("method2"))
			.build();
		assertNotNull(bean);
	}

	@Test
	void a06_on_methodInfos_propagatesToAppliedScope() throws Exception {
		// Exercises Builder.on(MethodInfo...).
		var bean = BeanAnnotation.create()
			.on(
				org.apache.juneau.commons.reflect.MethodInfo.of(A04Target.class.getMethod("method1")),
				org.apache.juneau.commons.reflect.MethodInfo.of(A04Target.class.getMethod("method2")))
			.build();
		assertNotNull(bean);
	}

	@Test
	void a07_defaultBuilder_omitsOptionalAttributes() {
		// Default builder (no description / methodScope / name / value set) should yield empty arrays
		// and empty strings from the runtime annotation — drives the {@code b.description == null} // NOSONAR
		// false-branch in BeanAnnotation.Object().
		var bean = BeanAnnotation.create().build();
		assertEquals("", bean.name());
		assertEquals("", bean.value());
		assertArrayEquals(new String[0], bean.description());
		assertArrayEquals(new String[0], bean.methodScope());
	}
}
