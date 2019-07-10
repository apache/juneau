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
package org.apache.juneau.rest.mock2;

import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;

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
 * 	<li class='link'>{@doc juneau-rest-mock.MockRest}
 * </ul>
 */
public class MockRest implements MockHttpConnection {
	private static Map<Class<?>,RestContext> CONTEXTS_DEBUG = new ConcurrentHashMap<>(), CONTEXTS_NORMAL = new ConcurrentHashMap<>();

	private final RestContext ctx;

	/** Requests headers to add to every request. */
	protected final Map<String,Object> headers;

	/** Debug mode enabled. */
	protected final boolean debug;

	final String contextPath, servletPath;

	/**
	 * Constructor.
	 *
	 * @param b Builder.
	 */
	protected MockRest(Builder b) {
		try {
			debug = b.debug;
			Class<?> c = b.impl instanceof Class ? (Class<?>)b.impl : b.impl.getClass();
			Map<Class<?>,RestContext> contexts = debug ? CONTEXTS_DEBUG : CONTEXTS_NORMAL;
			if (! contexts.containsKey(c)) {
				Object o = b.impl instanceof Class ? ((Class<?>)b.impl).newInstance() : b.impl;
				RestContextBuilder rcb = RestContext.create(o);
				if (debug) {
					rcb.debug(Enablement.ALWAYS);
					rcb.callLoggerConfig(RestCallLoggerConfig.DEFAULT_DEBUG);
				}
				RestContext rc = rcb.build();
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
			contextPath = b.contextPath;
			servletPath = b.servletPath;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new builder with the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * No <c>Accept</c> or <c>Content-Type</c> header is specified by default.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static Builder create(Object impl) {
		return new Builder(impl);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <c>"application/json+simple"</c> by default.
	 * <c>Content-Type</c> header is set to <c>"application/json"</c> by default.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl) {
		return build(impl, SimpleJson.DEFAULT);
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to the primary media types on the specified marshall.
	 *
	 * <p>
	 * Note that the marshall itself is not involved in any serialization or parsing.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).marshall(m).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for specifying the <c>Accept</c> and <c>Content-Type</c> headers.
	 * 	<br>If <jk>null</jk>, headers will be reset.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl, Marshall m) {
		return create(impl).marshall(m).build();
	}

	/**
	 * Convenience method for creating a MockRest over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to the primary media types on the specified serializer and parser.
	 *
	 * <p>
	 * Note that the marshall itself is not involved in any serialization or parsing.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRest.create(impl, SimpleJson.<jsf>DEFAULT</jsf>).serializer(s).parser(p).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for specifying the <c>Content-Type</c> header.
	 * 	<br>If <jk>null</jk>, header will be reset.
	 * @param p
	 * 	The parser to use for specifying the <c>Accept</c> header.
	 * 	<br>If <jk>null</jk>, header will be reset.
	 * @return A new {@link MockRest} object.
	 */
	public static MockRest build(Object impl, Serializer s, Parser p) {
		return create(impl).serializer(s).parser(p).build();
	}

	/**
	 * Builder class.
	 */
	public static class Builder {
		Object impl;
		boolean debug;
		Map<String,Object> headers = new LinkedHashMap<>();
		String contextPath, servletPath;

		Builder(Object impl) {
			this.impl = impl;
		}

		/**
		 * Enable debug mode.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder debug() {
			this.debug = true;
			header("X-Debug", true);
			return this;
		}

		/**
		 * Enable no-trace mode.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder noTrace() {
			header("X-NoTrace", true);
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
		 * Specifies the <c>Accept</c> header to every request.
		 *
		 * @param value The <code>Accept/code> header value.
		 * @return This object (for method chaining).
		 */
		public Builder accept(String value) {
			return header("Accept", value);
		}

		/**
		 * Specifies the  <c>Content-Type</c> header to every request.
		 *
		 * @param value The <c>Content-Type</c> header value.
		 * @return This object (for method chaining).
		 */
		public Builder contentType(String value) {
			return header("Content-Type", value);
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder json() {
			return accept("application/json").contentType("application/json");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json+simple"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder simpleJson() {
			return accept("application/json+simple").contentType("application/json+simple");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/xml"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder xml() {
			return accept("text/xml").contentType("text/xml");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/html"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder html() {
			return accept("text/html").contentType("text/html");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/plain"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder plainText() {
			return accept("text/plain").contentType("text/plain");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"octal/msgpack"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder msgpack() {
			return accept("octal/msgpack").contentType("octal/msgpack");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/uon"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder uon() {
			return accept("text/uon").contentType("text/uon");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/x-www-form-urlencoded"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder urlEnc() {
			return accept("application/x-www-form-urlencoded").contentType("application/x-www-form-urlencoded");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/yaml"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder yaml() {
			return accept("text/yaml").contentType("text/yaml");
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/openapi"</js>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder openapi() {
			return accept("text/openapi").contentType("text/openapi");
		}

		/**
		 * Convenience method for setting the <c>Content-Type</c> header to the primary media type on the specified serializer.
		 *
		 * @param value
		 * 	The serializer to get the media type from.
		 * 	<br>If <jk>null</jk>, header will be reset.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(Serializer value) {
			return contentType(value == null ? null : value.getPrimaryMediaType().toString());
		}

		/**
		 * Convenience method for setting the <c>Accept</c> header to the primary media type on the specified parser.
		 *
		 * @param value
		 * 	The parser to get the media type from.
		 * 	<br>If <jk>null</jk>, header will be reset.
		 * @return This object (for method chaining).
		 */
		public Builder parser(Parser value) {
			return accept(value == null ? null : value.getPrimaryMediaType().toString());
		}

		/**
		 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to the primary media types on the specified marshall.
		 *
		 * @param value
		 * 	The marshall to get the media types from.
		 * 	<br>If <jk>null</jk>, headers will be reset.
		 * @return This object (for method chaining).
		 */
		public Builder marshall(Marshall value) {
			contentType(value == null ? null : value.getSerializer().getPrimaryMediaType().toString());
			accept(value == null ? null : value.getParser().getPrimaryMediaType().toString());
			return this;
		}

		/**
		 * Identifies the context path for the REST resource.
		 *
		 * <p>
		 * If not specified, uses <js>""</js>.
		 *
		 * @param value
		 * 	The context path.
		 * 	<br>Must not be <jk>null</jk> and must either be blank or start but not end with a <js>'/'</js> character.
		 * @return This object (for method chaining).
		 */
		public Builder contextPath(String value) {
			validateContextPath(value);
			this.contextPath = value;
			return this;
		}

		/**
		 * Identifies the servlet path for the REST resource.
		 *
		 * <p>
		 * If not specified, uses <js>""</js>.
		 *
		 * @param value
		 * 	The servlet path.
		 * 	<br>Must not be <jk>null</jk> and must either be blank or start but not end with a <js>'/'</js> character.
		 * @return This object (for method chaining).
		 */
		public Builder servletPath(String value) {
			validateServletPath(value);
			this.servletPath = value;
			return this;
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
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return A new servlet request.
	 */
	@Override /* MockHttpConnection */
	public MockServletRequest request(String method, String path, Map<String,Object> headers, Object body) {
		String p = RestUtils.trimContextPath(ctx.getPath(), path);
		return MockServletRequest.create(method, p).contextPath(emptyIfNull(contextPath)).servletPath(emptyIfNull(servletPath)).body(body).headers(this.headers).headers(headers).debug(debug).restContext(ctx);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest request(String method, String path) {
		return request(method, path, null, null);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return A new servlet request.
	 */
	public MockServletRequest request(String method, String path, Object body) {
		return request(method, path, null, body);
	}

	/**
	 * Performs a REST request against the REST interface.
	 *
	 * @param method The HTTP method
	 * @param headers Optional headers to include in the request.
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest request(String method, Map<String,Object> headers, String path) {
		return request(method, path, headers, null);
	}

	/**
	 * Perform a GET request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest get(String path) {
		return request("GET", path, null, null);
	}

	/**
	 * Shortcut for <code>get(<js>""</js>)</code>
	 *
	 * @return A new servlet request.
	 */
	public MockServletRequest get() {
		return get("");
	}

	/**
	 * Perform a PUT request.
	 *
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return A new servlet request.
	 */
	public MockServletRequest put(String path, Object body)  {
		return request("PUT", path, null, body);
	}

	/**
	 * Perform a POST request.
	 *
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return A new servlet request.
	 */
	public MockServletRequest post(String path, Object body) {
		return request("POST", path, null, body);
	}

	/**
	 * Perform a DELETE request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest delete(String path) {
		return request("DELETE", path, null, null);
	}

	/**
	 * Perform a HEAD request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest head(String path) {
		return request("HEAD", path, null, null);
	}

	/**
	 * Perform an OPTIONS request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest options(String path) {
		return request("OPTIONS", path, null, null);
	}

	/**
	 * Perform a PATCH request.
	 *
	 * @param path The URI path.
	 * @param body
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return A new servlet request.
	 */
	public MockServletRequest patch(String path, Object body) {
		return request("PATCH", path, null, body);
	}

	/**
	 * Perform a CONNECT request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest connect(String path) {
		return request("CONNECT", path, null, null);
	}

	/**
	 * Perform a TRACE request.
	 *
	 * @param path The URI path.
	 * @return A new servlet request.
	 */
	public MockServletRequest trace(String path) {
		return request("TRACE", path, null, null);
	}

	/**
	 * Returns the headers that were defined in this class.
	 *
	 * @return
	 * 	The headers that were defined in this class.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getHeaders() {
		return headers;
	}
}
