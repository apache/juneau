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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.marshall.Enablement.*;
import static org.apache.juneau.rest.server.util.RestUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.RestRequest;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.logger.*;

import jakarta.servlet.*;

/**
 * Next-generation mock REST client that routes requests in-process to a Juneau {@link RestContext}.
 *
 * <p>
 * Use this class to write serverless unit tests for {@link org.apache.juneau.rest.server.Rest @Rest}-annotated
 * resource classes.  Requests are dispatched directly to the {@link RestContext} without any network activity,
 * making tests fast and self-contained.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyResource {
 * 		<ja>@RestGet</ja>(<js>"/hello"</js>)
 * 		<jk>public</jk> String hello() { <jk>return</jk> <js>"Hello!"</js>; }
 * 	}
 *
 * 	<ja>@Test</ja>
 * 	<jk>void</jk> test() <jk>throws</jk> Exception {
 * 		<jk>try</jk> (<jv>client</jv> = MockRestClient.<jsm>create</jsm>(MyResource.<jk>class</jk>)) {
 * 			var <jv>body</jv> = <jv>client</jv>.get(<js>"/hello"</js>).run().asString();
 * 			assertEquals(<js>"\"Hello!\""</js>, <jv>body</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.marshall.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/NextGenRestClient">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
@SuppressWarnings({
	"resource" // RestClient is closed by MockRestClient.close(); transport is owned by the client
})
public final class MockRestClient implements Closeable {

	private static final Map<Class<?>, RestContext> restContextCache = new ConcurrentHashMap<>();

	private final RestClient client;

	private MockRestClient(RestClient client) {
		this.client = client;
	}

	/**
	 * Creates an {@link MockRestClient} backed by the given REST bean or bean class.
	 *
	 * <p>
	 * {@link RestContext} instances are cached per class for performance.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link org.apache.juneau.rest.server.Rest @Rest}.
	 * 	If a {@link Class}, it must have a no-arg constructor.
	 * @return A new client. Never <jk>null</jk>.
	 */
	public static MockRestClient create(Object impl) {
		return builder(impl).build();
	}

	/**
	 * Returns a new {@link Builder} for the given REST bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link org.apache.juneau.rest.server.Rest @Rest}.
	 * 	If a {@link Class}, it must have a no-arg constructor.
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder builder(Object impl) {
		return new Builder(impl);
	}

	/**
	 * Creates a GET request to the given path.
	 *
	 * @param path The request path (relative to the resource's servlet path). Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest get(String path) { return client.get(path); }

	/**
	 * Creates a POST request to the given path.
	 *
	 * @param path The request path. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest post(String path) { return client.post(path); }

	/**
	 * Creates a PUT request to the given path.
	 *
	 * @param path The request path. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest put(String path) { return client.put(path); }

	/**
	 * Creates a PATCH request to the given path.
	 *
	 * @param path The request path. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest patch(String path) { return client.patch(path); }

	/**
	 * Creates a DELETE request to the given path.
	 *
	 * @param path The request path. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest delete(String path) { return client.delete(path); }

	/**
	 * Creates a HEAD request to the given path.
	 *
	 * @param path The request path. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest head(String path) { return client.head(path); }

	/**
	 * Creates a request with the given HTTP method and path.
	 *
	 * @param method The HTTP method. Must not be <jk>null</jk>.
	 * @param path The request path. Must not be <jk>null</jk>.
	 * @return A new {@link RestRequest}. Never <jk>null</jk>.
	 */
	public RestRequest request(String method, String path) { return client.request(method, path); }

	/**
	 * Returns the underlying {@link RestClient}.
	 *
	 * @return The client. Never <jk>null</jk>.
	 */
	public RestClient getClient() { return client; }

	@Override /* Closeable */
	public void close() throws IOException {
		client.close();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Inner types
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link MockRestClient}.
	 *
	 * @since 9.2.1
	 */
	public static final class Builder implements BeanStoreOverridable<Builder> {

		private final Object impl;
		private String contextPath;
		private BeanStore overridingBeanStore;

		private Builder(Object impl) {
			this.impl = impl;
		}

		/**
		 * Sets the servlet context path (e.g. {@code "/myapp"}).
		 *
		 * @param value The context path. May be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder contextPath(String value) {
			contextPath = toValidContextPath(value);
			return this;
		}

		/**
		 * Installs a {@link BeanStore} as the {@code overridingParent} of the {@link RestContext}'s
		 * bean store, so test-time overrides (e.g. {@code TestBeanStore}) resolve at tier 1 of the
		 * lookup chain &mdash; above the resource's local {@code @Bean} factory entries.
		 *
		 * <p>
		 * Setting this disables the per-resource {@link RestContext} cache for the duration of this
		 * build, so the overlay is not silently shared with a previously-cached instance.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>var</jk> <jv>overlay</jv> = <jk>new</jk> TestBeanStore().override(MyService.<jk>class</jk>, mockSvc);
		 * 	<jk>try</jk> (<jv>client</jv> = MockRestClient.<jsm>builder</jsm>(MyResource.<jk>class</jk>)
		 * 			.overridingBeanStore(<jv>overlay</jv>)
		 * 			.build()) {
		 * 		<jv>client</jv>.get(<js>"/widgets/1"</js>).run().assertStatus().is(200);
		 * 	}
		 * </p>
		 *
		 * @param value The override layer. Can be <jk>null</jk> to clear a previously-set value.
		 * @return This object.
		 *
		 * @since 10.0.0
		 */
		@Override
		public Builder overridingBeanStore(BeanStore value) {
			overridingBeanStore = value;
			return this;
		}

		/**
		 * Builds and returns the {@link MockRestClient}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		@SuppressWarnings({
			"java:S112" // Re-wrap as RuntimeException - build() wraps arbitrary checked exceptions from reflection (newInstance) and RestContext init/postInit; no specific type fits and the fluent build() method cannot declare checked exceptions.
		})
		public MockRestClient build() {
			try {
				var c = impl instanceof Class<?> c2 ? c2 : impl.getClass();
				var overlay = overridingBeanStore;
				RestContext restContext;
				if (overlay == null && restContextCache.containsKey(c)) {
					restContext = restContextCache.get(c);
				} else {
					var isClass = impl instanceof Class<?>;
					var o = isClass ? ((Class<?>)impl).getDeclaredConstructor().newInstance() : impl;
					restContext = new RestContext(new RestContext.Args(o.getClass(), null, null, () -> o, "", bs -> {
						bs.addBean(Enablement.class, CONDITIONAL);
						bs.addBeanType(CallLogger.class, BasicTestCallLogger.class);
					}, overlay, null, false)).postInit().postInitChildFirst();
					if (overlay == null)
						restContextCache.put(c, restContext);
				}

				var servletPath = toValidContextPath(restContext.getFullPath());
				var fullContextPath = emptyIfNull(contextPath) + emptyIfNull(servletPath);

				var transport = new JuneauRestTransport(restContext, servletPath);
				var ngClient = RestClient.builder()
					.transport(transport)
					.rootUrl("http://localhost" + fullContextPath)
					.build();

				return new MockRestClient(ngClient);
			} catch (Exception e) {
				throw new RuntimeException("Could not initialize MockRestClient", e);
			}
		}

		private static String emptyIfNull(String s) { return s == null ? "" : s; }
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Transport implementation
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * An {@link HttpTransport} that dispatches requests in-process through a Juneau {@link RestContext}.
	 */
	private static final class JuneauRestTransport implements HttpTransport {

		private final RestContext restContext;
		private final Object restObject;
		private final String servletPath;

		JuneauRestTransport(RestContext restContext, String servletPath) {
			this.restContext = restContext;
			this.restObject = restContext.getResource();
			this.servletPath = servletPath;
		}

		@Override /* HttpTransport */
		public TransportResponse execute(TransportRequest request) throws TransportException {
			try {
				var method = request.getMethod();
				var uri = request.getUri().toString();

				var servletReq = MockServletRequest.create(method, uri)
					.servletPath(servletPath)
					.contextPath("");

				for (var h : request.getHeaders())
					servletReq.header(h.name(), h.value());

				var body = request.getBody();
				if (body != null) {
					var baos = new ByteArrayOutputStream();
					body.writeTo(baos);
					servletReq.content(baos.toByteArray());
				}

				var servletRes = MockServletResponse.create();

				restContext.execute(restObject, servletReq, servletRes);

				if (servletRes.getStatus() == 0) // HTT: would require a buggy RestContext that never sets a status
					throw new TransportException("REST context returned status 0 — response was never committed");

				var responseBuilder = TransportResponse.builder()
					.statusCode(servletRes.getStatus())
					.reasonPhrase(servletRes.getMessage());

				for (var name : servletRes.getHeaderNames())
					for (var value : servletRes.getHeaders(name))
						responseBuilder.header(name, value);

				var content = servletRes.getContent();
				if (content.length > 0)
					responseBuilder.body(new ByteArrayInputStream(content));

				return responseBuilder.build();

			} catch (TransportException e) {
				throw e;
			} catch (ServletException | IOException e) {
				throw new TransportException("In-process dispatch failed: " + e.getMessage(), e);
			}
		}
	}
}
