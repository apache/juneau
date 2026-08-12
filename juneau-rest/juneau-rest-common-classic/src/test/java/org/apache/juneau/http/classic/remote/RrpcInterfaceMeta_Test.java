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
import org.apache.juneau.http.remote.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RrpcInterfaceMeta}.
 */
class RrpcInterfaceMeta_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Constructor -- path resolution from @Remote
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ctor_noRemoteAnnotation_emptyPath() {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertEquals("", m.getPath());
	}

	@Test void a02_ctor_remoteAnnotation_pathTrimmed() {
		var m = new RrpcInterfaceMeta(PathedIface.class, "http://x");
		assertEquals("my/path", m.getPath());
	}

	@Test void a03_ctor_remoteAnnotation_emptyPath_ignored() {
		var m = new RrpcInterfaceMeta(PlainRemoteIface.class, "http://x");
		assertEquals("", m.getPath());
	}

	//------------------------------------------------------------------------------------------------------------------
	// getJavaClass()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getJavaClass() {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertEquals(PlainIface.class, m.getJavaClass());
	}

	//------------------------------------------------------------------------------------------------------------------
	// getMethodMeta(Method) / getMethodMetaByPath(String) / getMethodsByPath()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getMethodMeta_found() throws Exception {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		var jm = PlainIface.class.getMethod("get");
		assertNotNull(m.getMethodMeta(jm));
	}

	@Test void c02_getMethodMeta_notFound() throws Exception {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		var jm = PathedIface.class.getMethod("get");
		assertNull(m.getMethodMeta(jm));
	}

	@Test void c03_getMethodMetaByPath_null_returnsNull() {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertNull(m.getMethodMetaByPath(null));
	}

	@Test void c04_getMethodMetaByPath_found() {
		var m = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertEquals(1, m.getMethodsByPath().size());
		var path = m.getMethodsByPath().keySet().iterator().next();
		assertNotNull(m.getMethodMetaByPath(path));
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode()
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_equals_sameClass_isEqual() {
		var m1 = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		var m2 = new RrpcInterfaceMeta(PlainIface.class, "http://y");
		assertEquals(m1, m2);
	}

	@Test void d02_equals_differentClass_notEqual() {
		var m1 = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		var m2 = new RrpcInterfaceMeta(PathedIface.class, "http://x");
		assertFalse(m1.equals(m2));
	}

	@SuppressWarnings({
		"unlikely-arg-type" // Intentionally comparing to a mismatched type to cover the equals() type-guard branch.
	})
	@Test void d03_equals_notAnInstance_returnsFalse() {
		var m1 = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertFalse(m1.equals("not a meta"));
	}

	@Test void d04_equals_null_returnsFalse() {
		var m1 = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertFalse(m1.equals(null));
	}

	@Test void d05_hashCode_isIdentityHashOfClass() {
		var m1 = new RrpcInterfaceMeta(PlainIface.class, "http://x");
		assertEquals(System.identityHashCode(PlainIface.class), m1.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//------------------------------------------------------------------------------------------------------------------

	public interface PlainIface {
		String get();
	}

	@Remote(path = "/my/path/")
	public interface PathedIface {
		String get();
	}

	@Remote
	public interface PlainRemoteIface {
		String get();
	}
}
