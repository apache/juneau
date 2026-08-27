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

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.*;

import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Regression tests for {@link RestContext#handleNotFound(RestSession)}.
 *
 * <p>
 * Reaching {@code handleNotFound} means no operation matched the request, so the outcome must always be a 404 for any
 * non-error status.  The status carried into this method mirrors {@code HttpServletResponse.getStatus()}: under a real
 * servlet container that defaults to {@code 200}, whereas {@link org.apache.juneau.rest.mock.MockServletResponse}'s
 * status defaults to a coincidental {@code 0}.  That mock default is exactly why a straight {@code else}-throws-500
 * branch stayed invisible to the mock-based suite while turning every unmatched path into an HTTP 500 in production.
 * These tests stub the session status directly so both the container-default {@code 200} and the mock-default
 * {@code 0} are exercised, without depending on {@code juneau-rest-mock} (which is intentionally absent from this
 * module's test scope to avoid a Maven reactor cycle).
 *
 * @since 10.0.0
 */
class RestContext_HandleNotFound_Test extends org.apache.juneau.TestBase {

	@Rest
	static class A_Bare {}

	static RestContext.Args argsOf(Class<?> resourceClass, Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	private static RestContext bareContext() throws Exception {
		return new RestContext(argsOf(A_Bare.class, A_Bare::new));
	}

	private static RestSession sessionWithStatus(int status) {
		var session = mock(RestSession.class);
		when(session.getStatus()).thenReturn(status);
		when(session.getPathInfo()).thenReturn("/missing");
		when(session.getMethod()).thenReturn("GET");
		return session;
	}

	//-----------------------------------------------------------------------------------------------------------
	// a - A still-successful or unset status resolves to a clean 404, never a 500.
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_successStatus_realContainerDefault200_throwsNotFound() throws Exception {
		assertThrows(NotFound.class, () -> bareContext().handleNotFound(sessionWithStatus(SC_OK)));
	}

	@Test void a02_unsetStatus_mockDefault0_throwsNotFound() throws Exception {
		assertThrows(NotFound.class, () -> bareContext().handleNotFound(sessionWithStatus(0)));
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - The recognized status codes keep their existing, distinct exceptions.
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_notFoundStatus_throwsNotFound() throws Exception {
		assertThrows(NotFound.class, () -> bareContext().handleNotFound(sessionWithStatus(SC_NOT_FOUND)));
	}

	@Test void b02_preconditionFailedStatus_throwsPreconditionFailed() throws Exception {
		assertThrows(PreconditionFailed.class, () -> bareContext().handleNotFound(sessionWithStatus(SC_PRECONDITION_FAILED)));
	}

	@Test void b03_methodNotAllowedStatus_throwsMethodNotAllowed() throws Exception {
		assertThrows(MethodNotAllowed.class, () -> bareContext().handleNotFound(sessionWithStatus(SC_METHOD_NOT_ALLOWED)));
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - A genuinely unexpected error status still surfaces the "invalid method response" 500.
	//-----------------------------------------------------------------------------------------------------------

	@Test void c01_unexpectedErrorStatus_throwsServletException() throws Exception {
		var e = assertThrows(ServletException.class, () -> bareContext().handleNotFound(sessionWithStatus(SC_INTERNAL_SERVER_ERROR)));
		assertTrue(e.getMessage().contains("Invalid method response"));
	}
}
