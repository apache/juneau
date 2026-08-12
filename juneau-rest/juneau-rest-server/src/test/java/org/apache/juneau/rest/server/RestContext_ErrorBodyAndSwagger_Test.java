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

import java.io.*;
import java.util.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.validation.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Tests for {@link RestContext} covering the private-static error-body writers ({@code getResponseWriter},
 * {@code writeProblemDetailsBody}, {@code writeValidationErrorBody} -- exercised via reflection since they're
 * private), the {@code getSwagger}/{@code getOpenApi} per-locale caches, and the {@code properties()}/
 * {@link Object#toString() toString()} debug dump (which fans out to ~20 getters in one shot).
 *
 * @since 10.0.0
 */
@SuppressWarnings("resource") // Mocked ServletOutputStream/HttpServletResponse are in-memory test doubles with no-op close(); the code under test owns wrapping/writing them, not this test.
class RestContext_ErrorBodyAndSwagger_Test extends org.apache.juneau.TestBase {

	static RestContext.Args argsOf(Class<?> resourceClass, java.util.function.Supplier<?> supplier) {
		return new RestContext.Args(resourceClass, null, null, supplier, null, null, null, null, null, null);
	}

	@Rest
	static class Fix_Bare {}

	//-----------------------------------------------------------------------------------------------------------
	// a - getResponseWriter(HttpServletResponse): normal + IllegalStateException fallback branches
	//-----------------------------------------------------------------------------------------------------------

	@Test void a01_getResponseWriter_normal_delegatesToServletWriter() throws Exception {
		var m = RestContext.class.getDeclaredMethod("getResponseWriter", HttpServletResponse.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		try (var sw = new StringWriter(); var pw = new PrintWriter(sw)) {
			when(res.getWriter()).thenReturn(pw);
			var result = (PrintWriter) m.invoke(null, res);
			assertSame(pw, result);
		}
	}

	@Test void a02_getResponseWriter_illegalStateException_fallsBackToOutputStream() throws Exception {
		var m = RestContext.class.getDeclaredMethod("getResponseWriter", HttpServletResponse.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		when(res.getWriter()).thenThrow(new IllegalStateException("already got output stream"));
		when(res.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
		var result = (PrintWriter) m.invoke(null, res);
		assertNotNull(result);
	}

	//-----------------------------------------------------------------------------------------------------------
	// b - writeProblemDetailsBody(HttpServletResponse, BasicHttpException, int): success + IOException fallback
	//-----------------------------------------------------------------------------------------------------------

	@Test void b01_writeProblemDetailsBody_success_returnsTrueAndWritesHeaders() throws Exception {
		var m = RestContext.class.getDeclaredMethod("writeProblemDetailsBody", HttpServletResponse.class, BasicHttpException.class, int.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		var os = mock(ServletOutputStream.class);
		when(res.getOutputStream()).thenReturn(os);
		var e = new InternalServerError("boom");
		var result = (Boolean) m.invoke(null, res, e, 500);
		assertTrue(result);
		verify(res).setStatus(500);
		verify(res).setHeader("Content-Encoding", "identity");
	}

	@Test void b02_writeProblemDetailsBody_ioExceptionDuringWrite_returnsFalse() throws Exception {
		var m = RestContext.class.getDeclaredMethod("writeProblemDetailsBody", HttpServletResponse.class, BasicHttpException.class, int.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		when(res.getOutputStream()).thenThrow(new IOException("simulated"));
		var e = new InternalServerError("boom");
		var result = (Boolean) m.invoke(null, res, e, 500);
		assertFalse(result);
	}

	//-----------------------------------------------------------------------------------------------------------
	// c - writeValidationErrorBody(HttpServletResponse, ValidationException, int, boolean): both problemDetails
	//     branches, plus the IOException fallback.
	//-----------------------------------------------------------------------------------------------------------

	private static ValidationException validationException() {
		return new ValidationException(List.of(new ValidationViolation("name", "must not be null", "NotNull")));
	}

	@Test void c01_writeValidationErrorBody_problemDetailsOn_writesProblemJson() throws Exception {
		var m = RestContext.class.getDeclaredMethod("writeValidationErrorBody", HttpServletResponse.class, ValidationException.class, int.class, boolean.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		when(res.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
		var result = (Boolean) m.invoke(null, res, validationException(), 400, true);
		assertTrue(result);
		verify(res).setContentType("application/problem+json");
	}

	@Test void c02_writeValidationErrorBody_problemDetailsOff_writesSimpleEnvelope() throws Exception {
		var m = RestContext.class.getDeclaredMethod("writeValidationErrorBody", HttpServletResponse.class, ValidationException.class, int.class, boolean.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		when(res.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
		var result = (Boolean) m.invoke(null, res, validationException(), 400, false);
		assertTrue(result);
		verify(res).setContentType("application/json");
	}

	@Test void c03_writeValidationErrorBody_ioException_returnsFalse() throws Exception {
		var m = RestContext.class.getDeclaredMethod("writeValidationErrorBody", HttpServletResponse.class, ValidationException.class, int.class, boolean.class);
		m.setAccessible(true);
		var res = mock(HttpServletResponse.class);
		when(res.getOutputStream()).thenThrow(new IOException("simulated"));
		var result = (Boolean) m.invoke(null, res, validationException(), 400, true);
		assertFalse(result);
	}

	//-----------------------------------------------------------------------------------------------------------
	// d - getSwagger(Locale) / getOpenApi(Locale): cache-miss-then-populate, then cache-hit
	//-----------------------------------------------------------------------------------------------------------

	@Test void d01_getSwagger_cacheMissThenHit() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var first = ctx.getSwagger(Locale.ENGLISH);
		assertNotNull(first);
		var second = ctx.getSwagger(Locale.ENGLISH);
		assertNotNull(second);
	}

	@Test void d02_getOpenApi_cacheMissThenHit() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var first = ctx.getOpenApi(Locale.ENGLISH);
		assertNotNull(first);
		var second = ctx.getOpenApi(Locale.ENGLISH);
		assertNotNull(second);
	}

	//-----------------------------------------------------------------------------------------------------------
	// e - properties()/toString(): fans out to ~20 getters in a single call
	//-----------------------------------------------------------------------------------------------------------

	@Test void e01_toString_includesResolvedProperties() throws Exception {
		var ctx = new RestContext(argsOf(Fix_Bare.class, Fix_Bare::new));
		var s = ctx.toString();
		assertNotNull(s);
		assertFalse(s.isEmpty());
	}
}
