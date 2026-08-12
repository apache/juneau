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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.impl.client.*;
import org.apache.http.params.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.client.classic.remote.*;
import org.apache.juneau.marshall.uon.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises additional {@link RestClient} branches not reached by {@code juneau-integration-tests}: the
 * instance-level {@code copy()}/{@code getRootUrl()}/{@code getParams()} accessors, the no-arg
 * {@code skipEmptyXxxData()} shortcuts, {@code close()}/{@code closeQuietly()} exception suppression,
 * {@code getRemote(...)}/{@code getRrpcInterface(...)} URI-resolution and SSRF-guard branches, the
 * {@code getPartParser(Class)}/{@code getPartSerializer(Class)} cache-and-instantiate helpers, {@code finalize()},
 * the {@code log(...)} overloads, and the checked-exception-wrapping branch of {@code onCallClose(...)}.
 */
@SuppressWarnings({
	"resource" // Several tests here intentionally leave a RestClient unclosed (to exercise close()/closeQuietly()/finalize() behavior directly) rather than using try-with-resources; Eclipse JDT's @Owning warning is by design.
})
class RestClient_Coverage2_Test {

	private static HttpServer server;
	private static int port;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		server.createContext("/echo", exchange -> {
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private static String url() {
		return "http://localhost:" + port;
	}

	@Test void a01_skipEmptyXxxData_noArg_setsTrue() throws Exception {
		try (var c1 = RestClient.create().skipEmptyFormData().build()) {
			assertTrue(isSkipEmptyFormData(c1));
		}
		try (var c2 = RestClient.create().skipEmptyHeaderData().build()) {
			assertTrue(isSkipEmptyHeaderData(c2));
		}
		try (var c3 = RestClient.create().skipEmptyQueryData().build()) {
			assertTrue(isSkipEmptyQueryData(c3));
		}
	}

	private static boolean isSkipEmptyFormData(RestClient c) { return c.isSkipEmptyFormData(); }
	private static boolean isSkipEmptyHeaderData(RestClient c) { return c.isSkipEmptyHeaderData(); }
	private static boolean isSkipEmptyQueryData(RestClient c) { return c.isSkipEmptyQueryData(); }

	@Test void a02_getRootUrl_setAndUnset() throws Exception {
		try (var withRoot = RestClient.create().rootUrl("http://example.com").build()) {
			assertEquals("http://example.com", withRoot.getRootUrl());
		}
		try (var withoutRoot = RestClient.create().build()) {
			assertNull(withoutRoot.getRootUrl());
		}
	}

	@Test void a03_instanceCopy_notImplemented() throws Exception {
		try (var c = RestClient.create().build()) {
			assertThrows(NoSuchMethodError.class, c::copy);
		}
	}

	@SuppressWarnings({
		"removal" // getParams() is marked for removal (forRemoval=true, so only the "removal" category is emitted, not "deprecation"); exercised intentionally for coverage.
	})
	@Test void a04_getParams_deprecated_delegatesToHttpClient() throws Exception {
		// The default Apache HttpClient (built via HttpClientBuilder) doesn't support the legacy HttpParams API and
		// throws on access; a custom CloseableHttpClient proves the successful delegation path completes normally.
		// Not try-with-resources: the custom client's close() always throws (needed for a06 below), so close it via
		// closeQuietly() instead of letting an auto-close propagate the simulated failure here.
		var c1 = RestClient.create().httpClient(throwingHttpClient()).build();
		assertNull(c1.getParams());
		c1.closeQuietly();

		try (var c2 = RestClient.create().build()) {
			assertThrows(UnsupportedOperationException.class, c2::getParams);
		}
	}

	@Test void a05_closeQuietly_suppressesExceptionFromHttpClientClose() throws Exception {
		var c = RestClient.create().httpClient(throwingHttpClient()).build();
		assertDoesNotThrow(c::closeQuietly);
	}

	@Test void a06_close_propagatesExceptionFromHttpClientClose() throws Exception {
		var c = RestClient.create().httpClient(throwingHttpClient()).build();
		assertThrows(IOException.class, c::close);
	}

	private static CloseableHttpClient throwingHttpClient() {
		return new CloseableHttpClient() {
			@Override
			protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, org.apache.http.protocol.HttpContext context) {
				throw new UnsupportedOperationException("Not used by this test.");
			}
			@Override
			public void close() throws IOException {
				throw new IOException("Simulated close failure.");
			}
			@Override
			@SuppressWarnings("deprecation")
			public HttpParams getParams() { return null; }
			@Override
			@SuppressWarnings("deprecation")
			public ClientConnectionManager getConnectionManager() { return null; }
		};
	}

	public interface EchoRemote {
		@RemoteOp(method = "GET", path = "/echo")
		void call();
	}

	@Test void a07_getRemote_rootUriMissing_throwsRemoteMetadataException() throws Exception {
		try (var c = RestClient.create().build()) {
			var proxy = c.getRemote(EchoRemote.class, null);
			var e = assertThrows(RemoteMetadataException.class, proxy::call);
			assertTrue(e.getMessage().contains("Root URI has not been specified"), "Unexpected message: " + e.getMessage());
		}
	}

	@Test void a08_getRemote_unsupportedScheme_throwsRemoteMetadataException() throws Exception {
		try (var c = RestClient.create().build()) {
			var proxy = c.getRemote(EchoRemote.class, "ftp://badhost");
			var e = assertThrows(RemoteMetadataException.class, proxy::call);
			assertTrue(e.getMessage().contains("Unsupported URL scheme"), "Unexpected message: " + e.getMessage());
		}
	}

	@Test void a09_getRemote_clientLevelRootUrl_resolvesAndSucceeds() throws Exception {
		try (var c = RestClient.create().rootUrl(url()).build()) {
			var proxy = c.getRemote(EchoRemote.class, null);
			assertDoesNotThrow(proxy::call);
		}
	}

	public interface RrpcRemote {
		void call();
	}

	@Test void a10_getRrpcInterface_rootUriMissing_throwsRemoteMetadataException() throws Exception {
		try (var c = RestClient.create().build()) {
			var e = assertThrows(RemoteMetadataException.class, () -> c.getRrpcInterface(RrpcRemote.class));
			assertTrue(e.getMessage().contains("Root URI has not been specified"), "Unexpected message: " + e.getMessage());
		}
	}

	@Test void a11_getPartParser_cachesInstanceAcrossCalls() throws Exception {
		try (var c = RestClient.create().build()) {
			var p1 = c.getPartParser(UonParser.class);
			var p2 = c.getPartParser(UonParser.class);
			assertSame(p1, p2, "Second call should return the cached instance");
		}
	}

	@Test void a12_getPartParser_uninstantiableClass_wrapsExecutableException() throws Exception {
		try (var c = RestClient.create().build()) {
			assertThrows(RuntimeException.class, () -> c.getPartParser(org.apache.juneau.marshall.httppart.HttpPartParser.class));
		}
	}

	@Test void a13_getPartSerializer_cachesInstanceAcrossCalls() throws Exception {
		try (var c = RestClient.create().build()) {
			var s1 = c.getPartSerializer(UonSerializer.class);
			var s2 = c.getPartSerializer(UonSerializer.class);
			assertSame(s1, s2, "Second call should return the cached instance");
		}
	}

	@Test void a14_getPartSerializer_uninstantiableClass_wrapsExecutableException() throws Exception {
		try (var c = RestClient.create().build()) {
			assertThrows(RuntimeException.class, () -> c.getPartSerializer(org.apache.juneau.marshall.httppart.HttpPartSerializer.class));
		}
	}

	@SuppressWarnings({
		"removal" // finalize() is marked for removal (forRemoval=true, so only the "removal" category is emitted, not "deprecation"); exercised intentionally for coverage.
	})
	@Test void a15_finalize_detectLeaks_withoutCreationStack_logsWithoutStackTrace() throws Throwable {
		var c = RestClient.create().detectLeaks().build();
		c.finalize();  // Manually invoked (in-package access); not relying on actual GC timing.
	}

	@SuppressWarnings({
		"removal" // finalize() is marked for removal (forRemoval=true, so only the "removal" category is emitted, not "deprecation"); exercised intentionally for coverage.
	})
	@Test void a16_finalize_detectLeaks_withCreationStack_logsWithStackTrace() throws Throwable {
		var c = RestClient.create().detectLeaks().debug().build();
		c.finalize();  // debug() populates the creation stack trace that finalize() then walks and logs.
	}

	@SuppressWarnings({
		"removal" // finalize() is marked for removal (forRemoval=true, so only the "removal" category is emitted, not "deprecation"); exercised intentionally for coverage.
	})
	@Test void a17_finalize_notDetectLeaks_isNoOp() throws Throwable {
		var c = RestClient.create().build();
		assertDoesNotThrow(c::finalize);
	}

	@SuppressWarnings({
		"removal" // finalize() is marked for removal (forRemoval=true, so only the "removal" category is emitted, not "deprecation"); exercised intentionally for coverage.
	})
	@Test void a17b_finalize_detectLeaks_butAlreadyClosed_isNoOp() throws Throwable {
		var c = RestClient.create().detectLeaks().build();
		c.close();
		assertDoesNotThrow(c::finalize);
	}

	@SuppressWarnings({
		"removal" // finalize() is marked for removal (forRemoval=true, so only the "removal" category is emitted, not "deprecation"); exercised intentionally for coverage.
	})
	@Test void a17c_finalize_detectLeaks_butKeepHttpClientOpen_isNoOp() throws Throwable {
		var c = RestClient.create().detectLeaks().keepHttpClientOpen().build();
		assertDoesNotThrow(c::finalize);
	}

	@Test void a18_log_string_loggableLevel_andConsole() throws Exception {
		try (var out = new ByteArrayOutputStream();
				var c = RestClient.create().logger(alwaysLoggableLogger()).logToConsole().console(new PrintStream(out)).build()) {
			c.log(Level.SEVERE, "hello %s", "world");
			assertTrue(out.toString().contains("hello world"), "Actual console output: " + out);
		}
	}

	@Test void a19_log_string_notLoggableLevel_stillPrintsToConsole() throws Exception {
		try (var out = new ByteArrayOutputStream();
				var c = RestClient.create().logger(neverLoggableLogger()).logToConsole().console(new PrintStream(out)).build()) {
			c.log(Level.SEVERE, "hello %s", "world");
			assertTrue(out.toString().contains("hello world"), "Actual console output: " + out);
		}
	}

	@Test void a20_log_throwable_loggableLevel_andConsole() throws Exception {
		try (var out = new ByteArrayOutputStream();
				var c = RestClient.create().logger(alwaysLoggableLogger()).logToConsole().console(new PrintStream(out)).build()) {
			c.log(Level.SEVERE, new Exception("boom"), "hello %s", "world");
			var s = out.toString();
			assertTrue(s.contains("hello world"), "Actual console output: " + s);
			assertTrue(s.contains("boom"), "Expected stack trace in console output: " + s);
		}
	}

	@Test void a21_log_throwable_notLoggableLevel_stillPrintsToConsole() throws Exception {
		try (var out = new ByteArrayOutputStream();
				var c = RestClient.create().logger(neverLoggableLogger()).logToConsole().console(new PrintStream(out)).build()) {
			c.log(Level.SEVERE, new Exception("boom"), "hello %s", "world");
			var s = out.toString();
			assertTrue(s.contains("hello world"), "Actual console output: " + s);
			assertTrue(s.contains("boom"), "Expected stack trace in console output: " + s);
		}
	}

	@Test void a21b_log_throwable_notLoggableLevel_nullThrowable_printsMessageOnly() throws Exception {
		try (var out = new ByteArrayOutputStream();
				var c = RestClient.create().logger(neverLoggableLogger()).logToConsole().console(new PrintStream(out)).build()) {
			assertDoesNotThrow(() -> c.log(Level.SEVERE, (Throwable)null, "hello %s", "world"));
			assertTrue(out.toString().contains("hello world"), "Actual console output: " + out);
		}
	}

	@Test void a21c_log_notLoggable_andNotLogToConsole_isNoOp() throws Exception {
		try (var c = RestClient.create().logger(neverLoggableLogger()).build()) {
			assertDoesNotThrow(() -> c.log(Level.SEVERE, "hello %s", "world"));
			assertDoesNotThrow(() -> c.log(Level.SEVERE, new Exception("boom"), "hello %s", "world"));
		}
	}

	private static Logger alwaysLoggableLogger() {
		var l = Logger.getLogger("RestClient_Coverage2_Test.alwaysLoggable");
		l.setLevel(Level.ALL);
		return l;
	}

	private static Logger neverLoggableLogger() {
		var l = Logger.getLogger("RestClient_Coverage2_Test.neverLoggable");
		l.setLevel(Level.OFF);
		return l;
	}

	/**
	 * A {@link RestCallInterceptor} whose {@code onClose} throws a checked (non-{@code RuntimeException}) exception,
	 * to exercise {@code RestClient#onCallClose}'s {@code catch (Exception e)} wrapping branch (only reachable via a
	 * checked exception type, since {@code RuntimeException}/{@code RestCallException} are rethrown as-is).
	 */
	public static class ThrowingOnCloseInterceptor extends BasicRestCallInterceptor {
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new java.io.IOException("Simulated onClose failure.");
		}
	}

	@Test void a22_onCallClose_checkedException_wrappedThenLoggedNotThrown() throws Exception {
		// RestCallException extends org.apache.http.HttpException, a CHECKED exception, so RestResponse#close's own
		// try/catch (RuntimeException rethrown, everything else logged) swallows it -- proving onCallClose's
		// catch(Exception) wrapping branch executes without asserting on a exception type that never surfaces to the caller.
		try (var out = new ByteArrayOutputStream();
				var c = RestClient.create().interceptors(new ThrowingOnCloseInterceptor()).logToConsole().console(new PrintStream(out)).build();
				var req = c.get(url() + "/echo");
				var res = req.run()) {
			assertDoesNotThrow(res::close);
			var s = out.toString();
			assertTrue(s.contains("Error during RestResponse close"), "Actual console output: " + s);
			assertTrue(s.contains("Interceptor threw an exception on close"), "Actual console output: " + s);
		}
	}
}
