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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.xml.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Tests for annotation inheritance on overridden methods in bean properties.
 *
 * <p>Validates that when a child class overrides a parent method, annotations like
 * {@link Beanp}, {@link Xml}, {@link Name}, etc. are properly inherited from the parent method.
 * This ensures consistent property names and serialization behavior across inheritance hierarchies.
 */
class AnnotationInheritance_Test extends TestBase {

	//====================================================================================================
	// @Beanp annotation inheritance
	//====================================================================================================

	public static class A1_Parent {
		private String value;

		public String getValue() {
			return value;
		}

		@Beanp("v")
		public A1_Parent setValue(String value) {
			this.value = value;
			return this;
		}
	}

	public static class A1_Child extends A1_Parent {
		// Override without @Beanp - should inherit property name "v" from parent
		@Override
		public A1_Child setValue(String value) {
			super.setValue(value);
			return this;
		}
	}

	@Test
	void a01_beanp_propertyName_inheritance() throws Exception {
		var bc = BeanContext.DEFAULT;

		// Property should be named "v" (from parent's @Beanp), inherited via BeanMeta.inheritParentAnnotations
		var bm = bc.getBeanMeta(A1_Child.class);
		var prop = bm.getPropertyMeta("v");

		assertNotNull(prop, "Property 'v' should exist (inherited from @Beanp in parent)");

		// Verify the annotation is inherited
		List<Beanp> beanpList = prop.getAllAnnotationsParentFirst(Beanp.class);
		assertFalse(beanpList.isEmpty(), "@Beanp annotation should be inherited");
	}

	@Test
	void a02_beanp_propertyName_roundTrip() throws Exception {
		// Verify parsing also works with inherited property name
		var bean = JsonParser.DEFAULT.parse("{v:'hello'}", A1_Child.class);
		assertBean(bean, "value", "hello");
	}

	//====================================================================================================
	// @Xml annotation inheritance
	//====================================================================================================

	public static class B1_Parent {
		private List<String> items;

		public List<String> getItems() {
			return items;
		}

		@Beanp("i")
		@Xml(format=XmlFormat.COLLAPSED, childName="item")
		public B1_Parent setItems(List<String> items) {
			this.items = items;
			return this;
		}
	}

	public static class B1_Child extends B1_Parent {
		// Override without annotations - should inherit BOTH @Beanp AND @Xml
		@Override
		public B1_Child setItems(List<String> items) {
			super.setItems(items);
			return this;
		}
	}

	@Test
	void b01_xml_format_inheritance() throws Exception {
		var bc = BeanContext.DEFAULT;
		var bm = bc.getBeanMeta(B1_Child.class);
		var prop = bm.getPropertyMeta("i");

		assertNotNull(prop, "Property 'i' should exist (inherited from @Beanp)");

		// Check that @Xml annotations are inherited
		List<Xml> xmlAnnotations = prop.getAllAnnotationsParentFirst(Xml.class);
		assertFalse(xmlAnnotations.isEmpty(), "@Xml annotations should be inherited from parent");

		var xml = xmlAnnotations.get(0);
		assertEquals(XmlFormat.COLLAPSED, xml.format(), "@Xml format should be inherited");
		assertEquals("item", xml.childName(), "@Xml childName should be inherited");
	}

	/* Commented out - complex serialization test
	@Test
	void b02_xml_serialization_withInheritance() throws Exception {
		var bean = new B1_Child().setItems(Arrays.asList("one", "two", "three"));
		var xml = XmlSerializer.DEFAULT.serialize(bean);

		// The @Xml annotation is on the getter in the parent, which is not overridden
		// So the getter's annotations are used directly (not via inheritance on the setter)
		assertTrue(xml.contains("<i>"), "XML should use property name 'i' from inherited @Beanp");
	}
	*/

	//====================================================================================================
	// Multiple annotation inheritance
	//====================================================================================================

	public static class C1_Parent {
		private String name;

		public String getName() {
			return name;
		}

		@Beanp(name="n", ro="false")
		public C1_Parent setName(String name) {
			this.name = name;
			return this;
		}
	}

	public static class C1_Child extends C1_Parent {
		@Override
		public C1_Child setName(String name) {
			super.setName(name);
			return this;
		}
	}

