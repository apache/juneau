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

import static org.apache.juneau.commons.reflect.ElementFlag.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ElementInfo_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test classes with various modifiers
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

	public static class StaticClass {
		public static void staticMethod() {}
		public static int staticField;
	}

	public static class FinalClass {
		public final void finalMethod() {}
		public final int finalField = 0;
	}

	public abstract static class AbstractClass {
		public abstract void abstractMethod();
	}

	public interface InterfaceClass {
		void interfaceMethod();
	}

	public static class SynchronizedClass {
		public synchronized void synchronizedMethod() {}
	}

	public static class NativeClass {
		public native void nativeMethod();
	}

	public static class StrictClass {
		public strictfp void strictMethod() {}
	}

	public static class TransientClass {
		public transient int transientField;
	}

	public static class VolatileClass {
		public volatile int volatileField;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static ClassInfo getClassInfo(Class<?> c) {
		return ClassInfo.of(c);
	}

	private static MethodInfo getMethod(Class<?> c, String name) {
		return ClassInfo.of(c).getPublicMethod(m -> m.hasName(name)).orElse(
			ClassInfo.of(c).getMethod(m -> m.hasName(name)).orElse(null)
		);
	}

	@SuppressWarnings("unused")
	private static ConstructorInfo getConstructor(Class<?> c) {
		return ClassInfo.of(c).getPublicConstructor(cons -> cons.getParameterCount() == 0).orElse(
			ClassInfo.of(c).getDeclaredConstructor(cons -> cons.getParameterCount() == 0).orElse(null)
		);
	}

	private static FieldInfo getField(Class<?> c, String name) {
		return ClassInfo.of(c).getPublicField(f -> f.hasName(name)).orElse(
			ClassInfo.of(c).getDeclaredField(f -> f.hasName(name)).orElse(null)
		);
	}

	//====================================================================================================
	// getModifiers()
	//====================================================================================================
	@Test
	void a001_getModifiers() {
		var publicClass = getClassInfo(PublicClass.class);
		var modifiers = publicClass.getModifiers();
		assertTrue(Modifier.isPublic(modifiers));
		assertFalse(Modifier.isPrivate(modifiers));
		assertFalse(Modifier.isProtected(modifiers));
		// PublicClass is a static inner class, so it IS static
		assertTrue(Modifier.isStatic(modifiers));
		assertFalse(Modifier.isFinal(modifiers));
		assertFalse(Modifier.isAbstract(modifiers));
		assertFalse(Modifier.isInterface(modifiers));
	}

	//====================================================================================================
	// is(ElementFlag)
	//====================================================================================================
	@Test
	void a002_is() {
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");
		var publicField = getField(PublicClass.class, "publicField");
		var privateClass = getClassInfo(PrivateClass.class);
		var staticMethod = getMethod(StaticClass.class, "staticMethod");
		var finalMethod = getMethod(FinalClass.class, "finalMethod");
		var abstractClass = getClassInfo(AbstractClass.class);
		var interfaceClass = getClassInfo(InterfaceClass.class);
		var synchronizedMethod = getMethod(SynchronizedClass.class, "synchronizedMethod");
		var nativeMethod = getMethod(NativeClass.class, "nativeMethod");
		var transientField = getField(TransientClass.class, "transientField");
		var volatileField = getField(VolatileClass.class, "volatileField");

		// Public
		assertTrue(publicClass.is(PUBLIC));
		assertTrue(publicMethod.is(PUBLIC));
		assertTrue(publicField.is(PUBLIC));
		assertFalse(publicClass.is(NOT_PUBLIC));
		assertFalse(publicMethod.is(NOT_PUBLIC));
		assertFalse(publicField.is(NOT_PUBLIC));

		// Private
		assertTrue(privateClass.is(PRIVATE));
		assertFalse(publicClass.is(PRIVATE));
		assertTrue(publicClass.is(NOT_PRIVATE));
		assertFalse(privateClass.is(NOT_PRIVATE));

		// Protected
		var protectedClass = getClassInfo(ProtectedClass.class);
		assertTrue(protectedClass.is(PROTECTED));
		assertFalse(publicClass.is(PROTECTED));
		assertTrue(publicClass.is(NOT_PROTECTED));
		assertFalse(protectedClass.is(NOT_PROTECTED));

		// Static
		assertTrue(staticMethod.is(STATIC));
		assertFalse(publicMethod.is(STATIC));
		assertTrue(publicMethod.is(NOT_STATIC));
		assertFalse(staticMethod.is(NOT_STATIC));

		// Final
		assertTrue(finalMethod.is(FINAL));
		assertFalse(publicMethod.is(FINAL));
		assertTrue(publicMethod.is(NOT_FINAL));
		assertFalse(finalMethod.is(NOT_FINAL));

		// Abstract
		assertTrue(abstractClass.is(ABSTRACT));
		assertFalse(publicClass.is(ABSTRACT));
		assertTrue(publicClass.is(NOT_ABSTRACT));
		assertFalse(abstractClass.is(NOT_ABSTRACT));

		// Interface
		assertTrue(interfaceClass.is(INTERFACE));
		assertFalse(publicClass.is(INTERFACE));

		// Synchronized
		assertTrue(synchronizedMethod.is(SYNCHRONIZED));
		assertFalse(publicMethod.is(SYNCHRONIZED));
		assertTrue(publicMethod.is(NOT_SYNCHRONIZED));
		assertFalse(synchronizedMethod.is(NOT_SYNCHRONIZED));

		// Native
		assertTrue(nativeMethod.is(NATIVE));
		assertFalse(publicMethod.is(NATIVE));
		assertTrue(publicMethod.is(NOT_NATIVE));
		assertFalse(nativeMethod.is(NOT_NATIVE));

		// Transient
		assertTrue(transientField.is(TRANSIENT));
		assertFalse(publicField.is(TRANSIENT));
		assertTrue(publicField.is(NOT_TRANSIENT));
		assertFalse(transientField.is(NOT_TRANSIENT));

		// Volatile
		assertTrue(volatileField.is(VOLATILE));
		assertFalse(publicField.is(VOLATILE));
		assertTrue(publicField.is(NOT_VOLATILE));
		assertFalse(volatileField.is(NOT_VOLATILE));
	}

	//====================================================================================================
	// isAbstract()
	//====================================================================================================
	@Test
	void a003_isAbstract() {
		var abstractClass = getClassInfo(AbstractClass.class);
		var abstractMethod = getMethod(AbstractClass.class, "abstractMethod");
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertTrue(abstractClass.isAbstract());
		assertTrue(abstractMethod.isAbstract());
		assertFalse(publicClass.isAbstract());
		assertFalse(publicMethod.isAbstract());
	}

	//====================================================================================================
	// isAll(ElementFlag...)
	//====================================================================================================
	@Test
	void a004_isAll() {
		var publicClass = getClassInfo(PublicClass.class);
		var staticMethod = getMethod(StaticClass.class, "staticMethod");
		var finalMethod = getMethod(FinalClass.class, "finalMethod");

		// All flags match
		assertTrue(publicClass.isAll(PUBLIC, NOT_PRIVATE, NOT_PROTECTED));
		assertTrue(staticMethod.isAll(PUBLIC, STATIC));

		// Not all flags match
		assertFalse(publicClass.isAll(PUBLIC, PRIVATE));
		assertFalse(staticMethod.isAll(PUBLIC, STATIC, FINAL));
		assertFalse(finalMethod.isAll(PUBLIC, STATIC));

		// Empty flags array - allMatch on empty stream returns true (vacuous truth) (line 143)
		assertTrue(publicClass.isAll());
		assertTrue(staticMethod.isAll());
		
		// Single flag - ensures stream(flags).allMatch(this::is) is executed (line 143)
		assertTrue(publicClass.isAll(PUBLIC));
		assertFalse(publicClass.isAll(PRIVATE));
		assertTrue(staticMethod.isAll(STATIC));
		assertFalse(staticMethod.isAll(FINAL));
		
		// Multiple flags where first doesn't match (short-circuit) - line 143
		// allMatch short-circuits on first false, so this tests the short-circuit branch
		assertFalse(publicClass.isAll(PRIVATE, PUBLIC, NOT_PRIVATE));
		
		// Multiple flags where all match (full iteration) - line 143
		// allMatch iterates through all elements when all are true
		assertTrue(publicClass.isAll(PUBLIC, NOT_PRIVATE, NOT_PROTECTED, NOT_FINAL));
	}

	//====================================================================================================
	// isAny(ElementFlag...)
	//====================================================================================================
	@Test
	void a005_isAny() {
		var publicClass = getClassInfo(PublicClass.class);
		var staticMethod = getMethod(StaticClass.class, "staticMethod");
		var finalMethod = getMethod(FinalClass.class, "finalMethod");

		// At least one flag matches
		assertTrue(publicClass.isAny(PUBLIC, PRIVATE));
		assertTrue(staticMethod.isAny(PUBLIC, STATIC, FINAL));
		assertTrue(finalMethod.isAny(PUBLIC, STATIC, FINAL));

		// No flags match
		assertFalse(publicClass.isAny(PRIVATE, PROTECTED));
		assertFalse(staticMethod.isAny(PRIVATE, FINAL));

		// Empty flags array - anyMatch on empty stream returns false (line 157)
		assertFalse(publicClass.isAny(new ElementFlag[0]));
		assertFalse(staticMethod.isAny(new ElementFlag[0]));
		
		// Single flag - ensures stream(flags).anyMatch(this::is) is executed (line 157)
		assertTrue(publicClass.isAny(PUBLIC));
		assertFalse(publicClass.isAny(PRIVATE));
		assertTrue(staticMethod.isAny(STATIC));
		assertFalse(staticMethod.isAny(FINAL));
		
		// Multiple flags where first matches (short-circuit) - line 157
		// anyMatch short-circuits on first true, so this tests the short-circuit branch
		assertTrue(publicClass.isAny(PUBLIC, PRIVATE, PROTECTED));
		
		// Multiple flags where none match (full iteration) - line 157
		// anyMatch iterates through all elements when all are false
		assertFalse(publicClass.isAny(PRIVATE, PROTECTED, FINAL, ABSTRACT));
	}

	//====================================================================================================
	// isFinal()
	//====================================================================================================
	@Test
	void a006_isFinal() {
		@SuppressWarnings("unused")
		var finalClass = getClassInfo(FinalClass.class);
		var finalMethod = getMethod(FinalClass.class, "finalMethod");
		var finalField = getField(FinalClass.class, "finalField");
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertTrue(finalMethod.isFinal());
		assertTrue(finalField.isFinal());
		assertFalse(publicClass.isFinal());
		assertFalse(publicMethod.isFinal());
	}

	//====================================================================================================
	// isInterface()
	//====================================================================================================
	@Test
	void a007_isInterface() {
		var interfaceClass = getClassInfo(InterfaceClass.class);
		var publicClass = getClassInfo(PublicClass.class);

		assertTrue(interfaceClass.isInterface());
		assertFalse(publicClass.isInterface());
	}

	//====================================================================================================
	// isNative()
	//====================================================================================================
	@Test
	void a008_isNative() {
		var nativeMethod = getMethod(NativeClass.class, "nativeMethod");
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertTrue(nativeMethod.isNative());
		assertFalse(publicMethod.isNative());
	}

	//====================================================================================================
	// isNotAbstract()
	//====================================================================================================
	@Test
	void a009_isNotAbstract() {
		var abstractClass = getClassInfo(AbstractClass.class);
		var abstractMethod = getMethod(AbstractClass.class, "abstractMethod");
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertFalse(abstractClass.isNotAbstract());
		assertFalse(abstractMethod.isNotAbstract());
		assertTrue(publicClass.isNotAbstract());
		assertTrue(publicMethod.isNotAbstract());
	}

	//====================================================================================================
	// isNotFinal()
	//====================================================================================================
	@Test
	void a010_isNotFinal() {
		var finalMethod = getMethod(FinalClass.class, "finalMethod");
		var finalField = getField(FinalClass.class, "finalField");
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertFalse(finalMethod.isNotFinal());
		assertFalse(finalField.isNotFinal());
		assertTrue(publicClass.isNotFinal());
		assertTrue(publicMethod.isNotFinal());
	}

	//====================================================================================================
	// isNotInterface()
	//====================================================================================================
	@Test
	void a011_isNotInterface() {
		var interfaceClass = getClassInfo(InterfaceClass.class);
		var publicClass = getClassInfo(PublicClass.class);

		assertFalse(interfaceClass.isNotInterface());
		assertTrue(publicClass.isNotInterface());
	}

	//====================================================================================================
	// isNotNative()
	//====================================================================================================
	@Test
	void a012_isNotNative() {
		var nativeMethod = getMethod(NativeClass.class, "nativeMethod");
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertFalse(nativeMethod.isNotNative());
		assertTrue(publicMethod.isNotNative());
	}

	//====================================================================================================
	// isNotPrivate()
	//====================================================================================================
	@Test
	void a013_isNotPrivate() {
		var privateClass = getClassInfo(PrivateClass.class);
		var publicClass = getClassInfo(PublicClass.class);
		var protectedClass = getClassInfo(ProtectedClass.class);

		assertFalse(privateClass.isNotPrivate());
		assertTrue(publicClass.isNotPrivate());
		assertTrue(protectedClass.isNotPrivate());
	}

	//====================================================================================================
	// isNotProtected()
	//====================================================================================================
	@Test
	void a014_isNotProtected() {
		var protectedClass = getClassInfo(ProtectedClass.class);
		var publicClass = getClassInfo(PublicClass.class);
		var privateClass = getClassInfo(PrivateClass.class);

		assertFalse(protectedClass.isNotProtected());
		assertTrue(publicClass.isNotProtected());
		assertTrue(privateClass.isNotProtected());
	}

	//====================================================================================================
	// isNotPublic()
	//====================================================================================================
	@Test
	void a015_isNotPublic() {
		var publicClass = getClassInfo(PublicClass.class);
		var privateClass = getClassInfo(PrivateClass.class);
		var protectedClass = getClassInfo(ProtectedClass.class);

		assertFalse(publicClass.isNotPublic());
		assertTrue(privateClass.isNotPublic());
		assertTrue(protectedClass.isNotPublic());
	}

	//====================================================================================================
	// isNotStatic()
	//====================================================================================================
	@Test
	void a016_isNotStatic() {
		var staticMethod = getMethod(StaticClass.class, "staticMethod");
		var publicMethod = getMethod(PublicClass.class, "publicMethod");
		var staticField = getField(StaticClass.class, "staticField");
		var publicField = getField(PublicClass.class, "publicField");

		assertFalse(staticMethod.isNotStatic());
		assertFalse(staticField.isNotStatic());
		assertTrue(publicMethod.isNotStatic());
		assertTrue(publicField.isNotStatic());
	}

	//====================================================================================================
	// isNotSynchronized()
	//====================================================================================================
	@Test
	void a018_isNotSynchronized() {
		var synchronizedMethod = getMethod(SynchronizedClass.class, "synchronizedMethod");
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertFalse(synchronizedMethod.isNotSynchronized());
		assertTrue(publicMethod.isNotSynchronized());
	}

	//====================================================================================================
	// isNotTransient()
	//====================================================================================================
	@Test
	void a019_isNotTransient() {
		var transientField = getField(TransientClass.class, "transientField");
		var publicField = getField(PublicClass.class, "publicField");

		assertFalse(transientField.isNotTransient());
		assertTrue(publicField.isNotTransient());
	}

	//====================================================================================================
	// isNotVolatile()
	//====================================================================================================
	@Test
	void a020_isNotVolatile() {
		var volatileField = getField(VolatileClass.class, "volatileField");
		var publicField = getField(PublicClass.class, "publicField");

		assertFalse(volatileField.isNotVolatile());
		assertTrue(publicField.isNotVolatile());
	}

	//====================================================================================================
	// isPrivate()
	//====================================================================================================
	@Test
	void a021_isPrivate() {
		var privateClass = getClassInfo(PrivateClass.class);
		var privateMethod = getMethod(PrivateClass.class, "privateMethod");
		var privateField = getField(PrivateClass.class, "privateField");
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertTrue(privateClass.isPrivate());
		assertTrue(privateMethod.isPrivate());
		assertTrue(privateField.isPrivate());
		assertFalse(publicClass.isPrivate());
		assertFalse(publicMethod.isPrivate());
	}

	//====================================================================================================
	// isProtected()
	//====================================================================================================
	@Test
	void a022_isProtected() {
		var protectedClass = getClassInfo(ProtectedClass.class);
		var protectedMethod = getMethod(ProtectedClass.class, "protectedMethod");
		var protectedField = getField(ProtectedClass.class, "protectedField");
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertTrue(protectedClass.isProtected());
		assertTrue(protectedMethod.isProtected());
		assertTrue(protectedField.isProtected());
		assertFalse(publicClass.isProtected());
		assertFalse(publicMethod.isProtected());
	}

	//====================================================================================================
	// isPublic()
	//====================================================================================================
	@Test
	void a023_isPublic() {
		var publicClass = getClassInfo(PublicClass.class);
		var publicMethod = getMethod(PublicClass.class, "publicMethod");
		var publicField = getField(PublicClass.class, "publicField");
		var privateClass = getClassInfo(PrivateClass.class);
		var protectedClass = getClassInfo(ProtectedClass.class);

		assertTrue(publicClass.isPublic());
		assertTrue(publicMethod.isPublic());
		assertTrue(publicField.isPublic());
		assertFalse(privateClass.isPublic());
		assertFalse(protectedClass.isPublic());
	}

	//====================================================================================================
	// isStatic()
	//====================================================================================================
	@Test
	void a024_isStatic() {
		var staticMethod = getMethod(StaticClass.class, "staticMethod");
		var staticField = getField(StaticClass.class, "staticField");
		var publicMethod = getMethod(PublicClass.class, "publicMethod");
		var publicField = getField(PublicClass.class, "publicField");

		assertTrue(staticMethod.isStatic());
		assertTrue(staticField.isStatic());
		assertFalse(publicMethod.isStatic());
		assertFalse(publicField.isStatic());
	}


	//====================================================================================================
	// isSynchronized()
	//====================================================================================================
	@Test
	void a026_isSynchronized() {
		var synchronizedMethod = getMethod(SynchronizedClass.class, "synchronizedMethod");
		var publicMethod = getMethod(PublicClass.class, "publicMethod");

		assertTrue(synchronizedMethod.isSynchronized());
		assertFalse(publicMethod.isSynchronized());
	}

	//====================================================================================================
	// isTransient()
	//====================================================================================================
	@Test
	void a027_isTransient() {
		var transientField = getField(TransientClass.class, "transientField");
		var publicField = getField(PublicClass.class, "publicField");

		assertTrue(transientField.isTransient());
		assertFalse(publicField.isTransient());
	}

	//====================================================================================================
	// isVolatile()
	//====================================================================================================
	@Test
	void a028_isVolatile() {
		var volatileField = getField(VolatileClass.class, "volatileField");
		var publicField = getField(PublicClass.class, "publicField");

		assertTrue(volatileField.isVolatile());
		assertFalse(publicField.isVolatile());
	}
}

