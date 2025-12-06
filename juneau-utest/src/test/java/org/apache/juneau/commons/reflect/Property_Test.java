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
package org.apache.juneau.commons.reflect;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Property_Test extends TestBase {

	public static class TestClass {
		public String publicField;
		private String privateField;
		private int intField;

		public void setPublicField(String value) {
			this.publicField = value;
		}

		public String getPublicField() {
			return publicField;
		}

		public void setPrivateField(String value) {
			this.privateField = value;
		}

		public String getPrivateField() {
			return privateField;
		}

		public void setIntField(int value) {
			this.intField = value;
		}

		public int getIntField() {
			return intField;
		}
	}

	//====================================================================================================
	// Property.create() - static factory method
	//====================================================================================================
	@Test
	void a001_create() {
		var builder = Property.create();
		assertNotNull(builder);
	}

	//====================================================================================================
	// Property.Builder.getter(Function) and setter(BiConsumer) - basic usage
	//====================================================================================================
	@Test
	void a002_functionGetterAndSetter() throws Exception {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		var obj = new TestClass();
		prop.set(obj, "testValue");
		assertEquals("testValue", prop.get(obj));
		assertEquals("testValue", obj.getPublicField());
	}

	//====================================================================================================
	// Property.Builder.getter(Function) - getter only
	//====================================================================================================
	@Test
	void a003_getterOnly() throws Exception {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.build();

		var obj = new TestClass();
		obj.setPublicField("testValue");
		assertEquals("testValue", prop.get(obj));
		assertTrue(prop.hasGetter());
		assertFalse(prop.hasSetter());
	}

	//====================================================================================================
	// Property.Builder.setter(BiConsumer) - setter only
	//====================================================================================================
	@Test
	void a004_setterOnly() throws Exception {
		var prop = Property.<TestClass, String>create()
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		var obj = new TestClass();
		prop.set(obj, "testValue");
		assertEquals("testValue", obj.getPublicField());
		assertFalse(prop.hasGetter());
		assertTrue(prop.hasSetter());
	}

	//====================================================================================================
	// Property.get() - with null producer
	//====================================================================================================
	@Test
	void a005_get_withNullProducer() throws Exception {
		var prop = Property.<TestClass, String>create()
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		var obj = new TestClass();
		var ex = assertThrows(ExecutableException.class, () -> prop.get(obj));
		assertTrue(ex.getMessage().contains("No getter defined"));
	}

	//====================================================================================================
	// Property.set() - with null consumer
	//====================================================================================================
	@Test
	void a006_set_withNullConsumer() throws Exception {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.build();

		var obj = new TestClass();
		var ex = assertThrows(ExecutableException.class, () -> prop.set(obj, "test"));
		assertTrue(ex.getMessage().contains("No setter defined"));
	}

	//====================================================================================================
	// Property.get() - with null object
	//====================================================================================================
	@Test
	void a007_get_withNullObject() {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.build();

		assertThrows(IllegalArgumentException.class, () -> prop.get(null));
	}

	//====================================================================================================
	// Property.set() - with null object
	//====================================================================================================
	@Test
	void a008_set_withNullObject() {
		var prop = Property.<TestClass, String>create()
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		assertThrows(IllegalArgumentException.class, () -> prop.set(null, "test"));
	}

	//====================================================================================================
	// Property.Builder.field(FieldInfo) - public field
	//====================================================================================================
	@Test
	void a009_field_publicField() throws Exception {
		var field = ClassInfo.of(TestClass.class).getPublicFields().stream()
			.filter(f -> f.getName().equals("publicField"))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, String>create()
			.field(field)
			.build();

		var obj = new TestClass();
		prop.set(obj, "testValue");
		assertEquals("testValue", prop.get(obj));
		assertEquals("testValue", obj.publicField);
	}

	//====================================================================================================
	// Property.Builder.field(FieldInfo) - private field
	//====================================================================================================
	@Test
	void a010_field_privateField() throws Exception {
		var field = ClassInfo.of(TestClass.class).getDeclaredFields().stream()
			.filter(f -> f.getName().equals("privateField"))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, String>create()
			.field(field)
			.build();

		var obj = new TestClass();
		prop.set(obj, "testValue");
		assertEquals("testValue", prop.get(obj));
		assertEquals("testValue", obj.getPrivateField());
	}

	//====================================================================================================
	// Property.Builder.field(FieldInfo) - with null field
	//====================================================================================================
	@Test
	void a011_field_withNullField() {
		assertThrows(IllegalArgumentException.class, () -> {
			Property.<TestClass, String>create()
				.field(null)
				.build();
		});
	}

	//====================================================================================================
	// Property.Builder.getter(MethodInfo) - method getter
	//====================================================================================================
	@Test
	void a012_getter_methodInfo() throws Exception {
		var getter = ClassInfo.of(TestClass.class).getPublicMethods().stream()
			.filter(m -> m.hasName("getPublicField"))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, String>create()
			.getter(getter)
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		var obj = new TestClass();
		obj.setPublicField("testValue");
		assertEquals("testValue", prop.get(obj));
	}

	//====================================================================================================
	// Property.Builder.setter(MethodInfo) - method setter
	//====================================================================================================
	@Test
	void a013_setter_methodInfo() throws Exception {
		var setter = ClassInfo.of(TestClass.class).getPublicMethods().stream()
			.filter(m -> m.hasName("setPublicField") && m.hasParameterTypes(String.class))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.setter(setter)
			.build();

		var obj = new TestClass();
		prop.set(obj, "testValue");
		assertEquals("testValue", obj.getPublicField());
	}

	//====================================================================================================
	// Property.Builder.getter(MethodInfo) - with null method
	//====================================================================================================
	@Test
	void a014_getter_withNullMethod() {
		assertThrows(IllegalArgumentException.class, () -> {
			Property.<TestClass, String>create()
				.getter((MethodInfo)null)
				.build();
		});
	}

	//====================================================================================================
	// Property.Builder.setter(MethodInfo) - with null method
	//====================================================================================================
	@Test
	void a015_setter_withNullMethod() {
		assertThrows(IllegalArgumentException.class, () -> {
			Property.<TestClass, String>create()
				.setter((MethodInfo)null)
				.build();
		});
	}

	//====================================================================================================
	// Property.Builder.field() - with primitive type
	//====================================================================================================
	@Test
	void a016_field_primitiveType() throws Exception {
		var field = ClassInfo.of(TestClass.class).getDeclaredFields().stream()
			.filter(f -> f.getName().equals("intField"))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, Integer>create()
			.field(field)
			.build();

		var obj = new TestClass();
		prop.set(obj, 42);
		assertEquals(Integer.valueOf(42), prop.get(obj));
		assertEquals(42, obj.getIntField());
	}

	//====================================================================================================
	// Property.set() - with null value
	//====================================================================================================
	@Test
	void a017_set_withNullValue() throws Exception {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		var obj = new TestClass();
		obj.setPublicField("initial");
		prop.set(obj, null);
		assertNull(prop.get(obj));
		assertNull(obj.getPublicField());
	}

	//====================================================================================================
	// Property.get() - exception handling
	//====================================================================================================
	@Test
	void a018_get_exceptionHandling() {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> {
				throw new RuntimeException("Test exception");
			})
			.build();

		var obj = new TestClass();
		var ex = assertThrows(RuntimeException.class, () -> prop.get(obj));
		assertEquals("Test exception", ex.getMessage());
	}

	//====================================================================================================
	// Property.set() - exception handling
	//====================================================================================================
	@Test
	void a019_set_exceptionHandling() {
		var prop = Property.<TestClass, String>create()
			.setter((obj, val) -> {
				throw new RuntimeException("Test exception");
			})
			.build();

		var obj = new TestClass();
		var ex = assertThrows(RuntimeException.class, () -> prop.set(obj, "test"));
		assertEquals("Test exception", ex.getMessage());
	}

	//====================================================================================================
	// Property.get() - ExecutableException pass-through
	//====================================================================================================
	@Test
	void a020_get_executableExceptionPassThrough() {
		var originalEx = new ExecutableException("Original exception");
		var prop = Property.<TestClass, String>create()
			.getter(obj -> {
				throw originalEx;
			})
			.build();

		var obj = new TestClass();
		var ex = assertThrows(ExecutableException.class, () -> prop.get(obj));
		assertSame(originalEx, ex);
	}

	//====================================================================================================
	// Property.set() - ExecutableException pass-through
	//====================================================================================================
	@Test
	void a021_set_executableExceptionPassThrough() {
		var originalEx = new ExecutableException("Original exception");
		var prop = Property.<TestClass, String>create()
			.setter((obj, val) -> {
				throw originalEx;
			})
			.build();

		var obj = new TestClass();
		var ex = assertThrows(ExecutableException.class, () -> prop.set(obj, "test"));
		assertSame(originalEx, ex);
	}

	//====================================================================================================
	// Property.get() - checked exception wrapping
	//====================================================================================================
	@Test
	void a022_get_checkedExceptionWrapping() {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> {
				throw new java.io.IOException("IO error");
			})
			.build();

		var obj = new TestClass();
		var ex = assertThrows(RuntimeException.class, () -> prop.get(obj));
		assertNotNull(ex.getCause());
		assertTrue(ex.getCause() instanceof java.io.IOException);
		assertEquals("IO error", ex.getCause().getMessage());
	}

	//====================================================================================================
	// Property.set() - checked exception wrapping
	//====================================================================================================
	@Test
	void a023_set_checkedExceptionWrapping() {
		var prop = Property.<TestClass, String>create()
			.setter((obj, val) -> {
				throw new java.io.IOException("IO error");
			})
			.build();

		var obj = new TestClass();
		var ex = assertThrows(RuntimeException.class, () -> prop.set(obj, "test"));
		assertNotNull(ex.getCause());
		assertTrue(ex.getCause() instanceof java.io.IOException);
		assertEquals("IO error", ex.getCause().getMessage());
	}

	//====================================================================================================
	// Property.Builder - chaining methods
	//====================================================================================================
	@Test
	void a024_builder_chaining() throws Exception {
		var prop = Property.<TestClass, String>create()
			.getter(obj -> obj.getPublicField())
			.setter((obj, val) -> obj.setPublicField(val))
			.build();

		assertNotNull(prop);
		assertTrue(prop.hasGetter());
		assertTrue(prop.hasSetter());
	}

	//====================================================================================================
	// Property.Builder.field() - complete round trip
	//====================================================================================================
	@Test
	void a025_field_roundTrip() throws Exception {
		var field = ClassInfo.of(TestClass.class).getPublicFields().stream()
			.filter(f -> f.getName().equals("publicField"))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, String>create()
			.field(field)
			.build();

		var obj = new TestClass();
		prop.set(obj, "value1");
		assertEquals("value1", prop.get(obj));
		prop.set(obj, "value2");
		assertEquals("value2", prop.get(obj));
		prop.set(obj, null);
		assertNull(prop.get(obj));
	}

	//====================================================================================================
	// Property.Builder.getter/setter(MethodInfo) - complete round trip
	//====================================================================================================
	@Test
	void a026_methodInfo_roundTrip() throws Exception {
		var getter = ClassInfo.of(TestClass.class).getPublicMethods().stream()
			.filter(m -> m.hasName("getPublicField"))
			.findFirst()
			.orElseThrow();
		var setter = ClassInfo.of(TestClass.class).getPublicMethods().stream()
			.filter(m -> m.hasName("setPublicField") && m.hasParameterTypes(String.class))
			.findFirst()
			.orElseThrow();
		var prop = Property.<TestClass, String>create()
			.getter(getter)
			.setter(setter)
			.build();

		var obj = new TestClass();
		prop.set(obj, "value1");
		assertEquals("value1", prop.get(obj));
		prop.set(obj, "value2");
		assertEquals("value2", prop.get(obj));
		prop.set(obj, null);
		assertNull(prop.get(obj));
	}
}