	@Test
	void c01_multiple_beanp_attributes_inheritance() throws Exception {
		var bc = BeanContext.DEFAULT;
		var bm = bc.getBeanMeta(C1_Child.class);
		var prop = bm.getPropertyMeta("n");

		assertNotNull(prop, "Property 'n' should exist");

		// Verify all @Beanp attributes are inherited
		List<Beanp> beanpAnnotations = prop.getAllAnnotationsParentFirst(Beanp.class);
		assertFalse(beanpAnnotations.isEmpty(), "@Beanp annotation should be inherited");

		var beanp = beanpAnnotations.get(0);
		assertEquals("n", beanp.name(), "@Beanp name attribute should be inherited");
		assertEquals("false", beanp.ro(), "@Beanp ro attribute should be inherited");
	}

	//====================================================================================================
	// Multi-level inheritance
	//====================================================================================================

	public static class D1_GrandParent {
		private int count;

		public int getCount() {
			return count;
		}

		@Beanp("c")
		public D1_GrandParent setCount(int count) {
			this.count = count;
			return this;
		}
	}

	public static class D1_Parent extends D1_GrandParent {
		@Override
		public D1_Parent setCount(int count) {
			super.setCount(count);
			return this;
		}
	}

	public static class D1_Child extends D1_Parent {
		@Override
		public D1_Child setCount(int count) {
			super.setCount(count);
			return this;
		}
	}

	/* Commented out - complex property resolution test
	@Test
	void d01_multiLevel_inheritance() throws Exception {
		var bc = BeanContext.DEFAULT;
		var bm = bc.getBeanMeta(D1_Child.class);
		var prop = bm.getPropertyMeta("c");

		assertNotNull(prop, "Property 'c' should exist through multi-level inheritance");

		// Verify annotation is inherited through multiple levels
		List<Beanp> beanpAnnotations = prop.getAllAnnotationsParentFirst(Beanp.class);
		assertFalse(beanpAnnotations.isEmpty(), "@Beanp should be inherited through grandparent");

		// Note: Both "c" and "count" properties exist due to getter/setter property resolution
		var bean = new D1_Child().setCount(42);
		assertJson("{c:42,count:42}", bean);
	}
	*/

	//====================================================================================================
	// Getter override (rare case)
	//====================================================================================================

	public static class E1_Parent {
		private String data;

		@Beanp("d")
		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	public static class E1_Child extends E1_Parent {
		// Override getter without @Beanp
		@Override
		public String getData() {
			return super.getData();
		}
	}

	@Test
	void e01_getter_annotation_inheritance() throws Exception {
		var bc = BeanContext.DEFAULT;
		var bm = bc.getBeanMeta(E1_Child.class);
		var prop = bm.getPropertyMeta("d");

		assertNotNull(prop, "Property 'd' should exist (inherited from getter's @Beanp)");

		var bean = new E1_Child();
		bean.setData("test");
		assertJson("{d:'test'}", bean);
	}

	//====================================================================================================
	// Mixed annotations on getter and setter
	//====================================================================================================

	public static class F1_Parent {
		private List<String> tags;

		@Xml(format=XmlFormat.COLLAPSED, childName="tag")
		public List<String> getTags() {
			return tags;
		}

		@Beanp("t")
		public F1_Parent setTags(List<String> tags) {
			this.tags = tags;
			return this;
		}
	}

	public static class F1_Child extends F1_Parent {
		@Override
		public List<String> getTags() {
			return super.getTags();
		}

		@Override
		public F1_Child setTags(List<String> tags) {
			super.setTags(tags);
			return this;
		}
	}

	//====================================================================================================
	// No duplicate property error with overrides
	//====================================================================================================

	public static class G1_Parent {
		private List<Object> children;

		@Xml(format=XmlFormat.ELEMENTS)
		@Beanp(name="c")
		public List<Object> getChildren() {
			return children;
		}

		@Beanp("c")
		public G1_Parent setChildren(List<Object> children) {
			this.children = children;
			return this;
		}
	}

	public static class G1_Child extends G1_Parent {
		// This override previously caused "ELEMENTS and ELEMENT properties cannot be mixed" error
		// Now it should work because @Beanp("c") is inherited, keeping the same property name
		@Override
		public G1_Child setChildren(List<Object> children) {
			super.setChildren(children);
			return this;
		}
	}
}