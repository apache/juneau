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
package org.apache.juneau.http.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RrpcInterfaceMethodMeta}.
 */
class RrpcInterfaceMethodMeta_Test extends TestBase {

	// Regression: getHeaderDefault/getQueryDefault/getFormDataDefault/getPathDefault(null) NPE'd because they
	// delegated straight into a null-hostile Map.of()-backed map, despite each documenting an "or null" @return.
	@Test void a01_getXxxDefault_nullName() throws Exception {
		var m = Object.class.getMethod("toString");
		var meta = new RrpcInterfaceMethodMeta(m, "POST", "/x", RemoteReturn.BODY);

		assertNull(meta.getHeaderDefault(null));
		assertNull(meta.getQueryDefault(null));
		assertNull(meta.getFormDataDefault(null));
		assertNull(meta.getPathDefault(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Policy — 0% branch: null-defensive compact constructor, defensive-copy accessor, equals()/hashCode()/toString()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_policy_none_isEmpty() {
		var p = RrpcInterfaceMethodMeta.Policy.NONE;
		assertEquals(0, p.interceptors().length);
		assertEquals("", p.timeout());
		assertEquals(0, p.retries());
		assertFalse(p.retryNonIdempotent());
		assertFalse(p.throwOnError());
	}

	@Test void b02_policy_compactConstructor_nullInterceptorsDefaultsToEmptyArray() {
		var p = new RrpcInterfaceMethodMeta.Policy(null, "5s", 3, true, true);
		assertEquals(0, p.interceptors().length);
	}

	@Test void b03_policy_compactConstructor_clonesInterceptorsArray() {
		var arr = new Class<?>[]{String.class};
		var p = new RrpcInterfaceMethodMeta.Policy(arr, "", 0, false, false);
		arr[0] = Integer.class; // Mutate caller's array after construction.
		assertEquals(String.class, p.interceptors()[0]);
	}

	@Test void b04_policy_interceptors_accessorReturnsDefensiveCopy() {
		var p = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{String.class}, "", 0, false, false);
		var a = p.interceptors();
		a[0] = Integer.class; // Mutate the returned array.
		assertEquals(String.class, p.interceptors()[0]); // Internal state unaffected.
	}

	@Test void b05_policy_equals_reflexive() {
		var p = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{String.class}, "5s", 3, true, true);
		assertEquals(p, p);
	}

	@Test void b06_policy_equals_wrongType() {
		var p = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 0, false, false);
		assertNotEquals("not a policy", p);
	}

	@Test void b07_policy_equals_sameValues() {
		var p1 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{String.class}, "5s", 3, true, true);
		var p2 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{String.class}, "5s", 3, true, true);
		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@Test void b08_policy_equals_differentInterceptors() {
		var p1 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{String.class}, "5s", 3, true, true);
		var p2 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{Integer.class}, "5s", 3, true, true);
		assertNotEquals(p1, p2);
	}

	@Test void b09_policy_equals_differentTimeout() {
		var p1 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "5s", 3, true, true);
		var p2 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "10s", 3, true, true);
		assertNotEquals(p1, p2);
	}

	@Test void b10_policy_equals_differentRetries() {
		var p1 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 3, true, true);
		var p2 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 4, true, true);
		assertNotEquals(p1, p2);
	}

	@Test void b11_policy_equals_differentRetryNonIdempotent() {
		var p1 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 0, true, true);
		var p2 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 0, false, true);
		assertNotEquals(p1, p2);
	}

	@Test void b12_policy_equals_differentThrowOnError() {
		var p1 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 0, false, true);
		var p2 = new RrpcInterfaceMethodMeta.Policy(new Class<?>[0], "", 0, false, false);
		assertNotEquals(p1, p2);
	}

	@Test void b13_policy_toString() {
		var p = new RrpcInterfaceMethodMeta.Policy(new Class<?>[]{String.class}, "5s", 3, true, false);
		var s = p.toString();
		assertTrue(s.contains("interceptors=[class java.lang.String]"));
		assertTrue(s.contains("timeout=5s"));
		assertTrue(s.contains("retries=3"));
		assertTrue(s.contains("retryNonIdempotent=true"));
		assertTrue(s.contains("throwOnError=false"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// ContentNegotiation
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_contentNegotiation_none_isEmpty() {
		var n = RrpcInterfaceMethodMeta.ContentNegotiation.NONE;
		assertEquals("", n.accept());
		assertEquals("", n.contentType());
	}

	@Test void c02_contentNegotiation_accessors() {
		var n = new RrpcInterfaceMethodMeta.ContentNegotiation("application/json", "application/xml");
		assertEquals("application/json", n.accept());
		assertEquals("application/xml", n.contentType());
	}
}
