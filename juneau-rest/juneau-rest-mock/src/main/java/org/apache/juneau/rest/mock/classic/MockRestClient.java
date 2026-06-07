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
package org.apache.juneau.rest.mock.classic;

import static java.util.Collections.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.rest.server.util.RestUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.classic.header.ContentType;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.classic.*;
import org.apache.juneau.rest.client.classic.RestRequest;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.logger.*;

import jakarta.servlet.http.*;

/**
 * Mocked {@link RestClient}.
 *
 * <p>
 * 	This class is used for performing serverless unit testing of {@link Rest @Rest}-annotated and {@link Remote @Remote}-annotated classes.
 *
 * <p>
 * 	The class itself extends from {@link RestClient} providing it with the rich feature set of that API and combines
 * 	it with the Apache HttpClient {@link HttpClientConnection} interface for processing requests.
 *  The class converts {@link HttpRequest} objects to instances of {@link MockServletRequest} and {@link MockServletResponse} which are passed directly
 *  to the call handler on the resource class {@link RestContext#execute(Object,HttpServletRequest,HttpServletResponse)}.
 *  In effect, you're fully testing your REST API as if it were running in a live servlet container, yet not
 *  actually having to run in a servlet container.
 *  All aspects of the client and server side code are tested, yet no servlet container is required.  The actual
 *  over-the-wire transmission is the only aspect being bypassed.
 *
 * <p>
 * The following shows a simple example of invoking a PUT method on a simple REST interface and asserting the correct status code and response body:
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> MockTest {
 *
 * 		<jc>// A simple bean with one field.</jc>
 * 		<jk>public static class</jk> MyBean {
 * 			<jk>public int</jk> <jf>foo</jf> = 1;
 * 		}
 *
 * 		<jc>// Our REST resource to test.</jc>
 * 		<jc>// Simply echos the response.</jc>
 * 		<ja>@Rest</ja>(
 * 			serializers=Json5Serializer.<jk>class</jk>,
 * 			parsers=JsonParser.<jk>class</jk>
 * 		)
 * 		<jk>public static class</jk> EchoRest {
 *
 * 			<ja>@RestPut</ja>(
 * 				path=<js>"/echo"</js>
 * 			)
 * 			<jk>public</jk> MyBean echo(<ja>@Content</ja> MyBean <jv>bean</jv>) {
 * 				<jk>return</jk> <jv>bean</jv>;
 * 			}
 * 		}
 *
 * 		<jc>// Our JUnit test.</jc>
 * 		<ja>@Test</ja>
 * 		<jk>public void</jk> testEcho() <jk>throws</jk> Exception {
 *
 * 			MyBean <jv>myBean</jv> = <jk>new</jk> MyBean();
 *
 * 			<jc>// Do a round-trip on the bean through the REST interface</jc>
 * 			<jv>myBean</jv> = MockRestClient
 * 				.<jsm>create</jsm>(EchoRest.<jk>class</jk>)
 * 				.json5()
 * 				.build()
 * 				.put(<js>"/echo"</js>, <jv>myBean</jv>)
 * 				.run()
 * 				.assertStatus().is(200)
 * 				.assertContent().is(<js>"{foo:1}"</js>)
 * 				.getContent().as(MyBean.<jk>class</jk>);
 *
 * 			<jsm>assertEquals</jsm>(1, <jv>myBean</jv>.<jf>foo</jf>);
 * 		}
 * 	}
 * </p>
 * <p>
 * 	Breaking apart the fluent method call above will help you understand how this works.
 *
 * <p class='bjava'>
 * 	<ja>@Test</ja>
 * 	<jk>public void</jk> testEcho() <jk>throws</jk> Exception {
 *
 * 		<jc>// Instantiate our mock client.</jc>
 * 		MockRestClient <jv>client</jv> = MockRestClient
 * 			.<jsm>create</jsm>(EchoRest.<jk>class</jk>)
 * 			.json5()
 * 			.build();
 *
 * 		<jc>// Create a request.</jc>
 * 		RestRequest <jv>req</jv> = <jv>client</jv>.put(<js>"/echo"</js>, <jv>bean</jv>);
 *
 * 		<jc>// Execute it (by calling RestCallHandler.service(...) and then returning the response object).</jc>
 * 		RestResponse <jv>res</jv> = <jv>req</jv>.run();
 *
 * 		<jc>// Run assertion tests on the results.</jc>
 * 		<jv>res</jv>.assertStatus().is(200);
 * 		<jv>res</jv>.assertContent().is(<js>"'foo'"</js>);
 *
 * 		<jc>// Convert the content of the response to a bean.</jc>
 * 		<jv>bean</jv> = <jv>res</jv>.getContent().as(MyBean.<jk>class</jk>);
 * 	}
 * </p>
 *
 * <p>
 * 	The <c>create(Object)</c> method can take in either <c>Class</c> objects or pre-instantiated beans.
 * 	The latter is particularly useful for testing Spring beans.
 *
 * <p>
 * 	The {@link MockRestRequest} object has convenience methods provided to allow you to set any properties
 * 	directly on the underlying {@link HttpServletRequest} object.  The following example shows how
 * 	this can be used to directly set roles on the request object to perform security testing.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(roleGuard=<js>"ADMIN"</js>)
 * 	<jk>public class</jk> A {
 * 		<ja>@RestGet</ja>
 * 		<jk>public</jk> String get() {
 * 			<jk>return</jk> <js>"OK"</js>;
 * 		}
 * 	}
 *
 * 	<ja>@Test</ja>
 * 	<jk>public void</jk> mytest() <jk>throws</jk> Exception {
 * 		MockRestClient <jv>client</jv> = MockRestClient.<jsm>build</jsm>(A.<jk>class</jk>);
 *
 * 		<jc>// Admin user should get 200, but anyone else should get 403-Unauthorized.</jc>
 * 		<jv>client</jv>.get().roles(<js>"ADMIN"</js>).run().assertStatus().is(200);
 * 		<jv>client</jv>.get().roles(<js>"USER"</js>).run().assertStatus().is(403);
 * 	}
 * </p>
 *
 * <p>
 * 	Debug mode is provided that will cause your HTTP requests and responses to be sent to the console:
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	MockRestClient <jv>client</jv> = MockRestClient
 * 		.<jsm>create</jsm>(MyRest.<jk>class</jk>)
 * 		.debug()
 * 		.json5()
 * 		.build();
 * </p>
 *
 * <p>
 * 	The class can also be used for testing of {@link Remote @Remote}-annotated interfaces against {@link Rest @Rest}-annotated resources.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Our remote resource to test.</jc>
 * 	<ja>@Remote</ja>
 * 	<jk>public interface</jk> MyRemoteInterface {
 *
 * 		<ja>@RemoteGet</ja>(<js>"/echoQuery"</js>)
 * 		<jk>public int</jk> echoQuery(<ja>@Query</ja>(name=<js>"id"</js>) <jk>int</jk> <jv>id</jv>);
 * 	}
 *
 * 	<jc>// Our mocked-up REST interface to test against.</jc>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> MyRest {
 *
 * 		<ja>@RestGet</ja>(path=<js>"/echoQuery"</js>)
 * 		<jk>public int</jk> echoQuery(<ja>@Query</ja>(<js>"id"</js>) String <jv>id</jv>) {
 * 			<jk>return</jk> <jv>id</jv>;
 * 		}
 * 	}
 *
 * 	<ja>@Test</ja>
 * 	<jk>public void</jk> testProxy() {
 * 		MyRemoteInterface <jv>client</jv> = MockRestClient
 * 			.create(MyRest.<jk>class</jk>)
 * 			.json()
 * 			.build()
 * 			.getRemote(MyRemoteInterface.<jk>class</jk>);
 *
 * 		<jsm>assertEquals</jsm>(123, <jv>client</jv>.echoQuery(123));
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestMockBasics">juneau-rest-mock Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"resource",  // Builders and requests returned to callers; lifecycle managed by the enclosing MockRestClient or test
	"java:S3740" // Raw Class/Builder types used intentionally for fluent mock client construction where response type is unknown at build time
})
public class MockRestClient extends RestClient implements HttpClientConnection {

	/**
	 * Builder class.
	 */
	public static class Builder extends RestClient.Builder<Builder> implements BeanStoreOverridable<Builder> {

		Object restBean;
		String contextPath;
		String servletPath;
		RestContext restContext;
		Map<String,String> pathVars;
		org.apache.juneau.commons.inject.BeanStore overridingBeanStore;

		/**
		 * No-arg constructor.
		 *
		 * <p>
		 * Provided so that this class can be easily subclassed.
		 */
		protected Builder() {
			connectionManager(new MockHttpClientConnectionManager());
		}

		@Override /* Overridden from Context.Builder<?> */
		public MockRestClient build() {
			return super.build(MockRestClient.class);
		}

		/**
		 * Identifies the context path for the REST resource.
		 *
		 * <p>
		 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
		 * 	object correctly.
		 *
		 * <p>
		 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
		 *
		 * <p>
		 * 	The following fixes are applied to non-conforming strings.
		 * <ul>
		 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
		 * 	<li>Trailing slashes are trimmed.
		 * 	<li>Leading slash is added if needed.
		 * </ul>
		 *
		 * @param value The context path.
		 * @return This object.
		 */
		public Builder contextPath(String value) {
			contextPath = toValidContextPath(value);
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder debug() {
			header("Debug", "true");
			super.debug();
			return this;
		}

		@Override /* Overridden from Builder */
		@SuppressWarnings({
			"unchecked" // Type erasure requires cast for builder chain
		})
		public Builder parsers(java.lang.Class<? extends org.apache.juneau.parser.Parser>...value) {
			super.parsers(value);
			return this;
		}

		/**
		 * Add resolved path variables to this client.
		 *
		 * <p>
		 * Allows you to add resolved parent path variables when performing tests on child resource classes.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// A parent class with a path variable.</jc>
		 * 	<ja>@Rest</ja>(
		 * 		path=<js>"/parent/{foo}"</js>,
		 * 		children={
		 * 			Child.<jk>class</jk>
		 * 		}
		 * 	)
		 * 	<jk>public class</jk> Parent { ... }
		 *
		 * 	<jc>// A child class that uses the parent path variable.</jc>
		 * 	<ja>@Rest</ja>
		 * 	<jk>public class</jk> Child {
		 *
		 * 		<jk>@RestGet</jk>
		 * 		<jk>public</jk> String get(<ja>@Path</ja>(<js>"foo"</js>) String <jv>foo</jv>) {
		 * 			<jk>return</jk> <jv>foo</jv>;
		 * 		}
		 * 	}
		 * </p>
		 * <p class='bjava'>
		 * 	<jc>// Test the method that uses the parent path variable.</jc>
		 * 	MockRestClient
		 * 		.<jsm>create</jsm>(Child.<jk>class</jk>)
		 * 		.json5()
		 * 		.pathVars(<js>"foo"</js>,<js>"bar"</js>)
		 * 		.build()
		 * 		.get(<js>"/"</js>)
		 * 		.run()
		 * 		.assertStatus().asCode().is(200)
		 * 		.assertContent().is(<js>"bar"</js>);
		 * </p>
		 *
		 * <review>Needs review</review>
		 *
		 * @param value The path variables.
		 * @return This object.
		 * @see MockServletRequest#pathVars(Map)
		 */
		public Builder pathVars(Map<String,String> value) {
			pathVars = value;
			return this;
		}

		/**
		 * Add resolved path variables to this client.
		 *
		 * <p>
		 * Identical to {@link #pathVars(Map)} but allows you to specify as a list of key/value pairs.
		 *
		 * @param pairs The key/value pairs.  Must be an even number of parameters.
		 * @return This object.
		 */
		public Builder pathVars(String...pairs) {
			return pathVars(mapb(String.class, String.class).addPairs((Object[])pairs).build());
		}

		/**
		 * Specifies the {@link Rest}-annotated bean class or instance to test against.
		 *
		 * @param value The {@link Rest}-annotated bean class or instance.
		 * @return This object.
		 */
		public Builder restBean(Object value) {
			restBean = value;
			return this;
		}

		/**
		 * Specifies the {@link RestContext} created for the REST bean.
		 *
		 * @param value The {@link RestContext} created for the REST bean.
		 * @return This object.
		 */
		public Builder restContext(RestContext value) {
			restContext = value;
			return this;
		}

		/**
		 * Installs a {@link org.apache.juneau.commons.inject.BeanStore BeanStore} as the
		 * {@code overridingParent} of the {@link RestContext}'s bean store, so test-time overrides
		 * (e.g. {@code TestBeanStore}) resolve at tier 1 of the lookup chain &mdash; above the
		 * resource's local {@code @Bean} factory entries.
		 *
		 * <p>
		 * Setting this disables the per-resource {@link RestContext} cache for the duration of this
		 * build, so the overlay is not silently shared with a previously-cached instance.
		 *
		 * @param value The override layer. Can be <jk>null</jk> to clear a previously-set value.
		 * @return This object.
		 *
		 * @since 10.0.0
		 */
		@Override
		public Builder overridingBeanStore(org.apache.juneau.commons.inject.BeanStore value) {
			overridingBeanStore = value;
			return this;
		}

		@Override /* Overridden from Builder */
		@SuppressWarnings({
			"unchecked" // Type erasure requires cast for builder chain
		})
		public Builder serializers(java.lang.Class<? extends org.apache.juneau.serializer.Serializer>...value) {
			super.serializers(value);
			return this;
		}

		/**
		 * Identifies the servlet path for the REST resource.
		 *
		 * <p>
		 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
		 * 	object correctly.
		 *
		 * <p>
		 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
		 *
		 * <p>
		 * 	The following fixes are applied to non-conforming strings.
		 * <ul>
		 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
		 * 	<li>Trailing slashes are trimmed.
		 * 	<li>Leading slash is added if needed.
		 * </ul>
		 *
		 * @param value The context path.
		 * @return This object.
		 */
		public Builder servletPath(String value) {
			servletPath = toValidContextPath(value);
			return this;
		}

		/**
		 * Suppress logging on this client.
		 *
		 * @return This object.
		 */
		public Builder suppressLogging() {
			return logRequests(DetailLevel.NONE, null, null);
		}


	}

	private static Map<Class<?>,RestContext> restContexts = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link RestClient} with no registered serializer or parser.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bjava'>
	 * 	MockRestClient.<jsm>create</jsm>(<jv>impl</jv>).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient build(Object impl) {
		return create(impl).build();
	}

	/**
	 * Creates a new {@link RestClient} with JSON marshalling support.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bjava'>
	 * 	MockRestClient.<jsm>create</jsm>(<jv>impl</jv>).json().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient buildJson(Object impl) {
		return create(impl).json().build();
	}

	/**
	 * Creates a new {@link RestClient} with Simplified-JSON marshalling support.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bjava'>
	 * 	MockRestClient.<jsm>create</jsm>(<jv>impl</jv>).json().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient buildJson5(Object impl) {
		return create(impl).json5().build();
	}

	/**
	 * Creates a new {@link RestClient} with Simplified-JSON marshalling support.
	 *
	 * <p>
	 * Same as {@link #buildJson5(Object)} but HTTP 400+ codes don't trigger {@link RestCallException RestCallExceptions}.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bjava'>
	 * 	MockRestClient.<jsm>create</jsm>(<jv>impl</jv>).json().ignoreErrors().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient buildJson5Lax(Object impl) {
		return create(impl).json5().ignoreErrors().noTrace().build();
	}

	/**
	 * Creates a new {@link RestClient} with JSON marshalling support.
	 *
	 * <p>
	 * Same as {@link #buildJson(Object)} but HTTP 400+ codes don't trigger {@link RestCallException RestCallExceptions}.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bjava'>
	 * 	MockRestClient.<jsm>create</jsm>(<jv>impl</jv>).json().ignoreErrors().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient buildJsonLax(Object impl) {
		return create(impl).json().ignoreErrors().noTrace().build();
	}

	/**
	 * Creates a new {@link RestClient} with no registered serializer or parser.
	 *
	 * <p>
	 * Same as {@link #build(Object)} but HTTP 400+ codes don't trigger {@link RestCallException RestCallExceptions}.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bjava'>
	 * 	MockRestClient.<jsm>create</jsm>(<jv>impl</jv>).ignoreErrors().noTrace().build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient buildLax(Object impl) {
		return create(impl).ignoreErrors().noTrace().build();
	}

	/**
	 * Creates a new {@link org.apache.juneau.rest.client.classic.RestClient.Builder}configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static Builder create(Object impl) {
		return new Builder().restBean(impl);
	}

	/**
	 * Creates a new {@link org.apache.juneau.rest.client.classic.RestClient.Builder}configured with the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * Same as {@link #create(Object)} but HTTP 400+ codes don't trigger {@link RestCallException RestCallExceptions}.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static Builder createLax(Object impl) {
		return new Builder().restBean(impl).ignoreErrors().noTrace();
	}

	private static Builder preInit(Builder builder) {
		try {
			var restBean = builder.restBean;
			var contextPath = builder.contextPath;
			var servletPath = builder.servletPath;
			var overlay = builder.overridingBeanStore;

			var c = restBean instanceof Class restBean2 ? (Class<?>)restBean2 : restBean.getClass();
			RestContext restBeanCtx;
			if (overlay == null && restContexts.containsKey(c)) {
				restBeanCtx = restContexts.get(c);
			} else {
				var isClass = restBean instanceof Class;
				var o = isClass ? ((Class<?>)restBean).getDeclaredConstructor().newInstance() : restBean;
				restBeanCtx = new RestContext(new RestContext.Args(o.getClass(), null, null, () -> o, "", bs -> {
					bs.addBean(Enablement.class, CONDITIONAL);
					bs.addBeanType(CallLogger.class, BasicTestCallLogger.class);
				}, overlay, null, false)).postInit().postInitChildFirst();
				if (overlay == null)
					restContexts.put(c, restBeanCtx);
			}
			builder.restContext(restBeanCtx);

			if (servletPath == null)
				servletPath = toValidContextPath(restBeanCtx.getFullPath());

			final var suffix = emptyIfNull(contextPath) + emptyIfNull(servletPath);
			final var existingSupplier = builder.getRootUrlSupplier();
			if (existingSupplier != null) {
				// Compose a new supplier that appends the fixed path suffix to whatever the original supplier returns.
				builder.rootUrl(() -> existingSupplier.get() + suffix);
			} else {
				builder.rootUrl("http://localhost" + suffix);
			}

			builder.servletPath = servletPath;
			return builder;
		} catch (Exception e) {
			throw new ConfigException(e, "Could not initialize MockRestClient");
		}
	}

	private final RestContext restContext;
	private final Object restObject;
	private final String contextPath;
	private final String servletPath;

	private final Map<String,String> pathVars;
	private final ThreadLocal<HttpRequest> rreq = new ThreadLocal<>();
	private final ThreadLocal<MockRestResponse> rres = new ThreadLocal<>();
	private final ThreadLocal<MockServletRequest> sreq = new ThreadLocal<>();

	private final ThreadLocal<MockServletResponse> sres = new ThreadLocal<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	public MockRestClient(Builder builder) {
		super(preInit(builder));
		restContext = builder.restContext;
		contextPath = nn(builder.contextPath) ? builder.contextPath : "";
		servletPath = nn(builder.servletPath) ? builder.servletPath : "";
		pathVars = nn(builder.pathVars) ? builder.pathVars : emptyMap();
		restObject = restContext.getResource();

		HttpClientConnectionManager ccm = getHttpClientConnectionManager();
		if (ccm instanceof MockHttpClientConnectionManager ccm2)
			ccm2.init(this);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest callback(String callString) throws RestCallException {
		return (MockRestRequest)super.callback(callString);
	}

	@Override /* Overridden from HttpClientConnection */
	public void close() throws IOException {
		// Don't call super.close() because it will close the client.
		rreq.remove();
		rres.remove();
		sreq.remove();
		sres.remove();
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest delete(Object url) throws RestCallException {
		return (MockRestRequest)super.delete(url);
	}

	@Override /* Overridden from HttpClientConnection */
	public void flush() throws IOException {
		// No-op: Mock implementation - full functionality not required
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest formPost(Object url) throws RestCallException {
		return (MockRestRequest)super.formPost(url);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest formPost(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.formPost(url, body);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest formPostPairs(Object url, String...parameters) throws RestCallException {
		return (MockRestRequest)super.formPostPairs(url, parameters);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest get() throws RestCallException {
		return (MockRestRequest)super.get();
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest get(Object url) throws RestCallException {
		return (MockRestRequest)super.get(url);
	}

	/**
	 * Returns the current client-side REST request.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current client-side REST request, or <jk>null</jk> if not set.
	 */
	public HttpRequest getCurrentClientRequest() { return rreq.get(); }

	/**
	 * Returns the current client-side REST response.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current client-side REST response, or <jk>null</jk> if not set.
	 */
	public MockRestResponse getCurrentClientResponse() { return rres.get(); }

	/**
	 * Returns the current server-side REST request.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current server-side REST request, or <jk>null</jk> if not set.
	 */
	public MockServletRequest getCurrentServerRequest() { return sreq.get(); }

	/**
	 * Returns the current server-side REST response.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current server-side REST response, or <jk>null</jk> if not set.
	 */
	public MockServletResponse getCurrentServerResponse() { return sres.get(); }

	@Override /* Overridden from HttpClientConnection */
	public HttpConnectionMetrics getMetrics() { return null; }

	@Override /* Overridden from HttpClientConnection */
	public int getSocketTimeout() { return Integer.MAX_VALUE; }

	@Override /* Overridden from RestClient */
	public MockRestRequest head(Object url) throws RestCallException {
		return (MockRestRequest)super.head(url);
	}

	@Override /* Overridden from HttpClientConnection */
	public boolean isOpen() { return true; }

	@Override /* Overridden from HttpClientConnection */
	public boolean isResponseAvailable(int timeout) throws IOException {
		return true;
	}

	@Override /* Overridden from HttpClientConnection */
	public boolean isStale() { return false; }

	@Override /* Overridden from RestClient */
	public MockRestRequest options(Object url) throws RestCallException {
		return (MockRestRequest)super.options(url);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest patch(Object url) throws RestCallException {
		return (MockRestRequest)super.patch(url);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest patch(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.patch(url, body);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest patch(Object url, String body, ContentType contentType) throws RestCallException {
		return (MockRestRequest)super.patch(url, body, contentType);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest post(Object url) throws RestCallException {
		return (MockRestRequest)super.post(url);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest post(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.post(url, body);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest post(Object url, String body, ContentType contentType) throws RestCallException {
		return (MockRestRequest)super.post(url, body, contentType);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest put(Object url) throws RestCallException {
		return (MockRestRequest)super.put(url);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest put(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.put(url, body);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest put(Object url, String body, ContentType contentType) throws RestCallException {
		return (MockRestRequest)super.put(url, body, contentType);
	}

	@Override /* Overridden from HttpClientConnection */
	public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
		InputStream is = new ByteArrayInputStream(sres.get().getContent());
		var contentEncoding = response.getLastHeader("Content-Encoding");
		if (nn(contentEncoding) && contentEncoding.getValue().equalsIgnoreCase("gzip"))
			is = new GZIPInputStream(is);
		response.setEntity(new InputStreamEntity(is));
	}

	@Override /* Overridden from HttpClientConnection */
	public HttpResponse receiveResponseHeader() throws HttpException, IOException {
		try {
			var res = MockServletResponse.create();
			restContext.execute(restObject, sreq.get(), res);

			// If the status isn't set, something's broken.
			if (res.getStatus() == 0)
				throw new IllegalStateException("Response status was 0.");

			// A bug in HttpClient causes an infinite loop if the response is less than 200.
			// As a workaround, just add 1000 to the status code (which is better than an infinite loop).
			if (res.getStatus() < 200)
				res.setStatus(1000 + res.getStatus());

			sres.set(res);

			var response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, res.getStatus(), res.getMessage()));
			res.getHeaders().forEach((k, v) -> {
				for (var hv : v)
					response.addHeader(k, hv);
			});

			return response;
		} catch (Exception e) {
			throw new HttpException(emptyIfNull(e.getMessage()), e);
		}
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest request(RestOperation op) throws RestCallException {
		return (MockRestRequest)super.request(op);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest request(String method, Object url) throws RestCallException {
		return (MockRestRequest)super.request(method, url);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest request(String method, Object url, boolean hasBody) throws RestCallException {
		return (MockRestRequest)super.request(method, url, hasBody);
	}

	@Override /* Overridden from RestClient */
	public MockRestRequest request(String method, Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.request(method, url, body);
	}

	@Override /* Overridden from HttpClientConnection */
	public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
		byte[] body = {};
		var entity = request.getEntity();
		if (nn(entity)) {
			var length = entity.getContentLength();
			if (length < 0)
				length = 1024;
			var baos = new ByteArrayOutputStream((int)Math.min(length, 1024));
			entity.writeTo(baos);
			baos.flush();
			body = baos.toByteArray();
		}
		sreq.get().content(body);
	}

	@Override /* Overridden from HttpClientConnection */
	public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
		try {
			var rl = request.getRequestLine();
			var path = rl.getUri();
			var target = findTarget(request);

			var req = findRestRequest(request);
			rreq.set(req);
			rres.remove();
			sreq.remove();
			sres.remove();

			path = target + path;

			var pr = new MockPathResolver(target, contextPath, servletPath, path, null);
			if (nn(pr.getError()))
				throw new IllegalStateException(pr.getError());

			var r = MockServletRequest.create(request.getRequestLine().getMethod(), pr.getURI()).contextPath(pr.getContextPath()).servletPath(pr.getServletPath()).pathVars(pathVars).debug(isDebug());

			for (var h : request.getAllHeaders())
				r.header(h.getName(), h.getValue());

			sreq.set(r);
			sreq.get().applyOverrides(req);
		} catch (Exception e) {
			throw new HttpException(e.getMessage(), e);
		}
	}

	@Override /* Overridden from HttpClientConnection */
	public void setSocketTimeout(int timeout) {
		// No-op: Mock implementation - full functionality not required
	}

	@Override /* Overridden from HttpClientConnection */
	public void shutdown() throws IOException {
		// No-op: Mock implementation - full functionality not required
	}

	/**
	 * Attempts to unwrap the request to find the underlying RestRequest object.
	 * Returns the same object if one of the low-level client methods are used (e.g. execute(HttpUriRequest)).
	 */
	private HttpRequest findRestRequest(HttpRequest req) {
		if (req instanceof RestRequestCreated req2)
			return req2.getRestRequest();
		if (req instanceof HttpRequestWrapper req3)
			return findRestRequest(req3.getOriginal());
		return req;
	}

	private static String findTarget(HttpRequest req) {
		if (req instanceof HttpRequestWrapper req2) {
			var httpHost = req2.getTarget();
			if (nn(httpHost))
				return httpHost.toURI();
		}
		return "http://localhost";
	}

	@Override /* Overridden from RestClient */
	protected MockRestRequest createRequest(URI uri, String method, boolean hasBody) throws RestCallException {
		return new MockRestRequest(this, uri, method, hasBody);
	}

	@Override /* Overridden from RestClient */
	protected MockRestResponse createResponse(RestRequest req, HttpResponse httpResponse, Parser parser) throws RestCallException {
		return new MockRestResponse(this, req, httpResponse, parser);
	}

	MockRestClient currentResponse(MockRestResponse value) {
		rres.set(value);
		return this;
	}
}