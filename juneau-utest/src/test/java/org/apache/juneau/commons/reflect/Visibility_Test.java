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

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

class Visibility_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test classes with various visibility modifiers
	//-----------------------------------------------------------------------------------------------------------------

	public static class PublicClass {
		public PublicClass() {}
		public void publicMethod() {}
		public int publicField;
	}

	private static class PrivateClass {
		private PrivateClass() {}
		@SuppressWarnings("unused")
		private void privateMethod() {}
		@SuppressWarnings("unused")
		private int privateField;
	}

	protected static class ProtectedClass {
		protected ProtectedClass() {}
		protected void protectedMethod() {}
		protected int protectedField;
	}

	static class PackagePrivateClass {
		PackagePrivateClass() {}
		void packagePrivateMethod() {}
		int packagePrivateField;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Constructor<?> getConstructor(Class<?> c, Class<?>... paramTypes) {
		try {
			return c.getDeclaredConstructor(paramTypes);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static Method getMethod(Class<?> c, String name, Class<?>... paramTypes) {
		try {
			return c.getDeclaredMethod(name, paramTypes);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private static Field getField(Class<?> c, String name) {
		try {
			return c.getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	//====================================================================================================
	// transform(Constructor<T>)
	//====================================================================================================
	@Test
	void a001_transform_constructor_null() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> Visibility.PUBLIC.transform((Constructor<?>)null));
		assertThrows(IllegalArgumentException.class, () -> Visibility.PRIVATE.transform((Constructor<?>)null));
		assertThrows(IllegalArgumentException.class, () -> Visibility.NONE.transform((Constructor<?>)null));
	}

	@Test
	void a002_transform_constructor_public() throws Exception {
		Constructor<?> publicCtor = getConstructor(PublicClass.class);
		Constructor<?> privateCtor = getConstructor(PrivateClass.class);
		Constructor<?> protectedCtor = getConstructor(ProtectedClass.class);
		Constructor<?> packageCtor = getConstructor(PackagePrivateClass.class);

		// PUBLIC visibility - only makes public accessible, returns others as-is
		assertNotNull(Visibility.PUBLIC.transform(publicCtor));
		assertSame(privateCtor, Visibility.PUBLIC.transform(privateCtor));
		assertSame(protectedCtor, Visibility.PUBLIC.transform(protectedCtor));
		assertSame(packageCtor, Visibility.PUBLIC.transform(packageCtor));

		// PROTECTED visibility - makes public and protected accessible
		assertNotNull(Visibility.PROTECTED.transform(publicCtor));
		assertSame(privateCtor, Visibility.PROTECTED.transform(privateCtor));
		assertNotNull(Visibility.PROTECTED.transform(protectedCtor));
		assertSame(packageCtor, Visibility.PROTECTED.transform(packageCtor));

		// DEFAULT visibility - makes public, protected, and package accessible
		assertNotNull(Visibility.DEFAULT.transform(publicCtor));
		assertSame(privateCtor, Visibility.DEFAULT.transform(privateCtor));
		assertNotNull(Visibility.DEFAULT.transform(protectedCtor));
		assertNotNull(Visibility.DEFAULT.transform(packageCtor));

		// PRIVATE visibility - makes all accessible
		assertNotNull(Visibility.PRIVATE.transform(publicCtor));
		assertNotNull(Visibility.PRIVATE.transform(privateCtor));
		assertNotNull(Visibility.PRIVATE.transform(protectedCtor));
		assertNotNull(Visibility.PRIVATE.transform(packageCtor));

		// NONE visibility - doesn't make anything accessible, but returns as-is
		assertSame(publicCtor, Visibility.NONE.transform(publicCtor));
		assertSame(privateCtor, Visibility.NONE.transform(privateCtor));
		assertSame(protectedCtor, Visibility.NONE.transform(protectedCtor));
		assertSame(packageCtor, Visibility.NONE.transform(packageCtor));
	}

	@Test
	void a003_transform_constructor_returnsSameInstance() throws Exception {
		Constructor<?> publicCtor = getConstructor(PublicClass.class);
		Constructor<?> result = Visibility.PUBLIC.transform(publicCtor);
		assertSame(publicCtor, result);
	}

	//====================================================================================================
	// transform(Field)
	//====================================================================================================
	@Test
	void a004_transform_field_null() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> Visibility.PUBLIC.transform((Field)null));
		assertThrows(IllegalArgumentException.class, () -> Visibility.PRIVATE.transform((Field)null));
		assertThrows(IllegalArgumentException.class, () -> Visibility.NONE.transform((Field)null));
	}

	@Test
	void a005_transform_field_public() throws Exception {
		Field publicField = getField(PublicClass.class, "publicField");
		Field privateField = getField(PrivateClass.class, "privateField");
		Field protectedField = getField(ProtectedClass.class, "protectedField");
		Field packageField = getField(PackagePrivateClass.class, "packagePrivateField");

		// PUBLIC visibility - only makes public accessible, returns others as-is
		assertNotNull(Visibility.PUBLIC.transform(publicField));
		assertSame(privateField, Visibility.PUBLIC.transform(privateField));
		assertSame(protectedField, Visibility.PUBLIC.transform(protectedField));
		assertSame(packageField, Visibility.PUBLIC.transform(packageField));

		// PROTECTED visibility - makes public and protected accessible
		assertNotNull(Visibility.PROTECTED.transform(publicField));
		assertSame(privateField, Visibility.PROTECTED.transform(privateField));
		assertNotNull(Visibility.PROTECTED.transform(protectedField));
		assertSame(packageField, Visibility.PROTECTED.transform(packageField));

		// DEFAULT visibility - makes public, protected, and package accessible
		assertNotNull(Visibility.DEFAULT.transform(publicField));
		assertSame(privateField, Visibility.DEFAULT.transform(privateField));
		assertNotNull(Visibility.DEFAULT.transform(protectedField));
		assertNotNull(Visibility.DEFAULT.transform(packageField));

		// PRIVATE visibility - makes all accessible
		assertNotNull(Visibility.PRIVATE.transform(publicField));
		assertNotNull(Visibility.PRIVATE.transform(privateField));
		assertNotNull(Visibility.PRIVATE.transform(protectedField));
		assertNotNull(Visibility.PRIVATE.transform(packageField));

		// NONE visibility - doesn't make anything accessible, but returns as-is
		assertSame(publicField, Visibility.NONE.transform(publicField));
		assertSame(privateField, Visibility.NONE.transform(privateField));
		assertSame(protectedField, Visibility.NONE.transform(protectedField));
		assertSame(packageField, Visibility.NONE.transform(packageField));
	}

	@Test
	void a006_transform_field_returnsSameInstance() throws Exception {
		Field publicField = getField(PublicClass.class, "publicField");
		Field result = Visibility.PUBLIC.transform(publicField);
		assertSame(publicField, result);
	}

	//====================================================================================================
	// transform(Method)
	//====================================================================================================
	@Test
	void a007_transform_method_null() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> Visibility.PUBLIC.transform((Method)null));
		assertThrows(IllegalArgumentException.class, () -> Visibility.PRIVATE.transform((Method)null));
		assertThrows(IllegalArgumentException.class, () -> Visibility.NONE.transform((Method)null));
	}

	@Test
	void a008_transform_method_public() throws Exception {
		Method publicMethod = getMethod(PublicClass.class, "publicMethod");
		Method privateMethod = getMethod(PrivateClass.class, "privateMethod");
		Method protectedMethod = getMethod(ProtectedClass.class, "protectedMethod");
		Method packageMethod = getMethod(PackagePrivateClass.class, "packagePrivateMethod");

		// PUBLIC visibility - only makes public accessible, returns others as-is
		assertNotNull(Visibility.PUBLIC.transform(publicMethod));
		assertSame(privateMethod, Visibility.PUBLIC.transform(privateMethod));
		assertSame(protectedMethod, Visibility.PUBLIC.transform(protectedMethod));
		assertSame(packageMethod, Visibility.PUBLIC.transform(packageMethod));

		// PROTECTED visibility - makes public and protected accessible
		assertNotNull(Visibility.PROTECTED.transform(publicMethod));
		assertSame(privateMethod, Visibility.PROTECTED.transform(privateMethod));
		assertNotNull(Visibility.PROTECTED.transform(protectedMethod));
		assertSame(packageMethod, Visibility.PROTECTED.transform(packageMethod));

		// DEFAULT visibility - makes public, protected, and package accessible
		assertNotNull(Visibility.DEFAULT.transform(publicMethod));
		assertSame(privateMethod, Visibility.DEFAULT.transform(privateMethod));
		assertNotNull(Visibility.DEFAULT.transform(protectedMethod));
		assertNotNull(Visibility.DEFAULT.transform(packageMethod));

		// PRIVATE visibility - makes all accessible
		assertNotNull(Visibility.PRIVATE.transform(publicMethod));
		assertNotNull(Visibility.PRIVATE.transform(privateMethod));
		assertNotNull(Visibility.PRIVATE.transform(protectedMethod));
		assertNotNull(Visibility.PRIVATE.transform(packageMethod));

		// NONE visibility - doesn't make anything accessible, but returns as-is
		assertSame(publicMethod, Visibility.NONE.transform(publicMethod));
		assertSame(privateMethod, Visibility.NONE.transform(privateMethod));
		assertSame(protectedMethod, Visibility.NONE.transform(protectedMethod));
		assertSame(packageMethod, Visibility.NONE.transform(packageMethod));
	}

	@Test
	void a009_transform_method_returnsSameInstance() throws Exception {
		Method publicMethod = getMethod(PublicClass.class, "publicMethod");
		Method result = Visibility.PUBLIC.transform(publicMethod);
		assertSame(publicMethod, result);
	}
}

