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
package org.apache.juneau.http.response;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BasicHttpResponse} constructors, copy constructor, and {@code equals()}/{@code hashCode()}/
 * {@code toString()} -- exercised via the concrete leaf {@link Ok}.
 */
class BasicHttpResponse_Test extends TestBase {

	@Test void a01_ctor_noBody_defaultsToReasonPhraseBody() {
		var r = new Ok();
		assertEquals("OK", r.getBody().toString());
	}

	@Test void a02_ctor_nullReasonPhrase_noBody() {
		var sl = HttpStatusLineBean.of(HttpProtocolVersion.HTTP_1_1, 200, null);
		var r = new NoReasonOk(sl);
		assertNull(r.getBody());
		assertNull(r.getStatusLine().getReasonPhrase());
	}

	@Test void a02b_ctor_nullStatusLine_rejected() {
		// Line 78-79's `statusLine != null && ...` short-circuits to `null` for the body arg, but the delegate
		// constructor still validates the status line itself and throws.
		assertThrows(IllegalArgumentException.class, () -> new NoReasonOk(null));
	}

	@Test void a03_ctor_httpBody() {
		var r = new Ok(StringBody.of("hi"));
		assertEquals("hi", r.getBody().toString());
	}

	@Test void a04_ctor_httpBody_null() {
		var r = new Ok((HttpBody)null);
		assertNull(r.getBody());
	}

	@Test void a05_ctor_stringBody() {
		var r = new Ok("hi");
		assertEquals("hi", r.getBody().toString());
	}

	@Test void a06_ctor_stringBody_null() {
		var r = new Ok((String)null);
		assertNull(r.getBody());
	}

	@Test void a07_copyCtor_copiesState() {
		var orig = new Ok().addHeader("X-Trace", "1").setLocale(java.util.Locale.GERMAN);
		var copy = new Ok(orig);
		assertEquals(1, copy.getHeaders().size());
		assertEquals(java.util.Locale.GERMAN, copy.getLocale());
		// Headers list is a defensive copy -- mutating the original doesn't affect the copy.
		orig.addHeader("X-More", "2");
		assertEquals(1, copy.getHeaders().size());
	}

	@Test void a08_equals_sameInstance() {
		var r = new Ok();
		assertEquals(r, r);
	}

	/*
	 * BUG (not silently fixed): equals()/hashCode() delegate to the body field's equals(), but
	 * StringBody has no equals()/hashCode() override (identity-based), so two independently constructed responses
	 * that would otherwise be logically identical (same status, same headers, same body *text*) never compare equal
	 * -- here specifically because BasicHttpResponse's own no-body constructor synthesizes a fresh StringBody from
	 * the reason phrase, and each Ok() call produces its own distinct StringBody instance. This test pins the
	 * current (surprising) behavior rather than the "value equality" contract the javadoc on equals() implies.
	 */
	@Test void a09_equals_independentInstances_neverEqual_dueToIdentityBasedBodyEquals() {
		var r1 = new Ok().addHeader("A", "1");
		var r2 = new Ok().addHeader("A", "1");
		assertNotEquals(r1, r2);
	}

	@Test void a09b_equals_copyConstructor_sharesBodyInstance_isEqual() {
		var r1 = new Ok().addHeader("A", "1");
		var r2 = new Ok(r1);
		// Headers compare structurally (HttpHeaderList extends ArrayList), but the copy constructor also shares the
		// same body reference, which is what actually makes this pair equal despite the bug noted above.
		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
	}

	@Test void a10_equals_differentStatusLine() {
		var r1 = new Ok();
		var r2 = new Ok().setStatusCode(201);
		assertNotEquals(r1, r2);
	}

	@Test void a11_equals_differentHeaders() {
		var r1 = new Ok();
		var r2 = new Ok().addHeader("A", "1");
		assertNotEquals(r1, r2);
	}

	@Test void a12_equals_differentBody() {
		var r1 = new Ok("hi");
		var r2 = new Ok("bye");
		assertNotEquals(r1, r2);
	}

	@Test void a13_equals_null() {
		// assertNotEquals(null, r) would call objectsAreEqual(null, r), which short-circuits on the null check
		// without ever invoking r.equals(null) -- assert the direction that actually exercises the `instanceof`
		// check's false branch.
		var r = new Ok();
		assertFalse(r.equals(null));
	}

	@SuppressWarnings({
		"unlikely-arg-type" // Intentionally comparing to a mismatched type to cover the equals() type-guard branch.
	})
	@Test void a14_equals_differentType() {
		// Likewise, assertNotEquals("not a response", r) would call "not a response".equals(r), which trivially
		// returns false via String.equals() -- never touching BasicHttpResponse.equals() at all.
		var r = new Ok();
		assertFalse(r.equals("not a response"));
	}

	@Test void a15_toString_delegatesToStatusLine() {
		var r = new Ok();
		assertEquals(r.getStatusLine().toString(), r.toString());
	}

	/** Minimal leaf used only to exercise the no-body constructor with a null reason phrase. */
	static class NoReasonOk extends BasicHttpResponse<NoReasonOk> {
		NoReasonOk(HttpStatusLine statusLine) {
			super(statusLine);
		}

		@Override
		public NoReasonOk unmodifiable() {
			return this;
		}
	}
}
