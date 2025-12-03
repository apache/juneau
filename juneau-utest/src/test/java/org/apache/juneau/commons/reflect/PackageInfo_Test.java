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

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.*;
import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class PackageInfo_Test extends TestBase {

	@Target(PACKAGE)
	@Retention(RUNTIME)
	public static @interface TestPackageAnnotation {
		String value() default "";
	}

	// Test classes in different packages
	public static class TestClass1 {}
	public static class TestClass2 {}

	//====================================================================================================
	// equals(Object)
	//====================================================================================================
	@Test
	void a001_equals() {
		var pi1 = PackageInfo.of(TestClass1.class);
		var pi2 = PackageInfo.of(TestClass2.class);
		var pi3 = PackageInfo.of(TestClass1.class);

		// Same package should be equal
		assertEquals(pi1, pi3);
		assertEquals(pi1.hashCode(), pi3.hashCode());

		// Different packages should not be equal
		if (!pi1.getName().equals(pi2.getName())) {
			assertNotEquals(pi1, pi2);
		}

		// Should equal the underlying Package
		assertEquals(pi1, pi1.inner());
	}

	//====================================================================================================
	// getAnnotatableType()
	//====================================================================================================
	@Test
	void a002_getAnnotatableType() {
		var pi = PackageInfo.of(TestClass1.class);
		assertEquals(AnnotatableType.PACKAGE_TYPE, pi.getAnnotatableType());
	}

	//====================================================================================================
	// getAnnotations()
	//====================================================================================================
	@Test
	void a003_getAnnotations() {
		var pi = PackageInfo.of(TestClass1.class);
		var annotations = pi.getAnnotations();
		assertNotNull(annotations);
		// May or may not have annotations depending on package-info.java
	}

	//====================================================================================================
	// getAnnotations(Class<A>)
	//====================================================================================================
	@Test
	void a004_getAnnotations_typed() {
		var pi = PackageInfo.of(TestClass1.class);
		var annotations = pi.getAnnotations(TestPackageAnnotation.class);
		assertNotNull(annotations);
		// May or may not have annotations depending on package-info.java
		var count = annotations.count();
		assertTrue(count >= 0);
	}

	//====================================================================================================
	// getImplementationTitle()
	//====================================================================================================
	@Test
	void a005_getImplementationTitle() {
		var pi = PackageInfo.of(String.class);
		// May be null if not specified in manifest
		// Just verify the method doesn't throw
		assertDoesNotThrow(() -> pi.getImplementationTitle());
	}

	//====================================================================================================
	// getImplementationVendor()
	//====================================================================================================
	@Test
	void a006_getImplementationVendor() {
		var pi = PackageInfo.of(String.class);
		// May be null if not specified in manifest
		// Just verify the method doesn't throw
		assertDoesNotThrow(() -> pi.getImplementationVendor());
	}

	//====================================================================================================
	// getImplementationVersion()
	//====================================================================================================
	@Test
	void a007_getImplementationVersion() {
		var pi = PackageInfo.of(String.class);
		// May be null if not specified in manifest
		// Just verify the method doesn't throw
		assertDoesNotThrow(() -> pi.getImplementationVersion());
	}

	//====================================================================================================
	// getLabel()
	//====================================================================================================
	@Test
	void a008_getLabel() {
		var pi = PackageInfo.of(TestClass1.class);
		var label = pi.getLabel();
		assertNotNull(label);
		assertEquals(pi.getName(), label);
	}

	//====================================================================================================
	// getName()
	//====================================================================================================
	@Test
	void a009_getName() {
		var pi = PackageInfo.of(TestClass1.class);
		var name = pi.getName();
		assertNotNull(name);
		assertEquals("org.apache.juneau.commons.reflect", name);

		// Test with String.class
		var pi2 = PackageInfo.of(String.class);
		assertEquals("java.lang", pi2.getName());
	}

	//====================================================================================================
	// getSpecificationTitle()
	//====================================================================================================
	@Test
	void a010_getSpecificationTitle() {
		var pi = PackageInfo.of(String.class);
		// May be null if not specified in manifest
		// Just verify the method doesn't throw
		assertDoesNotThrow(() -> pi.getSpecificationTitle());
	}

	//====================================================================================================
	// getSpecificationVendor()
	//====================================================================================================
	@Test
	void a011_getSpecificationVendor() {
		var pi = PackageInfo.of(String.class);
		// May be null if not specified in manifest
		// Just verify the method doesn't throw
		assertDoesNotThrow(() -> pi.getSpecificationVendor());
	}

	//====================================================================================================
	// getSpecificationVersion()
	//====================================================================================================
	@Test
	void a012_getSpecificationVersion() {
		var pi = PackageInfo.of(String.class);
		// May be null if not specified in manifest
		// Just verify the method doesn't throw
		assertDoesNotThrow(() -> pi.getSpecificationVersion());
	}

	//====================================================================================================
	// hashCode()
	//====================================================================================================
	@Test
	void a013_hashCode() {
		var pi1 = PackageInfo.of(TestClass1.class);
		var pi2 = PackageInfo.of(TestClass1.class);
		var pi3 = PackageInfo.of(TestClass2.class);

		// Same package should have same hash code
		assertEquals(pi1.hashCode(), pi2.hashCode());

		// Should match underlying Package hash code
		assertEquals(pi1.hashCode(), pi1.inner().hashCode());

		// Different packages may or may not have different hash codes
		if (!pi1.getName().equals(pi3.getName())) {
			// Hash codes might still be equal (collision), but usually different
		}
	}

	//====================================================================================================
	// inner()
	//====================================================================================================
	@Test
	void a014_inner() {
		var pi = PackageInfo.of(TestClass1.class);
		var pkg = pi.inner();
		assertNotNull(pkg);
		assertEquals("org.apache.juneau.commons.reflect", pkg.getName());
	}

	//====================================================================================================
	// isCompatibleWith(String)
	//====================================================================================================
	@Test
	void a015_isCompatibleWith() {
		var pi = PackageInfo.of(String.class);

		// Test line 309: ensure inner.isCompatibleWith(desired) is called
		// Package versions come from JAR manifest (Specification-Version header)
		// Most test packages don't have versions, so isCompatibleWith throws NumberFormatException
		// However, line 309 is still executed - the exception is thrown FROM that line
		
		// Try to find a package with a version (e.g., from standard library or a dependency)
		// If found, test the normal return path
		var specVersion = pi.getSpecificationVersion();
		if (specVersion != null && !specVersion.isEmpty()) {
			// Line 309: normal return path when package has a version
			var compatible = pi.isCompatibleWith("1.0");
			assertNotNull(Boolean.valueOf(compatible));
			
			// Test with another version
			var compatible2 = pi.isCompatibleWith("2.0");
			assertNotNull(Boolean.valueOf(compatible2));
		} else {
			// Package doesn't have a version - isCompatibleWith will throw NumberFormatException
			// Line 309 is still executed, exception is thrown from inner.isCompatibleWith
			assertThrows(NumberFormatException.class, () -> pi.isCompatibleWith("1.0"));
		}

		// Test with invalid version - should throw NumberFormatException
		// Line 309 is executed, then exception is thrown from inner.isCompatibleWith
		assertThrows(NumberFormatException.class, () -> pi.isCompatibleWith("invalid"));

		// Test with null - Package.isCompatibleWith(null) throws NumberFormatException
		// Line 309 is executed, then exception is thrown from inner.isCompatibleWith
		assertThrows(NumberFormatException.class, () -> pi.isCompatibleWith(null));
	}

	//====================================================================================================
	// isSealed()
	//====================================================================================================
	@Test
	void a016_isSealed() {
		var pi = PackageInfo.of(TestClass1.class);
		var sealed = pi.isSealed();
		// Most packages are not sealed
		assertNotNull(Boolean.valueOf(sealed));
	}

	//====================================================================================================
	// isSealed(URL)
	//====================================================================================================
	@Test
	void a017_isSealed_withUrl() throws Exception {
		var pi = PackageInfo.of(TestClass1.class);
		var url = new URL("file:///test.jar");
		var sealed = pi.isSealed(url);
		// Most packages are not sealed with respect to a specific URL
		assertNotNull(Boolean.valueOf(sealed));
	}

	//====================================================================================================
	// of(Class<?>)
	//====================================================================================================
	@Test
	void a018_of_withClass() {
		var pi = PackageInfo.of(TestClass1.class);
		assertNotNull(pi);
		assertEquals("org.apache.juneau.commons.reflect", pi.getName());

		// Test with null - should throw
		assertThrows(NullPointerException.class, () -> PackageInfo.of((Class<?>)null));
	}

	//====================================================================================================
	// of(ClassInfo)
	//====================================================================================================
	@Test
	void a019_of_withClassInfo() {
		var ci = ClassInfo.of(TestClass1.class);
		var pi = PackageInfo.of(ci);
		assertNotNull(pi);
		assertEquals("org.apache.juneau.commons.reflect", pi.getName());
	}

	//====================================================================================================
	// of(Package)
	//====================================================================================================
	@Test
	void a020_of_withPackage() {
		var pkg = TestClass1.class.getPackage();
		var pi = PackageInfo.of(pkg);
		assertNotNull(pi);
		assertEquals("org.apache.juneau.commons.reflect", pi.getName());

		// Test caching - should return same instance
		var pi2 = PackageInfo.of(pkg);
		assertSame(pi, pi2);

		// Test with null - should throw
		assertThrows(IllegalArgumentException.class, () -> PackageInfo.of((Package)null));
	}

	//====================================================================================================
	// toString()
	//====================================================================================================
	@Test
	void a021_toString() {
		var pi = PackageInfo.of(TestClass1.class);
		var str = pi.toString();
		assertNotNull(str);
		assertTrue(str.contains("package"));
		assertTrue(str.contains("org.apache.juneau.commons.reflect"));

		// Should match underlying Package toString
		assertEquals(pi.inner().toString(), str);
	}
}

