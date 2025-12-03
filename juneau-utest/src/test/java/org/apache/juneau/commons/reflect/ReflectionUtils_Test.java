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

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ReflectionUtils_Test extends TestBase {

	public static class TestClass {
		public String field;
		public TestClass() {}
		public TestClass(String value) {}
		public void method() {}
		public void method(String param) {}
	}

	//====================================================================================================
	// Constructor
	//====================================================================================================
	@Test
	void a000_constructor() {
		// Instantiate the class to cover the implicit constructor
		new ReflectionUtils();
	}

	//====================================================================================================
	// info(Class<?>)
	//====================================================================================================
	@Test
	void a001_info_class() {
		// Non-null class
		ClassInfo ci = ReflectionUtils.info(TestClass.class);
		assertNotNull(ci);
		assertEquals(TestClass.class, ci.inner());
		
		// Null class - ClassInfo.of(null) throws IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.info((Class<?>)null));
	}

	//====================================================================================================
	// info(Constructor<?>)
	//====================================================================================================
	@Test
	void a002_info_constructor() throws Exception {
		// Non-null constructor
		Constructor<?> ctor = TestClass.class.getConstructor();
		ConstructorInfo ci = ReflectionUtils.info(ctor);
		assertNotNull(ci);
		assertEquals(ctor, ci.inner());
		
		// Null constructor - ConstructorInfo.of(null) throws IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.info((Constructor<?>)null));
	}

	//====================================================================================================
	// info(Field)
	//====================================================================================================
	@Test
	void a003_info_field() throws Exception {
		// Non-null field
		Field field = TestClass.class.getField("field");
		FieldInfo fi = ReflectionUtils.info(field);
		assertNotNull(fi);
		assertEquals(field, fi.inner());
		
		// Null field - FieldInfo.of(null) throws IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.info((Field)null));
	}

	//====================================================================================================
	// info(Method)
	//====================================================================================================
	@Test
	void a004_info_method() throws Exception {
		// Non-null method
		Method method = TestClass.class.getMethod("method");
		MethodInfo mi = ReflectionUtils.info(method);
		assertNotNull(mi);
		assertEquals(method, mi.inner());
		
		// Null method - MethodInfo.of(null) throws IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.info((Method)null));
	}

	//====================================================================================================
	// info(Object)
	//====================================================================================================
	@Test
	void a005_info_object() {
		// Non-null object
		TestClass obj = new TestClass();
		ClassInfo ci = ReflectionUtils.info(obj);
		assertNotNull(ci);
		assertEquals(TestClass.class, ci.inner());
		
		// Null object - ClassInfo.of(null) calls object.getClass() which throws NPE
		assertThrows(NullPointerException.class, () -> ReflectionUtils.info((Object)null));
		
		// Class object
		ClassInfo ciClass = ReflectionUtils.info(TestClass.class);
		assertNotNull(ciClass);
		assertEquals(TestClass.class, ciClass.inner());
	}
}

