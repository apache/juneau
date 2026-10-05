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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the canonical {@link Shorts} argument checks ({@code req*}, throwing {@link IllegalArgumentException})
 * and state checks ({@code chk*}, throwing {@link IllegalStateException}).
 */
@SuppressWarnings({
	"java:S5961" // High assertion count acceptable in comprehensive test
})
@TestMethodOrder(MethodOrderer.MethodName.class)
class Shorts_Req_Test extends TestBase {

	//====================================================================================================
	// req(boolean, String, Object...)
	//====================================================================================================

	@Test
	void a001_req_true() {
		assertDoesNotThrow(() -> req(true, "never thrown %s", "x"));
	}

	@Test
	void a002_req_false() {
		var e = assertThrows(IllegalArgumentException.class, () -> req(false, "Bad value: %s", "x"));
		assertBean(e, "message", "Bad value: x");
	}

	@Test
	void a003_req_falseNoArgs() {
		var e = assertThrows(IllegalArgumentException.class, () -> req(false, "Plain message"));
		assertBean(e, "message", "Plain message");
	}

	//====================================================================================================
	// reqnn(String, T)
	//====================================================================================================

	@Test
	void b001_reqnn_returnsSameInstance() {
		var v = new Object();
		assertSame(v, reqnn("v", v));
	}

