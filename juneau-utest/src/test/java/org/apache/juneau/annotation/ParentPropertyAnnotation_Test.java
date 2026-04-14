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
package org.apache.juneau.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.ExecutableException;
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
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
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
		var bc = BeanContext.DEFAULT;
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
		var bc = BeanContext.DEFAULT;
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
		var bc = BeanContext.DEFAULT;
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
		var bc = BeanContext.DEFAULT;
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
}
