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
package org.apache.juneau.http.classic.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RrpcInterfaceMethodMeta}.
 */
class RrpcInterfaceMethodMeta_Test extends TestBase {

	public interface Iface {
		String get();
		String getWithArg(String s);
	}

	private static java.lang.reflect.Method getMethod(String name) throws Exception {
		for (var m : Iface.class.getMethods())
			if (m.getName().equals(name))
				return m;
		throw new AssertionError("Method not found: " + name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// getJavaMethod() / getPath() / getUri()
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_getJavaMethod() throws Exception {
		var m = getMethod("get");
		var meta = new RrpcInterfaceMethodMeta("http://x", m);
		assertSame(m, meta.getJavaMethod());
	}

	@Test void a02_getPath_noArgs() throws Exception {
		var meta = new RrpcInterfaceMethodMeta("http://x", getMethod("get"));
		assertEquals("get/", meta.getPath());
	}

	@Test void a03_getPath_withArgs() throws Exception {
		var meta = new RrpcInterfaceMethodMeta("http://x", getMethod("getWithArg"));
		assertEquals("getWithArg/(java.lang.String)", meta.getPath());
	}

	@Test void a04_getUri() throws Exception {
		var meta = new RrpcInterfaceMethodMeta("http://x", getMethod("get"));
		assertTrue(meta.getUri().startsWith("http://x/"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_equals_sameMethod_isEqual() throws Exception {
		var m1 = new RrpcInterfaceMethodMeta("http://x", getMethod("get"));
		var m2 = new RrpcInterfaceMethodMeta("http://y", getMethod("get"));
		assertEquals(m1, m2);
		assertEquals(m1.hashCode(), m2.hashCode());
	}

	@Test void b02_equals_differentMethod_notEqual() throws Exception {
		var m1 = new RrpcInterfaceMethodMeta("http://x", getMethod("get"));
		var m2 = new RrpcInterfaceMethodMeta("http://x", getMethod("getWithArg"));
		assertFalse(m1.equals(m2));
	}

	@SuppressWarnings({
		"unlikely-arg-type" // Intentionally comparing to a mismatched type to cover the equals() type-guard branch.
	})
	@Test void b03_equals_notAnInstance_returnsFalse() throws Exception {
		var m1 = new RrpcInterfaceMethodMeta("http://x", getMethod("get"));
		assertFalse(m1.equals("not a meta"));
	}

	@Test void b04_equals_null_returnsFalse() throws Exception {
		var m1 = new RrpcInterfaceMethodMeta("http://x", getMethod("get"));
		assertFalse(m1.equals(null));
	}

	@Test void b05_hashCode_matchesMethodHashCode() throws Exception {
		var m = getMethod("get");
		var meta = new RrpcInterfaceMethodMeta("http://x", m);
		assertEquals(m.hashCode(), meta.hashCode());
	}
}
