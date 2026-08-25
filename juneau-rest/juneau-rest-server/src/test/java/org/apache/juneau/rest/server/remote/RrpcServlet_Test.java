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
package org.apache.juneau.rest.server.remote;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RrpcServlet}.
 *
 * <p>
 * {@code MockRestClient} is intentionally absent from this module's test scope to avoid Maven reactor cycles
 * (see {@code RestArgResolvers_Test}'s note) -- {@code juneau-rest-mock} depends on this module, not the other
 * way around. {@link RrpcServlet}'s methods are plain, framework-agnostic Java methods (the {@code @RestGet}/
 * {@code @RestPost}/{@code @Path}/{@code @Header} annotations are dispatch metadata only), so they're exercised
 * here via direct method calls rather than an HTTP round-trip.
 *
 * <p>
 * {@code juneau-integration-tests}'s own {@code RrpcServlet_Test} already covers the HTTP-dispatch happy paths
 * ({@code getInterfaces}, {@code listMethods}, {@code invoke}'s success/404/500 cases) end-to-end via
 * {@code MockRestClient}; this class targets the gaps that test doesn't reach: the two defensive branches in
 * {@code invoke()} and the entirely-untested {@code showEntryForm()} form-rendering endpoint.
 */
class RrpcServlet_Test extends TestBase {

	// =========================================================================
	// Test interface and implementation
	// =========================================================================

	@SuppressWarnings({
		"java:S114" // L_categoryName test-grouping convention (see Test Class Naming Convention), not a real naming violation.
	})
	public interface A_TestInterface {
		int add(int a, int b);
		void doNothing();
	}

	public static class A_TestInterfaceImpl implements A_TestInterface {
		@Override public int add(int a, int b) { return a + b; }
		@Override public void doNothing() { /* Intentionally empty: exercises RPC dispatch of a void no-op method. */ }
	}

	public static class A_TestRrpcServlet extends RrpcServlet {
		private static final long serialVersionUID = 1L;
		private final Map<Class<?>,Object> services = Map.of(A_TestInterface.class, new A_TestInterfaceImpl());

		@Override
		protected Map<Class<?>,Object> getServiceMap() {
			return services;
		}
	}

	// =========================================================================
	// a — invoke(): parser-not-found defensive branch
	// =========================================================================

	@Test void a01_invoke_nullParser_throwsUnsupportedMediaType() {
		var servlet = new A_TestRrpcServlet();
		var iface = A_TestInterface.class.getName();
		assertThrowsWithMessage(UnsupportedMediaType.class, "text/plain",
			() -> servlet.invoke(new StringReader("[]"), null, ContentType.of("text/plain"), iface, "doNothing/"));
	}

	// =========================================================================
	// b — invoke(): service-not-found defensive branch
	// =========================================================================

	/**
	 * {@link RrpcServlet#getServiceMap()} is documented as "called often and not cached" -- a subclass may
	 * legitimately return a different map on each call. {@code invoke()} calls it twice per request (once
	 * indirectly via the private {@code getInterfaceClass()} to resolve the interface, once directly to look up
	 * the service instance), so a subclass whose map mutates between those two calls can hit the
	 * "Service not found" branch even though the interface itself resolved fine on the first call.
	 */
	public static class B01_FlakyServiceMapServlet extends RrpcServlet {
		private static final long serialVersionUID = 1L;
		private final Map<Class<?>,Object> full = Map.of(A_TestInterface.class, new A_TestInterfaceImpl());
		private final Flag firstCallDone = Flag.create();

		@Override
		protected Map<Class<?>,Object> getServiceMap() {
			if (firstCallDone.isUnset()) {
				firstCallDone.set();
				return full;
			}
			return Map.of();
		}
	}

	@Test void b01_invoke_serviceRemovedBetweenLookups_throwsNotFound() {
		var servlet = new B01_FlakyServiceMapServlet();
		var iface = A_TestInterface.class.getName();
		assertThrowsWithMessage(NotFound.class, "Service not found",
			() -> servlet.invoke(new StringReader("[]"), JsonParser.DEFAULT, ContentType.of("application/json"), iface, "doNothing/"));
	}

	// =========================================================================
	// c — showEntryForm(): entirely-untested HTML form-rendering endpoint
	// =========================================================================

	@Test void c01_showEntryForm_methodNotFound_throwsNotFound() {
		var servlet = new A_TestRrpcServlet();
		var iface = A_TestInterface.class.getName();
		assertThrowsWithMessage(NotFound.class, "Method not found", () -> servlet.showEntryForm(iface, "noSuchMethod"));
	}

	@Test void c02_showEntryForm_noArgMethod_rendersNoArgumentsMessage() throws Exception {
		var servlet = new A_TestRrpcServlet();
		var iface = A_TestInterface.class.getName();
		var html = servlet.showEntryForm(iface, "doNothing/").toString();
		assertTrue(html.contains("No arguments"), "Expected 'No arguments' in: " + html);
		assertFalse(html.contains("Reset"), "Reset button shouldn't render for a no-arg method: " + html);
	}

	@Test void c03_showEntryForm_methodWithArgs_rendersArgumentTable() throws Exception {
		var servlet = new A_TestRrpcServlet();
		var iface = A_TestInterface.class.getName();
		var html = servlet.showEntryForm(iface, "add/(int,int)").toString();
		assertTrue(html.contains("Index"), "Expected argument table headers in: " + html);
		assertTrue(html.contains("int"), "Expected parameter type 'int' in: " + html);
		assertTrue(html.contains("Reset"), "Expected a Reset button for an args method in: " + html);
	}
}
