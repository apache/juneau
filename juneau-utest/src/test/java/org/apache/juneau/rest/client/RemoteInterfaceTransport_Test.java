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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.apachehttpclient45.*;
import org.apache.juneau.rest.client.apachehttpclient50.*;
import org.apache.juneau.rest.client.jetty.*;
import org.apache.juneau.rest.client.okhttp.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import jakarta.servlet.*;

/**
 * Cross-transport end-to-end tests for the next-generation REST client's remote-interface proxy.
 *
 * <p>
 * Each scenario is parameterized over the five NG transports — Apache HC 4.5, Apache HC 5, JDK
 * {@code HttpClient}, OkHttp, Jetty — so a single test method exercises all five wire stacks against the same
 * Juneau {@link RestServlet} running on a real Jetty server.
 *
 * <p>
 * The fixture is started once per test class via {@link MicroserviceTestFixture}, and each scenario builds a fresh
 * {@link RestClient} bound to the chosen transport. The classic {@code @Remote}/{@code @RemoteOp} annotations
 * are honored by {@link RestClient#remote(Class)}, with the supported subset of parameter annotations
 * ({@code @Path} / {@code @Query} / {@code @Header} / {@code @Content}) and return types
 * ({@code String}, {@code int} via {@code RemoteReturn.STATUS}, {@code void}).
 */