	@Test
	void b002_reqnn_null() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnn("foo", null));
		assertBean(e, "message", "Argument 'foo' cannot be null.");
	}

	//====================================================================================================
	// reqnn(String, Object, ... ×2..5)
	//====================================================================================================

	@Test
	void c001_reqnn2_allPresent() {
		assertDoesNotThrow(() -> reqnn("a", 1, "b", 2));
	}

	@Test
	void c002_reqnn2_secondNull() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnn("a", 1, "b", null));
		assertBean(e, "message", "Argument 'b' cannot be null.");
	}

	@Test
	void c003_reqnn2_bothNullReportsFirst() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnn("a", null, "b", null));
		assertBean(e, "message", "Argument 'a' cannot be null.");
	}

	@Test
	void c004_reqnn3() {
		assertDoesNotThrow(() -> reqnn("a", 1, "b", 2, "c", 3));
		var e = assertThrows(IllegalArgumentException.class, () -> reqnn("a", 1, "b", 2, "c", null));
		assertBean(e, "message", "Argument 'c' cannot be null.");
	}

	@Test
	void c005_reqnn4() {
		assertDoesNotThrow(() -> reqnn("a", 1, "b", 2, "c", 3, "d", 4));
		var e = assertThrows(IllegalArgumentException.class, () -> reqnn("a", 1, "b", 2, "c", 3, "d", null));
		assertBean(e, "message", "Argument 'd' cannot be null.");
	}

	@Test
	void c006_reqnn5() {
		assertDoesNotThrow(() -> reqnn("a", 1, "b", 2, "c", 3, "d", 4, "e", 5));
		var e = assertThrows(IllegalArgumentException.class, () -> reqnn("a", 1, "b", 2, "c", 3, "d", 4, "e", null));
		assertBean(e, "message", "Argument 'e' cannot be null.");
	}

	//====================================================================================================
	// reqnb(String, String)
	//====================================================================================================

	@Test
	void d001_reqnb_returnsValue() {
		assertEquals(" x ", reqnb("s", " x "));
	}

	@Test
	void d002_reqnb_null() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnb("s", null));
		assertBean(e, "message", "Argument 's' cannot be null.");
	}

	@Test
	void d003_reqnb_empty() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnb("s", ""));
		assertBean(e, "message", "Argument 's' cannot be blank.");
	}

	@Test
	void d004_reqnb_whitespace() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnb("s", " \t\n"));
		assertBean(e, "message", "Argument 's' cannot be blank.");
	}

	//====================================================================================================
	// reqnns(String, T[]) / reqnns(String, Collection)
	//====================================================================================================

	@Test
	void e001_reqnns_array() {
		var a = new String[]{"x", "y"};
		assertSame(a, reqnns("a", a));
		assertList(reqnns("a", new String[0]));
	}

	@Test
	void e002_reqnns_arrayNull() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnns("a", (String[])null));
		assertBean(e, "message", "Argument 'a' cannot be null.");
	}

	@Test
	void e003_reqnns_arrayNullElement() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnns("a", new String[]{"x", null}));
		assertBean(e, "message", "Argument 'a' parameter 1 cannot be null.");
	}

	@Test
	void e004_reqnns_collection() {
		var c = List.of("x", "y");
		assertSame(c, reqnns("c", c));
		assertList(reqnns("c", new ArrayList<String>()));
	}

	@Test
	void e005_reqnns_collectionNull() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqnns("c", (List<String>)null));
		assertBean(e, "message", "Argument 'c' cannot be null.");
	}

	@Test
	void e006_reqnns_collectionNullElement() {
		var c = new ArrayList<String>();
		c.add("x");
		c.add("y");
		c.add(null);
		var e = assertThrows(IllegalArgumentException.class, () -> reqnns("c", c));
		assertBean(e, "message", "Argument 'c' element at index 2 cannot be null.");
	}

	//====================================================================================================
	// reqt(Class, Object) / reqt(Class, Object, Supplier)
	//====================================================================================================

	@Test
	void f001_reqt_match() {
		CharSequence s = reqt(CharSequence.class, "x");
		assertEquals("x", s);
	}

	@Test
	void f002_reqt_mismatch() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqt(String.class, 1));
		assertBean(e, "message", "Object is not an instance of java.lang.String: java.lang.Integer");
	}

	@Test
	void f003_reqt_nullType() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqt(null, "x"));
		assertBean(e, "message", "Argument 'type' cannot be null.");
	}

	@Test
	void f004_reqt_nullObject() {
		var e = assertThrows(IllegalArgumentException.class, () -> reqt(String.class, null));
		assertBean(e, "message", "Argument 'o' cannot be null.");
	}

	@Test
	void f005_reqtSupplier_match() {
		assertEquals("x", reqt(String.class, "x", () -> isex("never thrown")));
	}

	@Test
	void f006_reqtSupplier_mismatchThrowsSuppliedException() {
		var e = assertThrows(IllegalStateException.class, () -> reqt(String.class, 1, () -> isex("Wrong type")));
		assertBean(e, "message", "Wrong type");
	}

	@Test
	void f007_reqtSupplier_nullArgsStillIae() {
		var e1 = assertThrows(IllegalArgumentException.class, () -> reqt(null, "x", () -> isex("unused")));
		assertBean(e1, "message", "Argument 'type' cannot be null.");
		var e2 = assertThrows(IllegalArgumentException.class, () -> reqt(String.class, null, () -> isex("unused")));
		assertBean(e2, "message", "Argument 'o' cannot be null.");
	}

	//====================================================================================================
	// reqcat(String, Class, Class[])
	//====================================================================================================

	@Test
	void g001_reqcat_allSubtypes() {
		Class<?>[] in = {Integer.class, Long.class, Number.class};
		Class<Number>[] out = reqcat("types", Number.class, in);
		assertSame(in, out);
		assertList(reqcat("types", Number.class, new Class<?>[0]));
	}

	@Test
	void g002_reqcat_mismatch() {
		Class<?>[] in = {Integer.class, String.class};
		var e = assertThrows(IllegalArgumentException.class, () -> reqcat("types", Number.class, in));
		assertBean(e, "message", "Arg types did not have arg of type java.lang.Number at index 1: java.lang.String");
	}

	@Test
	void g003_reqcat_nullArgsUnguarded() {
		Class<?>[] in = {Integer.class};
		assertThrows(NullPointerException.class, () -> reqcat("types", Number.class, null));
		assertThrows(NullPointerException.class, () -> reqcat("types", null, in));
	}

	//====================================================================================================
	// chk(boolean, String, Object...) / chknn(T, String, Object...)
	//====================================================================================================

	@Test
	void h001_chk_true() {
		assertDoesNotThrow(() -> chk(true, "never thrown"));
	}

	@Test
	void h002_chk_false() {
		var e = assertThrows(IllegalStateException.class, () -> chk(false, "Bad state: %s", 42));
		assertBean(e, "message", "Bad state: 42");
	}

	@Test
	void h003_chknn_returnsSameInstance() {
		var v = new Object();
		assertSame(v, chknn(v, "never thrown"));
	}

	@Test
	void h004_chknn_null() {
		var e = assertThrows(IllegalStateException.class, () -> chknn(null, "Not connected: %s", "db"));
		assertBean(e, "message", "Not connected: db");
	}
}
