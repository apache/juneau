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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage tests for {@link ReflectionMap}, filling gaps not otherwise exercised
 * incidentally by {@code ReflectionMap_Test} - specifically the content-based
 * {@code equals()}/{@code hashCode()} overrides on the private {@code ConstructorEntry} and
 * {@code MethodEntry} record types, which are never invoked by {@link ReflectionMap#find} itself.
 */
class ReflectionMap_Coverage_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a: ConstructorEntry.equals() / hashCode()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_constructorEntryEquals_allFieldsSame_true() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		var b = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void a02_constructorEntryEquals_differentSimpleClassName_false() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		var b = new ReflectionMap.ConstructorEntry<>("Bar", "com.foo.Foo", new String[] {"String"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void a03_constructorEntryEquals_differentFullClassName_false() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		var b = new ReflectionMap.ConstructorEntry<>("Foo", "com.bar.Foo", new String[] {"String"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void a04_constructorEntryEquals_differentArgs_false() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		var b = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"int"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void a05_constructorEntryEquals_differentValue_false() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		var b = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 2);
		assertNotEquals(a, b);
	}

	@Test
	void a06_constructorEntryEquals_notAConstructorEntry_false() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		assertNotEquals("not a ConstructorEntry", a);
	}

	@Test
	void a07_constructorEntryEquals_null_false() {
		var a = new ReflectionMap.ConstructorEntry<>("Foo", "com.foo.Foo", new String[] {"String"}, 1);
		assertNotEquals(null, a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b: MethodEntry.equals() / hashCode()
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_methodEntryEquals_allFieldsSame_true() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		var b = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void b02_methodEntryEquals_differentSimpleClassName_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		var b = new ReflectionMap.MethodEntry<>("Bar", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void b03_methodEntryEquals_differentFullClassName_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		var b = new ReflectionMap.MethodEntry<>("Foo", "com.bar.Foo", "myMethod", new String[] {"String"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void b04_methodEntryEquals_differentMethodName_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		var b = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "otherMethod", new String[] {"String"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void b05_methodEntryEquals_differentArgs_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		var b = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"int"}, 1);
		assertNotEquals(a, b);
	}

	@Test
	void b06_methodEntryEquals_differentValue_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		var b = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 2);
		assertNotEquals(a, b);
	}

	@Test
	void b07_methodEntryEquals_notAMethodEntry_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		assertNotEquals("not a MethodEntry", a);
	}

	@Test
	void b08_methodEntryEquals_null_false() {
		var a = new ReflectionMap.MethodEntry<>("Foo", "com.foo.Foo", "myMethod", new String[] {"String"}, 1);
		assertNotEquals(null, a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c: classMatches() - inner-class "$"-splitting fallback, reached only when the pattern doesn't already
	// match the target's simple/full name at the top-level check (so all-nested-test-fixture patterns used by
	// ReflectionMap_Test's positive-match tests short-circuit before ever reaching this block).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_classMatches_nonInnerClass_noDollarSign_fallsThroughToFalse() {
		// Pattern matches neither simpleName, fullName, nor the wildcard, and the target class has no '$' in its
		// name, so classMatches() must fall through the "$"-splitting block entirely (never enters it) and return false.
		var rm = ReflectionMap.create(Number.class).append("NotAMatch", 1).build();
		assertFalse(rm.find(String.class).findAny().isPresent());
	}

	@Test
	void c02_classMatches_innerClassInDefaultPackage_nullPackage_fallsThroughToFalse() throws ClassNotFoundException {
		// Target class has '$' in its name but lives in the default (unnamed) package, so getPackage() is null -
		// classMatches() must skip the package-prefix strip and fall through the while loop without matching.
		var c = Class.forName("DefaultPackageTestClass$InnerClass");
		var rm = ReflectionMap.create(Number.class).append("NotAMatch", 1).build();
		assertFalse(rm.find(c).findAny().isPresent());
	}
}
