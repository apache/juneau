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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the error-response posture: internal exception detail is only echoed back to the client when it is
 * explicitly enabled (stack-trace rendering) or required by an RRPC dispatch target, and the plain-text error
 * body only carries a message for application-authored exceptions.
 */
class RestContext_ErrorPosture_Test extends TestBase {

	private static BasicHttpException ex() {
		return new BadRequest(new RuntimeException("root-cause-detail"));
	}

	@Test void a01_thrownHeaderSuppressedByDefault() {
		assertNull(RestContext.resolveThrownHeader(ex(), false, false));
	}

	@Test void a02_thrownHeaderWhenStackTracesRendered() {
		assertNotNull(RestContext.resolveThrownHeader(ex(), true, false));
	}

	@Test void a03_thrownHeaderForRpcDispatch() {
		assertNotNull(RestContext.resolveThrownHeader(ex(), false, true));
	}

	@Test void a04_appThrownMessageEchoed() {
		var e = new BadRequest("visible message");
		assertEquals("visible message", RestContext.suppressedErrorBodyMessage(e, true));
	}

	@Test void a05_nonAppThrownMessageSuppressed() {
		assertEquals("", RestContext.suppressedErrorBodyMessage(ex(), false));
	}

	@Test void a06_appThrownMessageScrubbed() {
		var e = new BadRequest("<b>x</b>&y");
		var r = RestContext.suppressedErrorBodyMessage(e, true);
		assertFalse(r.contains("<"));
		assertFalse(r.contains(">"));
		assertFalse(r.contains("&"));
	}
}
