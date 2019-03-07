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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.utils.*;

/**
 * Creates a mocked interface against a REST resource class.
 *
 * <p>
 * Allows you to test your REST resource classes without a running servlet container.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 *  <jk>public class</jk> MockTest {
 *
 *  	<jc>// Our REST resource to test.</jc>
 *  	<ja>@RestResource</ja>(serializers=JsonSerializer.Simple.<jk>class</jk>, parsers=JsonParser.<jk>class</jk>)
 *  	<jk>public static class</jk> MyRest {
 *
 *  		<ja>@RestMethod</ja>(name=<jsf>PUT</jsf>, path=<js>"/String"</js>)
 *  		<jk>public</jk> String echo(<ja>@Body</ja> String b) {
 *  			<jk>return</jk> b;
 *  		}
 *  	}
 *
 *  <ja>@Test</ja>
 *  <jk>public void</jk> testEcho() <jk>throws</jk> Exception {
 *  	MockRest
 *  		.<jsf>create</jsf>(MyRest.<jk>class</jk>)
 *  		.put(<js>"/String"</js>, <js>"'foo'"</js>)
 *  		.execute()
 *  		.assertStatus(200)
 *  		.assertBody(<js>"'foo'"</js>);
 *  }
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.UnitTesting}
 * 	<li class='link'>{@doc juneau-rest-client.UnitTesting}
 * </ul>
 */
public class MockRest implements MockHttpConnection {
	private static Map<Class<?>,RestContext> CONTEXTS_DEBUG = new ConcurrentHashMap<>(), CONTEXTS_NORMAL = new ConcurrentHashMap<>();

	private final RestContext ctx;

	/** Requests headers to add to every request. */
	protected final Map<String,Object> headers;

	/** Debug mode enabled. */
	protected final boolean debug;

