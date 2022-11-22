// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.mock;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static java.util.Collections.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;
import java.util.zip.*;

import javax.net.ssl.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.RestRequest;
import org.apache.juneau.rest.client.RestResponse;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.http.header.ContentType;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;

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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
public class MockRestClient extends RestClient implements HttpClientConnection {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static Map<Class<?>,RestContext> REST_CONTEXTS = new ConcurrentHashMap<>();

	/**
	 * Creates a new {@link org.apache.juneau.rest.client.RestClient.Builder} configured with the specified REST implementation bean or bean class.
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
	 * Creates a new {@link org.apache.juneau.rest.client.RestClient.Builder} configured with the specified REST implementation bean or bean class.
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

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters(ignore="debug")
	public static class Builder extends RestClient.Builder {

		Object restBean;
		String contextPath, servletPath;
		RestContext restContext;
		Map<String,String> pathVars;

		/**
		 * No-arg constructor.
		 *
		 * <p>
		 * Provided so that this class can be easily subclassed.
		 */
		protected Builder() {
			super();
			connectionManager(new MockHttpClientConnectionManager());
		}

		@Override /* Context.Builder */
		public Builder copy() {
			throw new NoSuchMethodError("Not implemented.");
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
			return pathVars(mapBuilder(String.class,String.class).addPairs((Object[])pairs).build());
		}

		/**
		 * Suppress logging on this client.
		 *
		 * @return This object.
		 */
		public Builder suppressLogging() {
			return logRequests(DetailLevel.NONE, null, null);
		}

		@Override /* Context.Builder */
		public Builder debug() {
			header("Debug", "true");
			super.debug();
			return this;
		}