class RemoteInterfaceTransport_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test REST resource — mounted at /api/* by MicroserviceTestFixture.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(path = "/api", defaultAccept = "text/plain")
	public static class TestService extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path = "/echo/{id}")
		public String echo(@Path("id") String id) {
			return "echo:" + id;
		}

		@RestGet(path = "/segments/{group}/{name}")
		public String segments(@Path("group") String group, @Path("name") String name) {
			return group + "/" + name;
		}

		@RestGet(path = "/query")
		public String query(@Query("q") String q) {
			return "q=" + q;
		}

		@RestPost(path = "/content")
		public String content(@Content String body) {
			return "body=" + body;
		}

		@RestGet(path = "/header")
		public String header(@Header("X-Test") String h) {
			return h == null ? "missing" : h;
		}

		@RestGet(path = "/no-content")
		public void noContent() {
			// 204 No Content (void return + no @Response annotation -> 204 by default convention)
		}

		@RestGet(path = "/missing")
		public String missing() {
			throw new org.apache.juneau.http.classic.response.NotFound("not here");
		}

		@RestPost(path = "/large")
		public String large(@Content String body) {
			return "len=" + body.length();
		}
	}

	@Configuration
	public static class TestServiceConfig {
		@Bean
		public Servlet testService() {
			return new TestService();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Remote interface — the @Remote annotation is honored by RestClient.remote().
	//-----------------------------------------------------------------------------------------------------------------

	@Remote(path = "/api")
	public interface TestApi {

		@RemoteGet("/echo/{id}")
		String echo(@Path("id") String id);

		@RemoteGet("/segments/{group}/{name}")
		String segments(@Path("group") String group, @Path("name") String name);

		@RemoteGet("/query")
		String query(@Query("q") String q);

		@RemotePost("/content")
		String content(@Content String body);

		@RemotePost("/content")
		String contentHttpBody(@Content org.apache.juneau.http.HttpBody body);

		@RemoteGet("/header")
		String header(@Header("X-Test") String h);

		@RemoteGet("/header")
		String headerDefaultOnly();

		@RemoteGet("/no-content")
		void noContent();

		@RemoteGet(value = "/echo/{id}", returns = RemoteReturn.STATUS)
		int echoStatus(@Path("id") String id);

		@RemoteGet(value = "/missing", returns = RemoteReturn.STATUS)
		int missingStatus();

		@RemotePost("/large")
		String large(@Content String body);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fixture — one Jetty server for the whole class.
	//-----------------------------------------------------------------------------------------------------------------

	@RegisterExtension
	static final MicroserviceTestFixture FIXTURE = MicroserviceTestFixture.create().configurations(TestServiceConfig.class);

	//-----------------------------------------------------------------------------------------------------------------
	// Transport providers.
	//-----------------------------------------------------------------------------------------------------------------

	/** Functional interface for a transport supplier that may throw. */
	@FunctionalInterface
	interface TransportSupplier {
		org.apache.juneau.rest.client.HttpTransport get() throws Exception;
	}

	static Stream<Arguments> transports() {
		return Stream.of(
			Arguments.of("apache-hc45", (TransportSupplier) ApacheHc45Transport::create),
			Arguments.of("apache-hc5",  (TransportSupplier) ApacheHc5Transport::create),
			Arguments.of("java-http",   (TransportSupplier) JavaHttpTransport::create),
			Arguments.of("okhttp",      (TransportSupplier) OkHttpTransport::create),
			Arguments.of("jetty",       (TransportSupplier) JettyHttpTransport::create)
		);
	}

	/** Closeable holder so each parameterized scenario can clean up its transport and client. */
	@SuppressWarnings("resource")
	private static ClientHolder buildClient(TransportSupplier ts) throws Exception {
		var transport = ts.get();
		var client = RestClient.builder()
			.transport(transport)
			.rootUrl(FIXTURE.getRootUrl().toString())
			.build();
		return new ClientHolder(client);
	}

	private record ClientHolder(RestClient client) implements AutoCloseable {
		TestApi proxy() { return client.remote(TestApi.class); }
		@Override public void close() throws java.io.IOException { client.close(); }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A. GET with @Path
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{0}] a01_get_pathSingle")
	@MethodSource("transports")
	void a01_get_pathSingle(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals("echo:42", c.proxy().echo("42"));
		}
	}

	@ParameterizedTest(name = "[{0}] a02_get_pathMultiSegment")
	@MethodSource("transports")
	void a02_get_pathMultiSegment(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals("foo/bar", c.proxy().segments("foo", "bar"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. GET with @Query
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{0}] b01_get_query")
	@MethodSource("transports")
	void b01_get_query(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals("q=hello", c.proxy().query("hello"));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. POST with @Content
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{0}] c01_post_stringContent")
	@MethodSource("transports")
	void c01_post_stringContent(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals("body=hello", c.proxy().content("hello"));
		}
	}

	@ParameterizedTest(name = "[{0}] c02_post_httpBodyContent")
	@MethodSource("transports")
	void c02_post_httpBodyContent(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals("body=hello", c.proxy().contentHttpBody(StringBody.of("hello", "text/plain")));
		}
	}

	@ParameterizedTest(name = "[{0}] c03_post_largeBody")
	@MethodSource("transports")
	void c03_post_largeBody(String name, TransportSupplier ts) throws Exception {
		var s = "x".repeat(8192);
		try (var c = buildClient(ts)) {
			assertEquals("len=8192", c.proxy().large(s));
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D. Headers
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{0}] d01_header_parameter")
	@MethodSource("transports")
	void d01_header_parameter(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals("vvv", c.proxy().header("vvv"));
		}
	}

	@ParameterizedTest(name = "[{0}] d02_header_defaultOnClient")
	@MethodSource("transports")
	void d02_header_defaultOnClient(String name, TransportSupplier ts) throws Exception {
		var transport = ts.get();
		try (var client = RestClient.builder()
				.transport(transport)
				.rootUrl(FIXTURE.getRootUrl().toString())
				.header("X-Test", "default-value")
				.build()) {
			assertEquals("default-value", client.remote(TestApi.class).headerDefaultOnly());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E. Status / void returns
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{0}] e01_voidReturn_succeeds")
	@MethodSource("transports")
	void e01_voidReturn_succeeds(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertDoesNotThrow(() -> c.proxy().noContent());
		}
	}

	@ParameterizedTest(name = "[{0}] e02_statusReturn_200")
	@MethodSource("transports")
	void e02_statusReturn_200(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals(200, c.proxy().echoStatus("42"));
		}
	}

	@ParameterizedTest(name = "[{0}] e03_statusReturn_404")
	@MethodSource("transports")
	void e03_statusReturn_404(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			assertEquals(404, c.proxy().missingStatus());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F. Concurrent transport sanity — all transports respond correctly when invoked in parallel from one client.
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest(name = "[{0}] f01_concurrentCalls")
	@MethodSource("transports")
	void f01_concurrentCalls(String name, TransportSupplier ts) throws Exception {
		try (var c = buildClient(ts)) {
			var proxy = c.proxy();
			var pool = Executors.newFixedThreadPool(8);
			try {
				var futures = IntStream.range(0, 32)
					.mapToObj(i -> pool.submit((Callable<String>) () -> proxy.echo("c" + i)))
					.toList();
				for (int i = 0; i < futures.size(); i++)
					assertEquals("echo:c" + i, futures.get(i).get(10, TimeUnit.SECONDS));
			} finally {
				pool.shutdown();
			}
		}
	}
}