	/**
	 * Constructor.
	 *
	 * @param b Builder.
	 */
	protected MockRest(Builder b) {
		try {
			debug = b.debug;
			Class<?> c = b.implClass;
			Object o = b.implObject;
			Map<Class<?>,RestContext> contexts = debug ? CONTEXTS_DEBUG : CONTEXTS_NORMAL;
			if (! contexts.containsKey(c)) {
				if (o == null)
					o = c.newInstance();
				RestContext rc = RestContext.create(o).logger(b.debug ? BasicRestLogger.class : NoOpRestLogger.class).build();
				if (o instanceof RestServlet) {
					((RestServlet)o).setContext(rc);
				} else {
					rc.postInit();
				}
				rc.postInitChildFirst();
				contexts.put(c, rc);
			}
			ctx = contexts.get(c);
			headers = new LinkedHashMap<>(b.headers);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new builder with no REST implementation.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Creates a new builder with the specified REST implementation class.
	 *
	 * <p>
	 * Uses Simple-JSON as the protocol by default.
	 *
	 * @param impl
	 * 	The REST bean class.
	 * 	<br>Class must have a no-arg constructor.
	 * 	<br>Use {@link #create(Object)} for already-instantiated REST classes.
	 * @return A new builder.
	 */
	public static Builder create(Class<?> impl) {
		return create().impl(impl);
	}

	/**
	 * Creates a new builder with the specified REST implementation class.
	 *
	 * <p>
	 * Uses Simple-JSON as the protocol by default.
	 *
	 * @param impl The REST bean.
	 * @return A new builder.
	 */
	public static Builder create(Object impl) {
		return create().impl(impl);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).build();
	 * </p>
	 *
	 * @param impl The REST bean class.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Class<?> impl) {
		return build(impl, SimpleJson.DEFAULT);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).build();
	 * </p>
	 *
	 * @param impl The REST bean.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl) {
		return build(impl, SimpleJson.DEFAULT);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation.
	 *
	 * @param impl The REST bean class.
	 * @param m
	 * 	The marshall to use for serializing and parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Class<?> impl, Marshall m) {
		Builder b = create().impl(impl);
		if (m != null)
			b.accept(m.getParser().getPrimaryMediaType().toString()).contentType(m.getSerializer().getPrimaryMediaType().toString());
		return b.build();
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation.
	 *
	 * @param impl The REST bean object.
	 * @param m
	 * 	The marshall to use for serializing and parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl, Marshall m) {
		Builder b = create().impl(impl);
		if (m != null)
			b.accept(m.getParser().getPrimaryMediaType().toString()).contentType(m.getSerializer().getPrimaryMediaType().toString());
		return b.build();
	}

	/**
	 * Returns the headers that were defined in this class.
	 *
	 * @return The headers that were defined in this class.  Never <jk>null</jk>.
	 */
	public Map<String,Object> getHeaders() {
		return headers;
	}

	/**
	 * Builder class.
	 */
	public static class Builder {
		Class<?> implClass;
		Object implObject;
		boolean debug;
		Map<String,Object> headers = new LinkedHashMap<>();

		/**
		 * Specifies the REST implementation class.
		 *
		 * @param value
		 * 	The REST implementation class.
		 * 	<br>Class must have a no-arg constructor.
		 * @return This object (for method chaining).
		 */
		public Builder impl(Class<?> value) {
			this.implClass = value;
			return this;
		}

		/**
		 * Specifies the REST implementation bean.
		 *
		 * @param value
		 * 	The REST implementation bean.
		 * @return This object (for method chaining).
		 */
		public Builder impl(Object value) {
			if (value instanceof Class) {
				this.implClass = (Class<?>)value;
			} else {
				this.implObject = value;
				this.implClass = value.getClass();
			}
			return this;
		}

		/**
		 * Enable debug mode.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder debug() {
			this.debug = true;
			return this;
		}

		/**
		 * Adds a header to every request.
		 *
		 * @param name The header name.
		 * @param value
		 * 	The header value.
		 * 	<br>Can be <jk>null</jk> (will be skipped).
		 * @return This object (for method chaining).
		 */
		public Builder header(String name, Object value) {
			this.headers.put(name, value);
			return this;
		}

		/**
		 * Adds the specified headers to every request.
		 *
		 * @param value
		 * 	The header values.
		 * 	<br>Can be <jk>null</jk> (existing values will be cleared).
		 * 	<br><jk>null</jk> null map values will be ignored.
		 * @return This object (for method chaining).
		 */
		public Builder headers(Map<String,Object> value) {
			if (value != null)
				this.headers.putAll(value);
			else
				this.headers.clear();
			return this;
		}

		/**
		 * Adds an <code>Accept</code> header to every request.
		 *
		 * @param value The <code>Accept/code> header value.
		 * @return This object (for method chaining).
		 */
		public Builder accept(String value) {
			return header("Accept", value);
		}

		/**
		 * Adds a <code>Content-Type</code> header to every request.
		 *
		 * @param value The <code>Content-Type</code> header value.
		 * @return This object (for method chaining).
		 */
		public Builder contentType(String value) {
			return header("Content-Type", value);
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder json() {
			return accept("application/json").contentType("application/json");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json+simple"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder simpleJson() {
			return accept("application/json+simple").contentType("application/json+simple");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/xml"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder xml() {
			return accept("text/xml").contentType("text/xml");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/html"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder html() {
			return accept("text/html").contentType("text/html");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/plain"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder plainText() {
			return accept("text/plain").contentType("text/plain");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"octal/msgpack"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder msgpack() {
			return accept("octal/msgpack").contentType("octal/msgpack");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/uon"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder uon() {
			return accept("text/uon").contentType("text/uon");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/x-www-form-urlencoded"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder urlEnc() {
			return accept("application/x-www-form-urlencoded").contentType("application/x-www-form-urlencoded");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/yaml"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder yaml() {
			return accept("text/yaml").contentType("text/yaml");
		}

		/**
		 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/openapi"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder openapi() {
			return accept("text/openapi").contentType("text/openapi");
		}

		/**
		 * Create a new {@link MockRest} object based on the settings on this builder.
		 *
		 * @return A new {@link MockRest} object.
		 */
		public MockRest build() {
			return new MockRest(this);
		}

	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @param headers Optional headers to include in the request.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <code>toString()</code> method.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	@Override /* MockHttpConnection */
	public MockServletRequest request(String method, String path, Map<String,Object> headers, Object body) throws Exception {
		String p = RestUtils.trimContextPath(ctx.getPath(), path);
		return MockServletRequest.create(method, p).body(body).headers(this.headers).headers(headers).debug(debug).restContext(ctx);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest request(String method, String path) throws Exception {
		return request(method, path, null, null);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param headers Optional headers to include in the request.
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest request(String method, Map<String,Object> headers, String path) throws Exception {
		return request(method, path, headers, null);
	}

	/**
	 * Perform a GET request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest get(String path) throws Exception {
		return request("GET", path, null, null);
	}

	/**
	 * Perform a PUT request.
	 *
	 * @param path The URI path.
	 * @param body The body of the request.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest put(String path, Object body) throws Exception {
		return request("PUT", path, null, body);
	}

	/**
	 * Perform a POST request.
	 *
	 * @param path The URI path.
	 * @param body The body of the request.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest post(String path, Object body) throws Exception {
		return request("POST", path, null, body);
	}

	/**
	 * Perform a DELETE request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest delete(String path) throws Exception {
		return request("DELETE", path, null, null);
	}

	/**
	 * Perform an OPTIONS request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest options(String path) throws Exception {
		return request("OPTIONS", path, null, null);
	}

	/**
	 * Perform a PATCH request.
	 *
	 * @param path The URI path.
	 * @param body The body of the request.
	 * @return A new servlet request.
	 * @throws Exception
	 */
	public MockServletRequest patch(String path, Object body) throws Exception {
		return request("PATCH", path, null, body);
	}
}