		@Override /* Context.Builder */
		public MockRestClient build() {
			return build(MockRestClient.class);
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanClassVisibility(Visibility value) {
			super.beanClassVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanConstructorVisibility(Visibility value) {
			super.beanConstructorVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanContext(BeanContext.Builder value) {
			super.beanContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanDictionary(java.lang.Class<?>...values) {
			super.beanDictionary(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanFieldVisibility(Visibility value) {
			super.beanFieldVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanInterceptor(Class<?> on, Class<? extends org.apache.juneau.swap.BeanInterceptor<?>> value) {
			super.beanInterceptor(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMapPutReturnsOldValue() {
			super.beanMapPutReturnsOldValue();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanMethodVisibility(Visibility value) {
			super.beanMethodVisibility(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Map<String,Object> values) {
			super.beanProperties(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(Class<?> beanClass, String properties) {
			super.beanProperties(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanProperties(String beanClassName, String properties) {
			super.beanProperties(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Map<String,Object> values) {
			super.beanPropertiesExcludes(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(Class<?> beanClass, String properties) {
			super.beanPropertiesExcludes(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesExcludes(String beanClassName, String properties) {
			super.beanPropertiesExcludes(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Map<String,Object> values) {
			super.beanPropertiesReadOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesReadOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesReadOnly(String beanClassName, String properties) {
			super.beanPropertiesReadOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Map<String,Object> values) {
			super.beanPropertiesWriteOnly(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(Class<?> beanClass, String properties) {
			super.beanPropertiesWriteOnly(beanClass, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beanPropertiesWriteOnly(String beanClassName, String properties) {
			super.beanPropertiesWriteOnly(beanClassName, properties);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireDefaultConstructor() {
			super.beansRequireDefaultConstructor();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSerializable() {
			super.beansRequireSerializable();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder beansRequireSettersForGetters() {
			super.beansRequireSettersForGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder dictionaryOn(Class<?> on, java.lang.Class<?>...values) {
			super.dictionaryOn(on, values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableBeansRequireSomeProperties() {
			super.disableBeansRequireSomeProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreMissingSetters() {
			super.disableIgnoreMissingSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreTransientFields() {
			super.disableIgnoreTransientFields();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableIgnoreUnknownNullBeanProperties() {
			super.disableIgnoreUnknownNullBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder disableInterfaceProxies() {
			super.disableInterfaceProxies();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, T o) {
			super.example(pojoClass, o);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T> Builder example(Class<T> pojoClass, String json) {
			super.example(pojoClass, json);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters() {
			super.findFluentSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder findFluentSetters(Class<?> on) {
			super.findFluentSetters(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnGetters() {
			super.ignoreInvocationExceptionsOnGetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreInvocationExceptionsOnSetters() {
			super.ignoreInvocationExceptionsOnSetters();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownBeanProperties() {
			super.ignoreUnknownBeanProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder ignoreUnknownEnumValues() {
			super.ignoreUnknownEnumValues();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClass(Class<?> interfaceClass, Class<?> implClass) {
			super.implClass(interfaceClass, implClass);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder implClasses(Map<Class<?>,Class<?>> values) {
			super.implClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaceClass(Class<?> on, Class<?> value) {
			super.interfaceClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder interfaces(java.lang.Class<?>...value) {
			super.interfaces(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder locale(Locale value) {
			super.locale(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder mediaType(MediaType value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanClasses(java.lang.Class<?>...values) {
			super.notBeanClasses(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder notBeanPackages(String...values) {
			super.notBeanPackages(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder propertyNamer(Class<?> on, Class<? extends org.apache.juneau.PropertyNamer> value) {
			super.propertyNamer(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties() {
			super.sortProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder sortProperties(java.lang.Class<?>...on) {
			super.sortProperties(on);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder stopClass(Class<?> on, Class<?> value) {
			super.stopClass(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction) {
			super.swap(normalClass, swappedClass, swapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public <T, S> Builder swap(Class<T> normalClass, Class<S> swappedClass, ThrowingFunction<T,S> swapFunction, ThrowingFunction<S,T> unswapFunction) {
			super.swap(normalClass, swappedClass, swapFunction, unswapFunction);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder swaps(java.lang.Class<?>...values) {
			super.swaps(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder timeZone(TimeZone value) {
			super.timeZone(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typeName(Class<?> on, String value) {
			super.typeName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(String value) {
			super.typePropertyName(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder typePropertyName(Class<?> on, String value) {
			super.typePropertyName(on, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useEnumNames() {
			super.useEnumNames();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanContextable.Builder */
		public Builder useJavaBeanIntrospector() {
			super.useJavaBeanIntrospector();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder acceptCharset(String value) {
			super.acceptCharset(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder addBeanTypes() {
			super.addBeanTypes();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder addInterceptorFirst(HttpRequestInterceptor itcp) {
			super.addInterceptorFirst(itcp);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder addInterceptorFirst(HttpResponseInterceptor itcp) {
			super.addInterceptorFirst(itcp);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder addInterceptorLast(HttpRequestInterceptor itcp) {
			super.addInterceptorLast(itcp);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder addInterceptorLast(HttpResponseInterceptor itcp) {
			super.addInterceptorLast(itcp);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder addRootType() {
			super.addRootType();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder backoffManager(BackoffManager backoffManager) {
			super.backoffManager(backoffManager);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder basicAuth(String host, int port, String user, String pw) {
			super.basicAuth(host, port, user, pw);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder callHandler(Class<? extends org.apache.juneau.rest.client.RestCallHandler> value) {
			super.callHandler(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder clientVersion(String value) {
			super.clientVersion(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder connectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
			super.connectionBackoffStrategy(connectionBackoffStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder connectionManager(HttpClientConnectionManager value) {
			super.connectionManager(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder connectionManagerShared(boolean shared) {
			super.connectionManagerShared(shared);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder connectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
			super.connectionReuseStrategy(reuseStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder connectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
			super.connectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder console(PrintStream value) {
			super.console(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder contentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
			super.contentDecoderRegistry(contentDecoderMap);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder contentType(String value) {
			super.contentType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder debugOutputLines(int value) {
			super.debugOutputLines(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
			super.defaultAuthSchemeRegistry(authSchemeRegistry);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultConnectionConfig(ConnectionConfig config) {
			super.defaultConnectionConfig(config);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
			super.defaultCookieSpecRegistry(cookieSpecRegistry);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultCookieStore(CookieStore cookieStore) {
			super.defaultCookieStore(cookieStore);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultCredentialsProvider(CredentialsProvider credentialsProvider) {
			super.defaultCredentialsProvider(credentialsProvider);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultRequestConfig(RequestConfig config) {
			super.defaultRequestConfig(config);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder defaultSocketConfig(SocketConfig config) {
			super.defaultSocketConfig(config);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder detectLeaks() {
			super.detectLeaks();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder detectRecursions() {
			super.detectRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder disableAuthCaching() {
			super.disableAuthCaching();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder disableAutomaticRetries() {
			super.disableAutomaticRetries();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder disableConnectionState() {
			super.disableConnectionState();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder disableContentCompression() {
			super.disableContentCompression();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder disableCookieManagement() {
			super.disableCookieManagement();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder disableRedirectHandling() {
			super.disableRedirectHandling();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder errorCodes(Predicate<Integer> value) {
			super.errorCodes(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder evictExpiredConnections() {
			super.evictExpiredConnections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
			super.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder executorService(ExecutorService executorService, boolean shutdownOnClose) {
			super.executorService(executorService, shutdownOnClose);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder formData(NameValuePair...parts) {
			super.formData(parts);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder formData(String name, String value) {
			super.formData(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder formData(String name, Supplier<String> value) {
			super.formData(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder header(String name, String value) {
			super.header(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder header(String name, Supplier<String> value) {
			super.header(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder headers(Header...parts) {
			super.headers(parts);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder html() {
			super.html();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder htmlDoc() {
			super.htmlDoc();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder htmlStrippedDoc() {
			super.htmlStrippedDoc();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder httpClient(CloseableHttpClient value) {
			super.httpClient(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder httpClientBuilder(HttpClientBuilder value) {
			super.httpClientBuilder(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder httpProcessor(HttpProcessor httpprocessor) {
			super.httpProcessor(httpprocessor);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder ignoreErrors() {
			super.ignoreErrors();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder ignoreRecursions() {
			super.ignoreRecursions();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder initialDepth(int value) {
			super.initialDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder interceptors(java.lang.Class<?>...values) throws Exception{
			super.interceptors(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder interceptors(Object...value) {
			super.interceptors(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder json() {
			super.json();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
			super.keepAliveStrategy(keepAliveStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder keepHttpClientOpen() {
			super.keepHttpClientOpen();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder keepNullProperties() {
			super.keepNullProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder logRequests(DetailLevel detail, Level level, BiPredicate<RestRequest,RestResponse> test) {
			super.logRequests(detail, level, test);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder logToConsole() {
			super.logToConsole();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder logger(Logger value) {
			super.logger(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder marshaller(Marshaller value) {
			super.marshaller(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder marshallers(Marshaller...value) {
			super.marshallers(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder maxConnPerRoute(int maxConnPerRoute) {
			super.maxConnPerRoute(maxConnPerRoute);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder maxConnTotal(int maxConnTotal) {
			super.maxConnTotal(maxConnTotal);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder maxDepth(int value) {
			super.maxDepth(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder maxIndent(int value) {
			super.maxIndent(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder mediaType(String value) {
			super.mediaType(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder msgPack() {
			super.msgPack();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder noTrace() {
			super.noTrace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder oapiCollectionFormat(HttpPartCollectionFormat value) {
			super.oapiCollectionFormat(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder oapiFormat(HttpPartFormat value) {
			super.oapiFormat(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder openApi() {
			super.openApi();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder paramFormat(ParamFormat value) {
			super.paramFormat(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder paramFormatPlain() {
			super.paramFormatPlain();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder parser(Class<? extends org.apache.juneau.parser.Parser> value) {
			super.parser(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder parser(Parser value) {
			super.parser(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		@SuppressWarnings("unchecked")
		public Builder parsers(java.lang.Class<? extends org.apache.juneau.parser.Parser>...value) {
			super.parsers(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder parsers(Parser...value) {
			super.parsers(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder partParser(Class<? extends org.apache.juneau.httppart.HttpPartParser> value) {
			super.partParser(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder partParser(HttpPartParser value) {
			super.partParser(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder partSerializer(Class<? extends org.apache.juneau.httppart.HttpPartSerializer> value) {
			super.partSerializer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder partSerializer(HttpPartSerializer value) {
			super.partSerializer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder pathData(NameValuePair...parts) {
			super.pathData(parts);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder pathData(String name, String value) {
			super.pathData(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder pathData(String name, Supplier<String> value) {
			super.pathData(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder plainText() {
			super.plainText();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder pooled() {
			super.pooled();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder proxy(HttpHost proxy) {
			super.proxy(proxy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder proxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
			super.proxyAuthenticationStrategy(proxyAuthStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder publicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
			super.publicSuffixMatcher(publicSuffixMatcher);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder queryData(NameValuePair...parts) {
			super.queryData(parts);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder queryData(String name, String value) {
			super.queryData(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder queryData(String name, Supplier<String> value) {
			super.queryData(name, value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder quoteChar(char value) {
			super.quoteChar(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder redirectStrategy(RedirectStrategy redirectStrategy) {
			super.redirectStrategy(redirectStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder requestExecutor(HttpRequestExecutor requestExec) {
			super.requestExecutor(requestExec);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder retryHandler(HttpRequestRetryHandler retryHandler) {
			super.retryHandler(retryHandler);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder rootUrl(Object value) {
			super.rootUrl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder routePlanner(HttpRoutePlanner routePlanner) {
			super.routePlanner(routePlanner);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder schemePortResolver(SchemePortResolver schemePortResolver) {
			super.schemePortResolver(schemePortResolver);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder serializer(Class<? extends org.apache.juneau.serializer.Serializer> value) {
			super.serializer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder serializer(Serializer value) {
			super.serializer(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		@SuppressWarnings("unchecked")
		public Builder serializers(java.lang.Class<? extends org.apache.juneau.serializer.Serializer>...value) {
			super.serializers(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder serializers(Serializer...value) {
			super.serializers(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
			super.serviceUnavailableRetryStrategy(serviceUnavailStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder json5() {
			super.json5();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder skipEmptyFormData() {
			super.skipEmptyFormData();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder skipEmptyFormData(boolean value) {
			super.skipEmptyFormData(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder skipEmptyHeaderData() {
			super.skipEmptyHeaderData();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder skipEmptyHeaderData(boolean value) {
			super.skipEmptyHeaderData(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder skipEmptyQueryData() {
			super.skipEmptyQueryData();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder skipEmptyQueryData(boolean value) {
			super.skipEmptyQueryData(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder sortCollections() {
			super.sortCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder sortMaps() {
			super.sortMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder sq() {
			super.sq();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder sslContext(SSLContext sslContext) {
			super.sslContext(sslContext);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
			super.sslHostnameVerifier(hostnameVerifier);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder sslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
			super.sslSocketFactory(sslSocketFactory);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder strict() {
			super.strict();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder targetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
			super.targetAuthenticationStrategy(targetAuthStrategy);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder trimEmptyCollections() {
			super.trimEmptyCollections();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder trimEmptyMaps() {
			super.trimEmptyMaps();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder trimStringsOnRead() {
			super.trimStringsOnRead();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder trimStringsOnWrite() {
			super.trimStringsOnWrite();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder uon() {
			super.uon();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder uriContext(UriContext value) {
			super.uriContext(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder uriRelativity(UriRelativity value) {
			super.uriRelativity(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder uriResolution(UriResolution value) {
			super.uriResolution(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder urlEnc() {
			super.urlEnc();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder useSystemProperties() {
			super.useSystemProperties();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder userTokenHandler(UserTokenHandler userTokenHandler) {
			super.userTokenHandler(userTokenHandler);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder ws() {
			super.ws();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.rest.client.RestClient.Builder */
		public Builder xml() {
			super.xml();
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final RestContext restContext;
	private final Object restObject;
	private final String contextPath, servletPath;
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
		contextPath = builder.contextPath != null ? builder.contextPath : "";
		servletPath = builder.servletPath != null ? builder.servletPath : "";
		pathVars = builder.pathVars != null ? builder.pathVars : emptyMap();
		restObject = restContext.getResource();

		HttpClientConnectionManager ccm = getHttpClientConnectionManager();
		if (ccm instanceof MockHttpClientConnectionManager)
			((MockHttpClientConnectionManager)ccm).init(this);
	}

	private static Builder preInit(Builder builder) {
		try {
			Object restBean = builder.restBean;
			String contextPath = builder.contextPath;
			String servletPath = builder.servletPath;
			String rootUrl = builder.getRootUri();
			if (rootUrl == null)
				rootUrl = "http://localhost";

			Class<?> c = restBean instanceof Class ? (Class<?>)restBean : restBean.getClass();
			if (! REST_CONTEXTS.containsKey(c)) {
				boolean isClass = restBean instanceof Class;
				Object o = isClass ? ((Class<?>)restBean).getDeclaredConstructor().newInstance() : restBean;
				RestContext rc = RestContext
					.create(o.getClass(), null, null)
					.defaultClasses(BasicTestCallLogger.class)
					.debugDefault(CONDITIONAL)
					.init(()->o)
					.build()
					.postInit()
					.postInitChildFirst();
				REST_CONTEXTS.put(c, rc);
			}
			RestContext restBeanCtx = REST_CONTEXTS.get(c);
			builder.restContext(restBeanCtx);

			if (servletPath == null)
				servletPath = toValidContextPath(restBeanCtx.getFullPath());

			rootUrl = rootUrl + emptyIfNull(contextPath) + emptyIfNull(servletPath);

			builder.servletPath = servletPath;
			builder.rootUrl(rootUrl);
			return builder;
		} catch (Exception e) {
			throw new ConfigException(e, "Could not initialize MockRestClient");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Entry point methods.
	//------------------------------------------------------------------------------------------------------------------

	@Override /* RestClient */
	public MockRestRequest request(RestOperation op) throws RestCallException {
		return (MockRestRequest)super.request(op);
	}

	@Override /* RestClient */
	public MockRestRequest get(Object url) throws RestCallException {
		return (MockRestRequest)super.get(url);
	}

	@Override /* RestClient */
	public MockRestRequest get() throws RestCallException {
		return (MockRestRequest)super.get();
	}

	@Override /* RestClient */
	public MockRestRequest put(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.put(url, body);
	}

	@Override /* RestClient */
	public MockRestRequest put(Object url, String body, ContentType contentType) throws RestCallException {
		return (MockRestRequest)super.put(url, body, contentType);
	}

	@Override /* RestClient */
	public MockRestRequest put(Object url) throws RestCallException {
		return (MockRestRequest)super.put(url);
	}

	@Override /* RestClient */
	public MockRestRequest post(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.post(url, body);
	}

	@Override /* RestClient */
	public MockRestRequest post(Object url, String body, ContentType contentType) throws RestCallException {
		return (MockRestRequest)super.post(url, body, contentType);
	}

	@Override /* RestClient */
	public MockRestRequest post(Object url) throws RestCallException {
		return (MockRestRequest)super.post(url);
	}

	@Override /* RestClient */
	public MockRestRequest delete(Object url) throws RestCallException {
		return (MockRestRequest)super.delete(url);
	}

	@Override /* RestClient */
	public MockRestRequest options(Object url) throws RestCallException {
		return (MockRestRequest)super.options(url);
	}

	@Override /* RestClient */
	public MockRestRequest head(Object url) throws RestCallException {
		return (MockRestRequest)super.head(url);
	}

	@Override /* RestClient */
	public MockRestRequest formPost(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.formPost(url, body);
	}

	@Override /* RestClient */
	public MockRestRequest formPost(Object url) throws RestCallException {
		return (MockRestRequest)super.formPost(url);
	}

	@Override /* RestClient */
	public MockRestRequest formPostPairs(Object url, String...parameters) throws RestCallException {
		return (MockRestRequest)super.formPostPairs(url, parameters);
	}

	@Override /* RestClient */
	public MockRestRequest patch(Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.patch(url, body);
	}

	@Override /* RestClient */
	public MockRestRequest patch(Object url, String body, ContentType contentType) throws RestCallException {
		return (MockRestRequest)super.patch(url, body, contentType);
	}

	@Override /* RestClient */
	public MockRestRequest patch(Object url) throws RestCallException {
		return (MockRestRequest)super.patch(url);
	}

	@Override /* RestClient */
	public MockRestRequest callback(String callString) throws RestCallException {
		return (MockRestRequest)super.callback(callString);
	}

	@Override /* RestClient */
	public MockRestRequest request(String method, Object url, Object body) throws RestCallException {
		return (MockRestRequest)super.request(method, url, body);
	}

	@Override /* RestClient */
	public MockRestRequest request(String method, Object url) throws RestCallException {
		return (MockRestRequest)super.request(method, url);
	}

	@Override /* RestClient */
	public MockRestRequest request(String method, Object url, boolean hasBody) throws RestCallException {
		return (MockRestRequest)super.request(method, url, hasBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Getters and setters.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the current client-side REST request.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current client-side REST request, or <jk>null</jk> if not set.
	 */
	public HttpRequest getCurrentClientRequest() {
		return rreq.get();
	}

	/**
	 * Returns the current client-side REST response.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current client-side REST response, or <jk>null</jk> if not set.
	 */
	public MockRestResponse getCurrentClientResponse() {
		return rres.get();
	}

	/**
	 * Returns the current server-side REST request.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current server-side REST request, or <jk>null</jk> if not set.
	 */
	public MockServletRequest getCurrentServerRequest() {
		return sreq.get();
	}

	/**
	 * Returns the current server-side REST response.
	 *
	 * <p>
	 * Note that this uses a {@link ThreadLocal} object for storage and so will not work on requests executed in
	 * separate threads such as when using {@link Future Futures}.
	 *
	 * @return The current server-side REST response, or <jk>null</jk> if not set.
	 */
	public MockServletResponse getCurrentServerResponse() {
		return sres.get();
	}

	MockRestClient currentResponse(MockRestResponse value) {
		rres.set(value);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// RestClient methods.
	//------------------------------------------------------------------------------------------------------------------

	@Override /* RestClient */
	protected MockRestRequest createRequest(URI uri, String method, boolean hasBody) throws RestCallException {
		return new MockRestRequest(this, uri, method, hasBody);
	}

	@Override /* RestClient */
	protected MockRestResponse createResponse(RestRequest req, HttpResponse httpResponse, Parser parser) throws RestCallException {
		return new MockRestResponse(this, req, httpResponse, parser);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClientConnection methods.
	//------------------------------------------------------------------------------------------------------------------

	@Override /* HttpClientConnection */
	public void close() throws IOException {
		// Don't call super.close() because it will close the client.
		rreq.remove();
		rres.remove();
		sreq.remove();
		sres.remove();
	}

	@Override /* HttpClientConnection */
	public boolean isOpen() {
		return true;
	}

	@Override /* HttpClientConnection */
	public boolean isStale() {
		return false;
	}

	@Override /* HttpClientConnection */
	public void setSocketTimeout(int timeout) {}

	@Override /* HttpClientConnection */
	public int getSocketTimeout() {
		return Integer.MAX_VALUE;
	}

	@Override /* HttpClientConnection */
	public void shutdown() throws IOException {}

	@Override /* HttpClientConnection */
	public HttpConnectionMetrics getMetrics() {
		return null;
	}

	@Override /* HttpClientConnection */
	public boolean isResponseAvailable(int timeout) throws IOException {
		return true;
	}

	@Override /* HttpClientConnection */
	public void sendRequestHeader(HttpRequest request) throws HttpException, IOException {
		try {
			RequestLine rl = request.getRequestLine();
			String path = rl.getUri();
			String target = findTarget(request);

			HttpRequest req = findRestRequest(request);
			rreq.set(req);
			rres.remove();
			sreq.remove();
			sres.remove();

			path = target + path;

			MockPathResolver pr = new MockPathResolver(target, contextPath, servletPath, path, null);
			if (pr.getError() != null)
				throw new RuntimeException(pr.getError());

			MockServletRequest r = MockServletRequest
				.create(request.getRequestLine().getMethod(), pr.getURI())
				.contextPath(pr.getContextPath())
				.servletPath(pr.getServletPath())
				.pathVars(pathVars)
				.debug(isDebug());

			for (Header h : request.getAllHeaders())
				r.header(h.getName(), h.getValue());

			sreq.set(r);
			sreq.get().applyOverrides(req);
		} catch (Exception e) {
			throw new HttpException(e.getMessage(), e);
		}
	}

	/**
	 * Attempts to unwrap the request to find the underlying RestRequest object.
	 * Returns the same object if one of the low-level client methods are used (e.g. execute(HttpUriRequest)).
	 */
	private HttpRequest findRestRequest(HttpRequest req) {
		if (req instanceof RestRequestCreated)
			return ((RestRequestCreated)req).getRestRequest();
		if (req instanceof HttpRequestWrapper)
			return findRestRequest(((HttpRequestWrapper) req).getOriginal());
		return req;
	}

	private String findTarget(HttpRequest req) {
		if (req instanceof HttpRequestWrapper) {
			HttpHost httpHost = ((HttpRequestWrapper)req).getTarget();
			if (httpHost != null)
				return httpHost.toURI();
		}
		return "http://localhost";
	}

	@Override /* HttpClientConnection */
	public void sendRequestEntity(HttpEntityEnclosingRequest request) throws HttpException, IOException {
		byte[] body = new byte[0];
		HttpEntity entity = request.getEntity();
		if (entity != null) {
			long length = entity.getContentLength();
			if (length < 0)
				length = 1024;
			ByteArrayOutputStream baos = new ByteArrayOutputStream((int)Math.min(length, 1024));
			entity.writeTo(baos);
			baos.flush();
			body = baos.toByteArray();
		}
		sreq.get().content(body);
	}

	@Override /* HttpClientConnection */
	public HttpResponse receiveResponseHeader() throws HttpException, IOException {
		try {
			MockServletResponse res = MockServletResponse.create();
			restContext.execute(restObject, sreq.get(), res);

			// If the status isn't set, something's broken.
			if (res.getStatus() == 0)
				throw new RuntimeException("Response status was 0.");

			// A bug in HttpClient causes an infinite loop if the response is less than 200.
			// As a workaround, just add 1000 to the status code (which is better than an infinite loop).
			if (res.getStatus() < 200)
				res.setStatus(1000 + res.getStatus());

			sres.set(res);

			HttpResponse response = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, res.getStatus(), res.getMessage()));
			res.getHeaders().forEach((k,v) -> {
				for (String hv : v)
					response.addHeader(k, hv);
			});

			return response;
		} catch (Exception e) {
			throw new HttpException(emptyIfNull(e.getMessage()), e);
		}
	}

	@Override /* HttpClientConnection */
	public void receiveResponseEntity(HttpResponse response) throws HttpException, IOException {
		InputStream is = new ByteArrayInputStream(sres.get().getContent());
		Header contentEncoding = response.getLastHeader("Content-Encoding");
		if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip"))
			is = new GZIPInputStream(is);
		response.setEntity(new InputStreamEntity(is));
	}

	@Override /* HttpClientConnection */
	public void flush() throws IOException {}
}
