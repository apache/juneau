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
package org.apache.juneau;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class ParentPropertyAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	ParentProperty a1 = ParentPropertyAnnotation.create()
		.description("a")
		.build();

	ParentProperty a2 = ParentPropertyAnnotation.create()
		.description("a")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "description", "[a]");
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var bc1 = MarshallingContext.create().annotations(a1).build();
		var bc2 = MarshallingContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	public static class D1 {
		@ParentProperty(
			description={ "a" }
		)
		public int f;
	}

	public static class D2 {
		@ParentProperty(
			description={ "a" }
		)
		public int f;
	}

	ParentProperty d1, d2;
	{
		try {
			d1 = D1.class.getField("f").getAnnotationsByType(ParentProperty.class)[0];
			d2 = D2.class.getField("f").getAnnotationsByType(ParentProperty.class)[0];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Property functionality tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class ParentBean {
		public int value = 42;
	}

	public static class TestBeanWithParentPropertyField {
		@ParentProperty
		public ParentBean parent;
	}

	public static class TestBeanWithParentPropertyMethod {
		private ParentBean parent;

		@ParentProperty
		protected void setParent(ParentBean parent) {
			this.parent = parent;
		}

		public ParentBean getParent() {
			return parent;
		}
	}

	@Test void e01_parentPropertyField() throws Exception {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(TestBeanWithParentPropertyField.class);
		var prop = cm.getParentProperty();
		assertNotNull(prop, "ParentProperty should be found");
		assertTrue(prop.canWrite(), "Should have setter");
		assertTrue(prop.canRead(), "Should have getter");

		var bean = new TestBeanWithParentPropertyField();
		var parent = new ParentBean();
		prop.set(bean, parent);
		assertSame(parent, prop.get(bean));
		assertSame(parent, bean.parent);
	}

	@Test void e02_parentPropertyMethod() throws Exception {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(TestBeanWithParentPropertyMethod.class);
		var prop = cm.getParentProperty();
		assertNotNull(prop, "ParentProperty should be found");
		assertTrue(prop.canWrite(), "Should have setter");
		assertTrue(prop.canRead(), "Should have getter");

		var bean = new TestBeanWithParentPropertyMethod();
		var parent = new ParentBean();
		prop.set(bean, parent);
		assertSame(parent, prop.get(bean));
		assertSame(parent, bean.getParent());
	}

	@Test void e03_parentPropertyNotFound() {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(String.class);
		var prop = cm.getParentProperty();
		assertNull(prop, "ParentProperty should not be found on String class");
	}

	public static class TestBeanWithReadOnlyParentProperty {
		private ParentBean parent = new ParentBean();

		@ParentProperty
		public ParentBean getParent() {
			return parent;
		}
	}

	@Test void e04_readOnlyParentProperty() throws Exception {
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(TestBeanWithReadOnlyParentProperty.class);
		var prop = cm.getParentProperty();
		assertNotNull(prop, "ParentProperty should be found even if read-only");
		assertFalse(prop.canWrite(), "Should not have setter");
		assertTrue(prop.canRead(), "Should have getter");

		var bean = new TestBeanWithReadOnlyParentProperty();
		var parent = (ParentBean)prop.get(bean);
		assertNotNull(parent, "Should be able to get parent");
		assertEquals(42, parent.value);

		// Verify that set() throws an exception
		var newParent = new ParentBean();
		var ex = assertThrows(ExecutableException.class, () -> prop.set(bean, newParent));
		assertTrue(ex.getMessage().contains("No setter defined"), "Should throw exception when trying to set read-only property");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Cyclic @ParentProperty graph serialization contract (behavior-pinning).
	//
	// A @ParentProperty back-reference that is also a normally-visible bean property forms a parent/child cycle.
	// Juneau does NOT auto-omit the back-reference on the write side, so the serialization contract is:
	//   - default config: maxDepth=100 size guard silently truncates -> finite (but semantically-incomplete) output, no throw.
	//   - detectRecursions(): fail-fast with a recursion SerializeException.
	//   - ignoreRecursions(): omit the repeated node -> round-trips cleanly (parent re-injected via @ParentProperty).
	//------------------------------------------------------------------------------------------------------------------

	public static class F_Parent {
		public String name;
		public F_Child child;
	}

	public static class F_Child {
		public String name;

		@ParentProperty
		public F_Parent parent;
	}

	// Builds a parent/child cycle: Parent -> child (Child) -> parent (@ParentProperty back-reference -> Parent).
	static F_Parent f_cyclicGraph() {
		var p = new F_Parent();
		p.name = "p";
		var c = new F_Child();
		c.name = "c";
		p.child = c;
		c.parent = p;
		return p;
	}

	@Test void f01_cyclicGraph_defaultConfig_truncatesFiniteNoThrow() {
		var p = f_cyclicGraph();
		// Default config does NOT throw: the maxDepth=100 size guard silently truncates the cycle to finite output.
		var json = assertDoesNotThrow(() -> JsonSerializer.DEFAULT.serialize(p));
		assertTrue(json.length() < 100_000, "Output should be finite (truncated at maxDepth): length=" + json.length());
		assertTrue(json.contains("\"name\":\"c\""), "Output should contain the traversed graph: " + json);
	}

	@Test void f02_cyclicGraph_detectRecursions_throws() {
		var p = f_cyclicGraph();
		var s = JsonSerializer.create().detectRecursions().build();
		assertThrowsWithMessage(SerializeException.class, "Recursion occurred", () -> s.serialize(p));
	}

	@Test void f03_cyclicGraph_ignoreRecursions_roundTrips() throws Exception {
		var p = f_cyclicGraph();
		var s = JsonSerializer.create().ignoreRecursions().build();
		var json = assertDoesNotThrow(() -> s.serialize(p));
		// The back-reference to the already-seen parent is omitted -> output is finite and does not re-nest.
		assertTrue(json.contains("\"name\":\"c\""), "Output should serialize the child: " + json);
		assertFalse(json.contains("\"child\":{\"child\""), "Cycle should be broken (no re-nesting): " + json);
		// Round-trips cleanly, and the parser re-injects the parent via @ParentProperty.
		var p2 = JsonParser.DEFAULT.parse(json, F_Parent.class);
		assertEquals("p", p2.name);
		assertEquals("c", p2.child.name);
		assertSame(p2, p2.child.parent, "Parser should re-inject the parent via @ParentProperty");
	}
}
