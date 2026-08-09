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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.rrpc.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests for the error-response detail decisions in {@link RestContext#handleError(RestSession, Throwable)},
 * exercised via the package-private helpers
 * {@link RestContext#resolveThrownHeader(BasicHttpException, boolean, boolean)},
 * {@link RestContext#isRpcDispatch(RestSession)}, and
 * {@link RestContext#suppressedErrorBodyMessage(BasicHttpException, boolean)}.
 *
 * @since 10.0.0
 */
class RestContext_Test extends org.apache.juneau.TestBase {

	// -----------------------------------------------------------------------------------------
	// a — resolveThrownHeader
	// -----------------------------------------------------------------------------------------

	@Test void a01_resolveThrownHeader_stackTracesOff_returnsNull() {
		var e2 = new BasicHttpException(500, null, new IllegalStateException("internal detail"));
		assertNull(RestContext.resolveThrownHeader(e2, false, false));
	}

	/**
	 * Covers the three stackTraces/rpcDispatch combinations that carry a root cause through to the
	 * returned header (stackTraces-on-only, rpcDispatch-on-only, and both-on).
	 */
	@ParameterizedTest
	@CsvSource({
		"true,false",
		"false,true",
		"true,true"
	})
	void a02_resolveThrownHeader_returnsHeaderWithRootCause(boolean stackTraces, boolean rpcDispatch) {
		var cause = new IllegalStateException("internal detail");
		var e2 = new BasicHttpException(500, null, cause);
		var t = RestContext.resolveThrownHeader(e2, stackTraces, rpcDispatch);
		assertNotNull(t);
		assertEquals("Thrown", t.getName());
		assertTrue(t.getValue().contains("IllegalStateException"));
	}

	@Test void a03_resolveThrownHeader_stackTracesOn_noRootCause_returnsNull() {
		var e2 = new BasicHttpException(404, "Not Found");
		assertNull(RestContext.resolveThrownHeader(e2, true, false));
	}

	// -----------------------------------------------------------------------------------------
	// b — suppressedErrorBodyMessage
	// -----------------------------------------------------------------------------------------

	@Test void b01_suppressedErrorBodyMessage_notAppThrown_returnsEmpty() {
		var e2 = new BasicHttpException(500, null, new IllegalStateException("relation \"internal_billing\" does not exist"));
		assertEquals("", RestContext.suppressedErrorBodyMessage(e2, false));
	}

	@Test void b02_suppressedErrorBodyMessage_appThrown_returnsMessage() {
		var e2 = new NotFound("Widget %s not found", "42");
		assertEquals("Widget 42 not found", RestContext.suppressedErrorBodyMessage(e2, true));
	}

	@Test void b03_suppressedErrorBodyMessage_appThrown_scrubsXssChars() {
		var e2 = new BadRequest("bad <script>alert(1)</script> & value");
		var msg = RestContext.suppressedErrorBodyMessage(e2, true);
		assertFalse(msg.contains("<"));
		assertFalse(msg.contains(">"));
		assertFalse(msg.contains("&"));
	}

	@Test void b04_suppressedErrorBodyMessage_appThrown_noExplicitMessage_fallsBackToReasonPhrase() {
		var e2 = new NotFound();
		assertEquals("Not Found", RestContext.suppressedErrorBodyMessage(e2, true));
	}

	// -----------------------------------------------------------------------------------------
	// c — isRpcDispatch
	// -----------------------------------------------------------------------------------------

	@Test void c01_isRpcDispatch_rrpcOpContext_returnsTrue() {
		var opContext = mock(RrpcRestOpContext.class);
		var opSession = mock(RestOpSession.class);
		when(opSession.getContext()).thenReturn(opContext);
		var session = mock(RestSession.class);
		when(session.getOpSessionOrNull()).thenReturn(opSession);
		assertTrue(RestContext.isRpcDispatch(session));
	}

	@Test void c02_isRpcDispatch_ordinaryOpContext_returnsFalse() {
		var opContext = mock(RestOpContext.class);
		var opSession = mock(RestOpSession.class);
		when(opSession.getContext()).thenReturn(opContext);
		var session = mock(RestSession.class);
		when(session.getOpSessionOrNull()).thenReturn(opSession);
		assertFalse(RestContext.isRpcDispatch(session));
	}

	@Test void c03_isRpcDispatch_noOpSession_returnsFalse() {
		var session = mock(RestSession.class);
		when(session.getOpSessionOrNull()).thenReturn(null);
		assertFalse(RestContext.isRpcDispatch(session));
	}
}
