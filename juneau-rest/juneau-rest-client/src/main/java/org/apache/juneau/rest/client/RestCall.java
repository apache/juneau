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
package org.apache.juneau.rest.client;

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Represents a connection to a remote REST resource.
 *
 * <p>
 * Instances of this class are created by the various {@code doX()} methods on the {@link RestClient} class.
 *
 * <p>
 * This class uses only Java standard APIs.  Requests can be built up using a fluent interface with method chaining,
 * like so...
 * <p class='bcode'>
 * 	RestClient client = <jk>new</jk> RestClient();
 * 	RestCall c = client.doPost(<jsf>URL</jsf>).setInput(o).setHeader(x,y);
 * 	MyBean b = c.getResponse(MyBean.<jk>class</jk>);
 * </p>
 *
 * <p>
 * The actual connection and request/response transaction occurs when calling one of the <code>getResponseXXX()</code>
 * methods.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-client">Overview &gt; juneau-rest-client</a>
 * </ul>
 */
@SuppressWarnings({ "unchecked" })
public final class RestCall extends BeanSession implements Closeable {

	private final RestClient client;                       // The client that created this call.
	private final HttpRequestBase request;                 // The request.
	private HttpResponse response;                         // The response.
	private List<RestCallInterceptor> interceptors = new ArrayList<>();               // Used for intercepting and altering requests.

	private boolean isConnected = false;                   // connect() has been called.
	private boolean allowRedirectsOnPosts;
	private int retries = 1;
	private int redirectOnPostsTries = 5;
	private long retryInterval = -1;
	private RetryOn retryOn;
	private boolean ignoreErrors;
	private boolean byLines = false;
	private TeeWriter writers = new TeeWriter();
	private StringWriter capturedResponseWriter;
	private String capturedResponse;
	private TeeOutputStream outputStreams = new TeeOutputStream();
	private boolean isClosed = false;
	private boolean isFailed = false;
	private Object input;
	private boolean hasInput;  // input() was called, even if it's setting 'null'.
	private Serializer serializer;
	private Parser parser;
	private HttpPartParser partParser;
	private URIBuilder uriBuilder;
	private NameValuePairs formData;

	/**
	 * Constructs a REST call with the specified method name.
	 *
	 * @param client The client that created this request.
	 * @param request The wrapped Apache HTTP client request object.
	 * @param uri The URI for this call.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestCall(RestClient client, HttpRequestBase request, URI uri) throws RestCallException {
		super(client, BeanSessionArgs.DEFAULT);
		this.client = client;
		this.request = request;
		for (RestCallInterceptor i : this.client.interceptors)
			interceptor(i);
		this.retryOn = client.retryOn;
		this.retries = client.retries;
		this.retryInterval = client.retryInterval;
		this.serializer = client.serializer;
		this.parser = client.parser;
		this.partParser = client.getPartParser();
		uriBuilder = new URIBuilder(uri);
	}

	/**
	 * Sets the URI for this call.
	 *
	 * <p>
	 * Can be any of the following types:
	 * <ul>
	 * 	<li>{@link URI}
	 * 	<li>{@link URL}
	 * 	<li>{@link URIBuilder}
	 * 	<li>Anything else converted to a string using {@link Object#toString()}.
	 * </ul>
	 *
	 * <p>
	 * Relative URL strings will be interpreted as relative to the root URL defined on the client.
	 *
	 * @param uri
	 * 	The URI to use for this call.
	 * 	This overrides the URI passed in from the client.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall uri(Object uri) throws RestCallException {
		try {
			if (uri != null)
				uriBuilder = new URIBuilder(client.toURI(uri));
			return this;
		} catch (URISyntaxException e) {
			throw new RestCallException(e);
		}
	}

	/**
	 * Sets the URI scheme.
	 *
	 * @param scheme The new URI host.
	 * @return This object (for method chaining).
	 */
	public RestCall scheme(String scheme) {
		uriBuilder.setScheme(scheme);
		return this;
	}

	/**
	 * Sets the URI host.
	 *
	 * @param host The new URI host.
	 * @return This object (for method chaining).
	 */
	public RestCall host(String host) {
		uriBuilder.setHost(host);
		return this;
	}

	/**
	 * Sets the URI port.
	 *
	 * @param port The new URI port.
	 * @return This object (for method chaining).
	 */
	public RestCall port(int port) {
		uriBuilder.setPort(port);
		return this;
	}

	/**
	 * Adds a query parameter to the URI query.
	 *
	 * @param name
	 * 	The parameter name.
	 * 	Can be null/blank/* if the value is a {@link Map}, {@link String}, {@link NameValuePairs}, or bean.
	 * @param value
	 * 	The parameter value converted to a string using UON notation.
	 * 	Can also be {@link Map}, {@link String}, {@link NameValuePairs}, or bean if the name is null/blank/*.
	 * 	If a {@link String} and the name is null/blank/*, then calls {@link URIBuilder#setCustomQuery(String)}.
	 * @param skipIfEmpty Don't add the pair if the value is empty.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * 	If <jk>null</jk>, then the URL-encoding serializer defined on the client is used.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Ignored if the part serializer is not a subclass of {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall query(String name, Object value, boolean skipIfEmpty, HttpPartSerializer serializer, HttpPartSchema schema) throws RestCallException {
		if (serializer == null)
			serializer = client.getPartSerializer();
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			if (value != null && ! (ObjectUtils.isEmpty(value) && skipIfEmpty))
				try {
					uriBuilder.addParameter(name, serializer.createSession(null).serialize(HttpPartType.QUERY, schema, value));
				} catch (SchemaValidationException e) {
					throw new RestCallException(e, "Validation error on request query parameter ''{0}''=''{1}''", name, value);
				} catch (SerializeException e) {
					throw new RestCallException(e, "Serialization error on request query parameter ''{0}''", name);
				}
		} else if (value instanceof NameValuePairs) {
			for (NameValuePair p : (NameValuePairs)value)
				query(p.getName(), p.getValue(), skipIfEmpty, serializer, schema);
		} else if (value instanceof Map) {
			for (Map.Entry<String,Object> p : ((Map<String,Object>) value).entrySet())
				query(p.getKey(), p.getValue(), skipIfEmpty, serializer, schema);
		} else if (isBean(value)) {
			return query(name, toBeanMap(value), skipIfEmpty, serializer, schema);
		} else if (value instanceof Reader) {
			try {
				uriBuilder.setCustomQuery(read(value));
			} catch (IOException e) {
				throw new RestCallException(e);
			}
		} else if (value instanceof CharSequence) {
			String s = value.toString();
			if (isNotEmpty(s))
				uriBuilder.setCustomQuery(s);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to query(name,value,skipIfEmpty) for data type ''{1}''", name, getReadableClassNameForObject(value));
		}
		return this;
	}

	/**
	 * Adds a query parameter to the URI query.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall query(String name, Object value) throws RestCallException {
		return query(name, value, false, null, null);
	}

	/**
	 * Adds query parameters to the URI query.
	 *
	 * @param params The parameters.  Values are converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall query(Map<String,Object> params) throws RestCallException {
		return query(null, params);
	}

	/**
	 * Adds a query parameter to the URI query if the parameter value is not <jk>null</jk> or an empty string.
	 *
	 * <p>
	 * NE = "not empty"
	 *
	 * @param name The parameter name.
	 * @param value The parameter value converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall queryIfNE(String name, Object value) throws RestCallException {
		return query(name, value, true, null, null);
	}

	/**
	 * Adds query parameters to the URI for any parameters that aren't null/empty.
	 *
	 * <p>
	 * NE = "not empty"
	 *
	 * @param params The parameters.  Values are converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall queryIfNE(Map<String,Object> params) throws RestCallException {
		return query(null, params, true, null, null);
	}

	/**
	 * Sets a custom URI query.
	 *
	 * @param query The new URI query string.
	 * @return This object (for method chaining).
	 */
	public RestCall query(String query) {
		uriBuilder.setCustomQuery(query);
		return this;
	}

	/**
	 * Adds a form data pair to this request to perform a URL-encoded form post.
	 *
	 * @param name
	 * 	The parameter name.
	 * 	Can be null/blank/* if the value is a {@link Map}, {@link NameValuePairs}, or bean.
	 * @param value
	 * 	The parameter value converted to a string using UON notation.
	 * 	Can also be {@link Map}, {@link NameValuePairs}, or bean if the name is null/blank/*.
	 * @param skipIfEmpty Don't add the pair if the value is empty.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * 	If <jk>null</jk>, then the URL-encoding serializer defined on the client is used.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Ignored if the part serializer is not a subclass of {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall formData(String name, Object value, boolean skipIfEmpty, HttpPartSerializer serializer, HttpPartSchema schema) throws RestCallException {
		if (formData == null)
			formData = new NameValuePairs();
		if (serializer == null)
			serializer = client.getPartSerializer();
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			if (value != null && ! (ObjectUtils.isEmpty(value) && skipIfEmpty))
				formData.add(new SerializedNameValuePair(name, value, serializer, schema));
		} else if (value instanceof NameValuePairs) {
			for (NameValuePair p : (NameValuePairs)value)
				if (p.getValue() != null && ! (isEmpty(p.getValue()) && skipIfEmpty))
					formData.add(p);
		} else if (value instanceof Map) {
			for (Map.Entry<String,Object> p : ((Map<String,Object>) value).entrySet())
				formData(p.getKey(), p.getValue(), skipIfEmpty, serializer, schema);
		} else if (isBean(value)) {
			return formData(name, toBeanMap(value), skipIfEmpty, serializer, schema);
		} else if (value instanceof Reader) {
			contentType("application/x-www-form-urlencoded");
			body(value);
		} else if (value instanceof CharSequence) {
			try {
				contentType("application/x-www-form-urlencoded");
				body(new StringEntity(value.toString()));
			} catch (UnsupportedEncodingException e) {}
		} else {
			throw new FormattedRuntimeException("Invalid name ''{0}'' passed to formData(name,value,skipIfEmpty) for data type ''{1}''", name, getReadableClassNameForObject(value));
		}
		return this;
	}

	/**
	 * Adds a form data pair to this request to perform a URL-encoded form post.
	 *
	 * @param name
	 * 	The parameter name.
	 * 	Can be null/blank if the value is a {@link Map} or {@link NameValuePairs}.
	 * @param value
	 * 	The parameter value converted to a string using UON notation.
	 * 	Can also be a {@link Map} or {@link NameValuePairs}.
	 * @return This object (for method chaining).
	 * @throws RestCallException If name was null/blank and value wasn't a {@link Map} or {@link NameValuePairs}.
	 */
	public RestCall formData(String name, Object value) throws RestCallException {
		return formData(name, value, false, null, null);
	}

	/**
	 * Adds form data pairs to this request to perform a URL-encoded form post.
	 *
	 * @param nameValuePairs The name-value pairs of the request.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall formData(NameValuePairs nameValuePairs) throws RestCallException {
		return formData(null, nameValuePairs);
	}

	/**
	 * Adds form data pairs to this request to perform a URL-encoded form post.
	 *
	 * @param params The parameters.  Values are converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException If name was null/blank and value wasn't a {@link Map} or {@link NameValuePairs}.
	 */
	public RestCall formData(Map<String,Object> params) throws RestCallException {
		return formData(null, params);
	}

	/**
	 * Adds a form data pair to the request if the parameter value is not <jk>null</jk> or an empty string.
	 *
	 * <p>
	 * NE = "not empty"
	 *
	 * @param name The parameter name.
	 * @param value The parameter value converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall formDataIfNE(String name, Object value) throws RestCallException {
		return formData(name, value, true, null, null);
	}

	/**
	 * Adds form data parameters to the request for any parameters that aren't null/empty.
	 *
	 * <p>
	 * NE = "not empty"
	 *
	 * @param params The parameters.  Values are converted to a string using UON notation.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall formDataIfNE(Map<String,Object> params) throws RestCallException {
		return formData(null, params, true, null, null);
	}

	/**
	 * Replaces a variable of the form <js>"{name}"</js> in the URL path with the specified value.
	 *
	 * @param name The path variable name.
	 * @param value The replacement value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * 	If <jk>null</jk>, then the URL-encoding serializer defined on the client is used.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Ignored if the part serializer is not a subclass of {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 * @throws RestCallException If variable could not be found in path.
	 */
	public RestCall path(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) throws RestCallException {
		String path = uriBuilder.getPath();
		if (serializer == null)
			serializer = client.getPartSerializer();
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			String var = "{" + name + "}";
			if (path.indexOf(var) == -1)
				throw new RestCallException("Path variable {"+name+"} was not found in path.");
			try {
				uriBuilder.setPath(path.replace(var, serializer.createSession(null).serialize(HttpPartType.PATH, schema, value)));
			} catch (SchemaValidationException e) {
				throw new RestCallException(e, "Validation error on request path parameter ''{0}''=''{1}''", name, value);
			} catch (SerializeException e) {
				throw new RestCallException(e, "Serialization error on request path parameter ''{0}''", name);
			}
		} else if (value instanceof NameValuePairs) {
			for (NameValuePair p : (NameValuePairs)value)
				path(p.getName(), p.getValue(), serializer, schema);
		} else if (value instanceof Map) {
			for (Map.Entry<String,Object> p : ((Map<String,Object>) value).entrySet())
				path(p.getKey(), p.getValue(), serializer, schema);
		} else if (isBean(value)) {
			return path(name, toBeanMap(value), serializer, schema);
		} else if (value != null) {
			throw new RestCallException("Invalid name ''{0}'' passed to path(name,value) for data type ''{1}''", name, getReadableClassNameForObject(value));
		}
		return this;
	}

	/**
	 * Replaces a variable of the form <js>"{name}"</js> in the URL path with the specified value.
	 *
	 * @param name The path variable name.
	 * @param value The replacement value.
	 * @return This object (for method chaining).
	 * @throws RestCallException If variable could not be found in path.
	 */
	public RestCall path(String name, Object value) throws RestCallException {
		return path(name, value, null, null);
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param userInfo The new URI user info.
	 * @return This object (for method chaining).
	 */
	public RestCall userInfo(String userInfo) {
		uriBuilder.setUserInfo(userInfo);
		return this;
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param username The new URI username.
	 * @param password The new URI password.
	 * @return This object (for method chaining).
	 */
	public RestCall userInfo(String username, String password) {
		uriBuilder.setUserInfo(username, password);
		return this;
	}

	/**
	 * Sets the input for this REST call.
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT and POST) requests.
	 * 	<br>Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestCall body(Object input) throws RestCallException {
		this.input = input;
		this.hasInput = true;
		this.formData = null;
		return this;
	}

	/**
	 * Same as {@link #body(Object)} but allows you to specify a part serializer to use to serialize the body.
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT and POST) requests. <br>
	 * @param partSerializer
	 * 	The part serializer to use to serialize the body of the request.
	 * @param schema
	 * 	The schema information about the part being serialized.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall body(Object input, HttpPartSerializer partSerializer, HttpPartSchema schema) throws RestCallException {
		try {
			if (partSerializer != null)
				body(partSerializer.serialize(HttpPartType.BODY, schema, input));
			else
				body(input);
		} catch (SchemaValidationException e) {
			throw new RestCallException(e, "Validation error on request body.");
		} catch (SerializeException e) {
			throw new RestCallException(e, "Serialization error on request body.");
		}
		return this;
	}

	/**
	 * Specifies the serializer to use on this call.
	 *
	 * <p>
	 * Overrides the serializer specified on the {@link RestClient}.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object (for method chaining).
	 */
	public RestCall serializer(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Specifies the parser to use on this call.
	 *
	 * <p>
	 * Overrides the parser specified on the {@link RestClient}.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object (for method chaining).
	 */
	public RestCall parser(Parser parser) {
		this.parser = parser;
		return this;
	}

	//--------------------------------------------------------------------------------
	// HTTP headers
	//--------------------------------------------------------------------------------

	/**
	 * Sets a header on the request.
	 *
	 * @param name
	 * 	The header name.
	 * 	The name can be null/empty if the value is a {@link Map}.
	 * @param value The header value.
	 * @param skipIfEmpty Don't add the header if the name is null/empty.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * 	If <jk>null</jk>, then the URL-encoding serializer defined on the client is used.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Ignored if the part serializer is not a subclass of {@link OpenApiPartSerializer}.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall header(String name, Object value, boolean skipIfEmpty, HttpPartSerializer serializer, HttpPartSchema schema) throws RestCallException {
		if (serializer == null)
			serializer = client.getPartSerializer();
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			if (value != null && ! (ObjectUtils.isEmpty(value) && skipIfEmpty))
				try {
					request.setHeader(name, serializer.createSession(null).serialize(HttpPartType.HEADER, schema, value));
				} catch (SchemaValidationException e) {
					throw new RestCallException(e, "Validation error on request header parameter ''{0}''=''{1}''", name, value);
				} catch (SerializeException e) {
					throw new RestCallException(e, "Serialization error on request header parameter ''{0}''", name);
				}
		} else if (value instanceof NameValuePairs) {
			for (NameValuePair p : (NameValuePairs)value)
				header(p.getName(), p.getValue(), skipIfEmpty, SimpleUonPartSerializer.DEFAULT, schema);
		} else if (value instanceof Map) {
			for (Map.Entry<String,Object> p : ((Map<String,Object>) value).entrySet())
				header(p.getKey(), p.getValue(), skipIfEmpty, serializer, schema);
		} else if (isBean(value)) {
			return header(name, toBeanMap(value), skipIfEmpty, serializer, schema);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to header(name,value,skipIfEmpty) for data type ''{1}''", name, getReadableClassNameForObject(value));
		}
		return this;
	}


	/**
	 * Sets a header on the request.
	 *
	 * @param name
	 * 	The header name.
	 * 	The name can be null/empty if the value is a {@link Map}.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall header(String name, Object value) throws RestCallException {
		return header(name, value, false, null, null);
	}

	/**
	 * Sets headers on the request.
	 *
	 * @param values The header values.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall headers(Map<String,Object> values) throws RestCallException {
		return header(null, values, false, null, null);
	}

	/**
	 * Sets a header on the request if the value is not null/empty.
	 *
	 * <p>
	 * NE = "not empty"
	 *
	 * @param name
	 * 	The header name.
	 * 	The name can be null/empty if the value is a {@link Map}.
	 * @param value The header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall headerIfNE(String name, Object value) throws RestCallException {
		return header(name, value, true, null, null);
	}

	/**
	 * Sets headers on the request if the values are not null/empty.
	 *
	 * <p>
	 * NE = "not empty"
	 *
	 * @param values The header values.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall headersIfNE(Map<String,Object> values) throws RestCallException {
		return header(null, values, true, null, null);
	}

	/**
	 * Sets the value for the <code>Accept</code> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling
	 * <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall accept(Object value) throws RestCallException {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <code>Accept-Charset</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall acceptCharset(Object value) throws RestCallException {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <code>Accept-Encoding</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall acceptEncoding(Object value) throws RestCallException {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <code>Accept-Language</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall acceptLanguage(Object value) throws RestCallException {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <code>Authorization</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall authorization(Object value) throws RestCallException {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <code>Cache-Control</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall cacheControl(Object value) throws RestCallException {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the value for the <code>Connection</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall connection(Object value) throws RestCallException {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <code>Content-Length</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall contentLength(Object value) throws RestCallException {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <code>Content-Type</code> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling
	 * <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall contentType(Object value) throws RestCallException {
		return header("Content-Type", value);
	}

	/**
	 * Sets the value for the <code>Date</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall date(Object value) throws RestCallException {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <code>Expect</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall expect(Object value) throws RestCallException {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <code>Forwarded</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall forwarded(Object value) throws RestCallException {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <code>From</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall from(Object value) throws RestCallException {
		return header("From", value);
	}

	/**
	 * Sets the value for the <code>Host</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall host(Object value) throws RestCallException {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <code>If-Match</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall ifMatch(Object value) throws RestCallException {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Modified-Since</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall ifModifiedSince(Object value) throws RestCallException {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <code>If-None-Match</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall ifNoneMatch(Object value) throws RestCallException {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <code>If-Range</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall ifRange(Object value) throws RestCallException {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <code>If-Unmodified-Since</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall ifUnmodifiedSince(Object value) throws RestCallException {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <code>Max-Forwards</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall maxForwards(Object value) throws RestCallException {
		return header("Max-Forwards", value);
	}

	/**
	 * Sets the value for the <code>Origin</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall origin(Object value) throws RestCallException {
		return header("Origin", value);
	}

	/**
	 * Sets the value for the <code>Pragma</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall pragma(Object value) throws RestCallException {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <code>Proxy-Authorization</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall proxyAuthorization(Object value) throws RestCallException {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <code>Range</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall range(Object value) throws RestCallException {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <code>Referer</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall referer(Object value) throws RestCallException {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <code>TE</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall te(Object value) throws RestCallException {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <code>User-Agent</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall userAgent(Object value) throws RestCallException {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <code>Upgrade</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall upgrade(Object value) throws RestCallException {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <code>Via</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall via(Object value) throws RestCallException {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <code>Warning</code> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall warning(Object value) throws RestCallException {
		return header("Warning", value);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param version The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall clientVersion(String version) throws RestCallException {
		return header("X-Client-Version", version);
	}

	/**
	 * Make this call retryable if an error response (>=400) is received.
	 *
	 * @param retries The number of retries to attempt.
	 * @param interval The time in milliseconds between attempts.
	 * @param retryOn
	 * 	Optional object used for determining whether a retry should be attempted.
	 * 	If <jk>null</jk>, uses {@link RetryOn#DEFAULT}.
	 * @return This object (for method chaining).
	 * @throws RestCallException If current entity is not repeatable.
	 */
	public RestCall retryable(int retries, long interval, RetryOn retryOn) throws RestCallException {
		if (request instanceof HttpEntityEnclosingRequestBase) {
			if (input != null && input instanceof HttpEntity) {
				HttpEntity e = (HttpEntity)input;
				if (e != null && ! e.isRepeatable())
					throw new RestCallException("Attempt to make call retryable, but entity is not repeatable.");
				}
			}
		this.retries = retries;
		this.retryInterval = interval;
		this.retryOn = (retryOn == null ? RetryOn.DEFAULT : retryOn);
		return this;

	}

	/**
	 * For this call, allow automatic redirects when a 302 or 307 occurs when performing a POST.
	 *
	 * <p>
	 * Note that this can be inefficient since the POST body needs to be serialized twice.
	 * The preferred approach if possible is to use the {@link LaxRedirectStrategy} strategy on the underlying HTTP
	 * client.
	 * However, this method is provided if you don't have access to the underlying client.
	 *
	 * @param b Redirect flag.
	 * @return This object (for method chaining).
	 */
	public RestCall allowRedirectsOnPosts(boolean b) {
		this.allowRedirectsOnPosts = b;
		return this;
	}

	/**
	 * Specify the number of redirects to follow before throwing an exception.
	 *
	 * @param maxAttempts Allow a redirect to occur this number of times.
	 * @return This object (for method chaining).
	 */
	public RestCall redirectMaxAttempts(int maxAttempts) {
		this.redirectOnPostsTries = maxAttempts;
		return this;
	}

	/**
	 * Add an interceptor for this call only.
	 *
	 * @param interceptor The interceptor to add to this call.
	 * @return This object (for method chaining).
	 */
	public RestCall interceptor(RestCallInterceptor interceptor) {
		interceptors.add(interceptor);
		interceptor.onInit(this);
		return this;
	}

	/**
	 * Pipes the request output to the specified writer when {@link #run()} is called.
	 *
	 * <p>
	 * The writer is not closed.
	 *
	 * <p>
	 * This method can be called multiple times to pipe to multiple writers.
	 *
	 * @param w The writer to pipe the output to.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(Writer w) {
		return pipeTo(w, false);
	}

	/**
	 * Pipe output from response to the specified writer when {@link #run()} is called.
	 *
	 * <p>
	 * This method can be called multiple times to pipe to multiple writers.
	 *
	 * @param w The writer to write the output to.
	 * @param close Close the writer when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(Writer w, boolean close) {
		return pipeTo(null, w, close);
	}

	/**
	 * Pipe output from response to the specified writer when {@link #run()} is called and associate that writer with an
	 * ID so it can be retrieved through {@link #getWriter(String)}.
	 *
	 * <p>
	 * This method can be called multiple times to pipe to multiple writers.
	 *
	 * @param id A string identifier that can be used to retrieve the writer using {@link #getWriter(String)}
	 * @param w The writer to write the output to.
	 * @param close Close the writer when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(String id, Writer w, boolean close) {
		writers.add(id, w, close);
		return this;
	}

	/**
	 * Retrieves a writer associated with an ID via {@link #pipeTo(String, Writer, boolean)}
	 *
	 * @param id A string identifier that can be used to retrieve the writer using {@link #getWriter(String)}
	 * @return The writer, or <jk>null</jk> if no writer is associated with that ID.
	 */
	public Writer getWriter(String id) {
		return writers.getWriter(id);
	}

	/**
	 * When output is piped to writers, flush the writers after every line of output.
	 *
	 * @return This object (for method chaining).
	 */
	public RestCall byLines() {
		this.byLines = true;
		return this;
	}

	/**
	 * Pipes the request output to the specified output stream when {@link #run()} is called.
	 *
	 * <p>
	 * The output stream is not closed.
	 *
	 * <p>
	 * This method can be called multiple times to pipe to multiple output streams.
	 *
	 * @param os The output stream to pipe the output to.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(OutputStream os) {
		return pipeTo(os, false);
	}

	/**
	 * Pipe output from response to the specified output stream when {@link #run()} is called.
	 *
	 * <p>
	 * This method can be called multiple times to pipe to multiple output stream.
	 *
	 * @param os The output stream to write the output to.
	 * @param close Close the output stream when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(OutputStream os, boolean close) {
		return pipeTo(null, os, close);
	}

	/**
	 * Pipe output from response to the specified output stream when {@link #run()} is called and associate
	 * that output stream with an ID so it can be retrieved through {@link #getOutputStream(String)}.
	 *
	 * <p>
	 * This method can be called multiple times to pipe to multiple output stream.
	 *
	 * @param id A string identifier that can be used to retrieve the output stream using {@link #getOutputStream(String)}
	 * @param os The output stream to write the output to.
	 * @param close Close the output stream when {@link #close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestCall pipeTo(String id, OutputStream os, boolean close) {
		outputStreams.add(id, os, close);
		return this;
	}

	/**
	 * Retrieves an output stream associated with an ID via {@link #pipeTo(String, OutputStream, boolean)}
	 *
	 * @param id A string identifier that can be used to retrieve the writer using {@link #getWriter(String)}
	 * @return The writer, or <jk>null</jk> if no writer is associated with that ID.
	 */
	public OutputStream getOutputStream(String id) {
		return outputStreams.getOutputStream(id);
	}

	/**
	 * Prevent {@link RestCallException RestCallExceptions} from being thrown when HTTP status 400+ is encountered.
	 *
	 * @return This object (for method chaining).
	 */
	public RestCall ignoreErrors() {
		this.ignoreErrors = true;
		return this;
	}

	/**
	 * Stores the response text so that it can later be captured using {@link #getCapturedResponse()}.
	 *
	 * <p>
	 * This method should only be called once.  Multiple calls to this method are ignored.
	 *
	 * @return This object (for method chaining).
	 */
	public RestCall captureResponse() {
		if (capturedResponseWriter == null) {
			capturedResponseWriter = new StringWriter();
			writers.add(capturedResponseWriter, false);
		}
		return this;
	}


	/**
	 * Look for the specified regular expression pattern in the response output.
	 *
	 * <p>
	 * Causes a {@link RestCallException} to be thrown if the specified pattern is found in the output.
	 *
	 * <p>
	 * This method uses {@link #getCapturedResponse()} to read the response text and so does not affect the other output
	 * methods such as {@link #getResponseAsString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Throw a RestCallException if FAILURE or ERROR is found in the output.</jc>
	 * 	restClient.doGet(<jsf>URL</jsf>)
	 * 		.failurePattern(<js>"FAILURE|ERROR"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param errorPattern A regular expression to look for in the response output.
	 * @return This object (for method chaining).
	 */
	public RestCall failurePattern(final String errorPattern) {
		responsePattern(
			new ResponsePattern(errorPattern) {
				@Override
				public void onMatch(RestCall rc, Matcher m) throws RestCallException {
					throw new RestCallException("Failure pattern detected.");
				}
			}
		);
		return this;
	}

	/**
	 * Look for the specified regular expression pattern in the response output.
	 *
	 * <p>
	 * Causes a {@link RestCallException} to be thrown if the specified pattern is not found in the output.
	 *
	 * <p>
	 * This method uses {@link #getCapturedResponse()} to read the response text and so does not affect the other output
	 * methods such as {@link #getResponseAsString()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Throw a RestCallException if SUCCESS is not found in the output.</jc>
	 * 	restClient.doGet(<jsf>URL</jsf>)
	 * 		.successPattern(<js>"SUCCESS"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param successPattern A regular expression to look for in the response output.
	 * @return This object (for method chaining).
	 */
	public RestCall successPattern(String successPattern) {
		responsePattern(
			new ResponsePattern(successPattern) {
				@Override
				public void onNoMatch(RestCall rc) throws RestCallException {
					throw new RestCallException("Success pattern not detected.");
				}
			}
		);
		return this;
	}

	/**
	 * Adds a response pattern finder to look for regular expression matches in the response output.
	 *
	 * <p>
	 * This method can be called multiple times to add multiple response pattern finders.
	 *
	 * <p>
	 * {@link ResponsePattern ResponsePatterns} use the {@link #getCapturedResponse()} to read the response text and so
	 * does not affect the other output methods such as {@link #getResponseAsString()}.
	 *
	 * @param responsePattern The response pattern finder.
	 * @return This object (for method chaining).
	 */
	public RestCall responsePattern(final ResponsePattern responsePattern) {
		captureResponse();
		interceptor(
			new RestCallInterceptor() {
				@Override
				public void onClose(RestCall restCall) throws RestCallException {
					responsePattern.match(RestCall.this);
				}
			}
		);
		return this;
	}

	/**
	 * Set configuration settings on this request.
	 *
	 * <p>
	 * Use {@link RequestConfig#custom()} to create configuration parameters for the request.
	 *
	 * @param config The new configuration settings for this request.
	 * @return This object (for method chaining).
	 */
	public RestCall setConfig(RequestConfig config) {
		this.request.setConfig(config);
		return this;
	}

	/**
	 * Method used to execute an HTTP response where you're only interested in the HTTP response code.
	 *
	 * <p>
	 * The response entity is discarded unless one of the pipe methods have been specified to pipe the output to an
	 * output stream or writer.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jk>try</jk> {
	 * 		RestClient client = <jk>new</jk> RestClient();
	 * 		<jk>int</jk> rc = client.doGet(url).execute();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * @return The HTTP status code.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	public int run() throws RestCallException {
		connect();
		try {
			StatusLine status = response.getStatusLine();
			int sc = status.getStatusCode();
			if (sc >= 400 && ! ignoreErrors)
				throw new RestCallException(sc, status.getReasonPhrase(), request.getMethod(), request.getURI(), getResponseAsString()).setHttpResponse(response);
			if (outputStreams.size() > 0 || writers.size() > 0)
				getReader();
			return sc;
		} catch (RestCallException e) {
			isFailed = true;
			throw e;
		} catch (IOException e) {
			isFailed = true;
			throw new RestCallException(e).setHttpResponse(response);
		} finally {
			close();
		}
	}

	/**
	 * Same as {@link #run()} but allows you to run the call asynchronously.
	 *
	 * @return The HTTP status code.
	 * @throws RestCallException If the executor service was not defined.
	 * @see RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * {@link Future Futures}.
	 */
	public Future<Integer> runFuture() throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<Integer>() {
				@Override /* Callable */
				public Integer call() throws Exception {
					return run();
				}
			}
		);
	}

	/**
	 * Connects to the REST resource.
	 *
	 * <p>
	 * If this is a <code>PUT</code> or <code>POST</code>, also sends the input to the remote resource.<br>
	 *
	 * <p>
	 * Typically, you would only call this method if you're not interested in retrieving the body of the HTTP response.
	 * Otherwise, you're better off just calling one of the {@link #getReader()}/{@link #getResponse(Class)}/{@link #pipeTo(Writer)}
	 * methods directly which automatically call this method already.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException If an exception or <code>400+</code> HTTP status code occurred during the connection attempt.
	 */
	public RestCall connect() throws RestCallException {

		if (isConnected)
			return this;
		isConnected = true;

		try {

			request.setURI(uriBuilder.build());

			if (hasInput || formData != null) {

				if (hasInput && formData != null)
					throw new RestCallException("Both input and form data found on same request.");

				if (! (request instanceof HttpEntityEnclosingRequestBase))
					throw new RestCallException(0, "Method does not support content entity.", request.getMethod(), request.getURI(), null);

				HttpEntity entity = null;
				if (formData != null)
					entity = new UrlEncodedFormEntity(formData);
				else if (input instanceof NameValuePairs)
					entity = new UrlEncodedFormEntity((NameValuePairs)input);
				else if (input instanceof HttpEntity)
					entity = (HttpEntity)input;
				else if (input instanceof Reader)
					entity = new StringEntity(IOUtils.read((Reader)input));
				else if (input instanceof InputStream)
					entity = new InputStreamEntity((InputStream)input);
				else if (serializer != null)
					entity = new RestRequestEntity(input, serializer);
				else
					entity = new StringEntity(getBeanContext().getClassMetaForObject(input).toString(input));

				if (retries > 1 && ! entity.isRepeatable())
					throw new RestCallException("Rest call set to retryable, but entity is not repeatable.");

				((HttpEntityEnclosingRequestBase)request).setEntity(entity);
			}

			int sc = 0;
			while (retries > 0) {
				retries--;
				Exception ex = null;
				try {
					response = client.execute(request);
					sc = (response == null || response.getStatusLine() == null) ? -1 : response.getStatusLine().getStatusCode();
				} catch (Exception e) {
					ex = e;
					sc = -1;
					if (response != null)
						EntityUtils.consumeQuietly(response.getEntity());
				}
				if (! retryOn.onResponse(response))
					retries = 0;
				if (retries > 0) {
					for (RestCallInterceptor rci : interceptors)
						rci.onRetry(this, sc, request, response, ex);
					request.reset();
					long w = retryInterval;
					synchronized(this) {
						wait(w);
					}
				} else if (ex != null) {
					throw ex;
				}
			}
			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(this, sc, request, response);
			if (response == null)
				throw new RestCallException("HttpClient returned a null response");
			StatusLine sl = response.getStatusLine();
			String method = request.getMethod();
			sc = sl.getStatusCode(); // Read it again in case it was changed by one of the interceptors.
			if (sc >= 400 && ! ignoreErrors)
				throw new RestCallException(sc, sl.getReasonPhrase(), method, request.getURI(), getResponseAsString())
					.setServerException(response.getFirstHeader("Exception-Name"), response.getFirstHeader("Exception-Message"), response.getFirstHeader("Exception-Trace"))
					.setHttpResponse(response);
			if ((sc == 307 || sc == 302) && allowRedirectsOnPosts && method.equalsIgnoreCase("POST")) {
				if (redirectOnPostsTries-- < 1)
					throw new RestCallException(sc, "Maximum number of redirects occurred.  Location header: " + response.getFirstHeader("Location"), method, request.getURI(), getResponseAsString());
				Header h = response.getFirstHeader("Location");
				if (h != null) {
					reset();
					request.setURI(URI.create(h.getValue()));
					retries++;  // Redirects should affect retries.
					connect();
				}
			}

		} catch (RestCallException e) {
			isFailed = true;
			try {
			close();
			} catch (RestCallException e2) { /* Ignore */ }
			throw e;
		} catch (Exception e) {
			isFailed = true;
			close();
			throw new RestCallException(e).setHttpResponse(response);
		}

		return this;
	}

	private void reset() {
		if (response != null)
			EntityUtils.consumeQuietly(response.getEntity());
		request.reset();
		isConnected = false;
		isClosed = false;
		isFailed = false;
		if (capturedResponseWriter != null)
			capturedResponseWriter.getBuffer().setLength(0);
	}

	/**
	 * Connects to the remote resource (if <code>connect()</code> hasn't already been called) and returns the HTTP
	 * response message body as a reader.
	 *
	 * <p>
	 * If an {@link Encoder} has been registered with the {@link RestClient}, then the underlying input stream will be
	 * wrapped in the encoded stream (e.g. a <code>GZIPInputStream</code>).
	 *
	 * <p>
	 * If present, automatically handles the <code>charset</code> value in the <code>Content-Type</code> response header.
	 *
	 * <p>
	 * <b>IMPORTANT:</b>  It is your responsibility to close this reader once you have finished with it.
	 *
	 * @return
	 * 	The HTTP response message body reader.
	 * 	<jk>null</jk> if response was successful but didn't contain a body (e.g. HTTP 204).
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 */
	public Reader getReader() throws IOException {
		InputStream is = getInputStream();
		if (is == null)
			return null;

		// Figure out what the charset of the response is.
		String cs = null;
		Header contentType = response.getLastHeader("Content-Type");
		String ct = contentType == null ? null : contentType.getValue();

		// First look for "charset=" in Content-Type header of response.
		if (ct != null && ct.contains("charset="))
			cs = ct.substring(ct.indexOf("charset=")+8).trim();

		if (cs == null)
			cs = "UTF-8";

		if (writers.size() > 0) {
			try (Reader isr = new InputStreamReader(is, cs)) {
				StringWriter sw = new StringWriter();
				writers.add(sw, true);
				IOPipe.create(isr, writers).byLines(byLines).run();
				return new StringReader(sw.toString());
			}
		}

		return new InputStreamReader(is, cs);
	}

	/**
	 * Returns the response text as a string if {@link #captureResponse()} was called on this object.
	 *
	 * <p>
	 * Note that while similar to {@link #getResponseAsString()}, this method can be called multiple times to retrieve
	 * the response text multiple times.
	 *
	 * <p>
	 * Note that this method returns <jk>null</jk> if you have not called one of the methods that cause the response to
	 * be processed.  (e.g. {@link #run()}, {@link #getResponse()}, {@link #getResponseAsString()}.
	 *
	 * @return The captured response, or <jk>null</jk> if {@link #captureResponse()} has not been called.
	 * @throws IllegalStateException If trying to call this method before the response is consumed.
	 */
	public String getCapturedResponse() {
		if (! isClosed)
			throw new IllegalStateException("This method cannot be called until the response has been consumed.");
		if (capturedResponse == null && capturedResponseWriter != null && capturedResponseWriter.getBuffer().length() > 0)
			capturedResponse = capturedResponseWriter.toString();
		return capturedResponse;
	}

	/**
	 * Returns the value of the <code>Content-Length</code> header.
	 *
	 * @return The value of the <code>Content-Length</code> header, or <code>-1</code> if header is not present.
	 * @throws IOException
	 */
	public int getContentLength() throws IOException {
		connect();
		Header h = response.getLastHeader("Content-Length");
		if (h == null)
			return -1;
		long l = Long.parseLong(h.getValue());
		if (l > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return (int)l;
	}

	/**
	 * Connects to the remote resource (if <code>connect()</code> hasn't already been called) and returns the HTTP
	 * response message body as an input stream.
	 *
	 * <p>
	 * If an {@link Encoder} has been registered with the {@link RestClient}, then the underlying input stream will be
	 * wrapped in the encoded stream (e.g. a <code>GZIPInputStream</code>).
	 *
	 * <p>
	 * <b>IMPORTANT:</b>  It is your responsibility to close this reader once you have finished with it.
	 *
	 * @return
	 * 	The HTTP response message body input stream. <jk>null</jk> if response was successful but didn't contain
	 * 	a body (e.g. HTTP 204).
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 * @throws IllegalStateException If an attempt is made to read the response more than once.
	 */
	@SuppressWarnings("resource")
	public InputStream getInputStream() throws IOException {
		if (isClosed)
			throw new IllegalStateException("Method cannot be called.  Response has already been consumed.");
		connect();
		if (response == null)
			throw new RestCallException("Response was null");
		if (response.getEntity() == null)  // HTTP 204 results in no content.
			return null;
		InputStream is = response.getEntity().getContent();

		if (outputStreams.size() > 0) {
			ByteArrayInOutStream baios = new ByteArrayInOutStream();
			outputStreams.add(baios, true);
			IOPipe.create(is, baios).run();
			is.close();
			return baios.getInputStream();
		}
		return is;
	}

	/**
	 * Connects to the remote resource (if {@code connect()} hasn't already been called) and returns the HTTP response
	 * message body as plain text.
	 *
	 * <p>
	 * The response entity is discarded unless one of the pipe methods have been specified to pipe the output to an
	 * output stream or writer.
	 *
	 * @return The response as a string.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 */
	public String getResponseAsString() throws IOException {
		try (Reader r = getReader()) {
			return read(r).toString();
		} catch (IOException e) {
			isFailed = true;
			throw e;
		} finally {
			close();
		}
	}

	/**
	 * Connects to the remote resource (if {@code connect()} hasn't already been called) and returns the value of
	 * an HTTP header on the response.
	 *
	 * <p>
	 * Useful if you're only interested in a particular header value from the response and not the body of the response.
	 *
	 * <p>
	 * The response entity is discarded unless one of the pipe methods have been specified to pipe the output to an
	 * output stream or writer.
	 *
	 * @param name The header name.
	 * @return The response header as a string, or <jk>null</jk> if the header was not found.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 */
	public String getResponseHeader(String name) throws IOException {
		try {
			HttpResponse r = getResponse();
			Header h = r.getFirstHeader(name);
			return h == null ? null : h.getValue();
		} catch (IOException e) {
			isFailed = true;
			throw e;
		} finally {
			close();
		}
	}

	/**
	 * Connects to the remote resource (if {@code connect()} hasn't already been called) and returns the HTTP response code.
	 *
	 * <p>
	 * Useful if you're only interested in the status code and not the body of the response.
	 *
	 * <p>
	 * The response entity is discarded unless one of the pipe methods have been specified to pipe the output to an
	 * output stream or writer.
	 *
	 * @return The response code.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 * @throws IOException If an exception occurred while streaming was already occurring.
	 */
	public int getResponseCode() throws IOException {
		return run();
	}

	/**
	 * Same as {@link #getResponse(Class)} but allows you to run the call asynchronously.
	 *
	 * @return The response as a string.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public Future<String> getResponseAsStringFuture() throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<String>() {
				@Override /* Callable */
				public String call() throws Exception {
					return getResponseAsString();
				}
			}
		);
	}

	/**
	 * Same as {@link #getResponse(Type, Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = restClient.doGet(url).getResponse(String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = restClient.doGet(url).getResponse(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] ba = restClient.doGet(url).getResponse(MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = restClient.doGet(url).getResponse(TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		You can also specify any of the following types:
	 * 		<ul>
	 * 			<li>{@link HttpResponse} - Returns the raw <code>HttpResponse</code> returned by the inner <code>HttpClient</code>.
	 * 			<li>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 		</ul>
	 * </ul>
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #getResponse(Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a connection error occurred.
	 */
	public <T> T getResponse(Class<T> type) throws IOException, ParseException {
		BeanContext bc = parser;
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return getResponseInner(null, null, bc.getClassMeta(type));
	}

	/**
	 * Same as {@link #getResponse(Class)} but allows you to run the call asynchronously.
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #getResponse(Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> getResponseFuture(final Class<T> type) throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return getResponse(type);
				}
			}
		);
	}

	/**
	 * Parses HTTP body into the specified object type.
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List l = restClient.doGet(url).getResponse(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map m = restClient.doGet(url).getResponse(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map m = restClient.doGet(url).getResponse(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <code>Collection</code> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <code>Map</code> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * TODO - Describe allowed POJOs when parser is not defined.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Use the {@link #getResponse(Class)} method instead if you don't need a parameterized map/collection.
	 * 	<li>
	 * 		You can also specify any of the following types:
	 * 		<ul>
	 * 			<li>{@link HttpResponse} - Returns the raw <code>HttpResponse</code> returned by the inner <code>HttpClient</code>.
	 * 			<li>{@link Reader} - Returns access to the raw reader of the response.
	 * 			<li>{@link InputStream} - Returns access to the raw input stream of the response.
	 * 		</ul>
	 * </ul>
	 *
	 * @param <T> The class type of the object to create.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a connection error occurred.
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	public <T> T getResponse(Type type, Type...args) throws IOException, ParseException {
		BeanContext bc = parser;
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return (T)getResponseInner(null, null, bc.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #getResponse(Type, Type...)} but allows you to specify a part parser to use for parsing the response.
	 *
	 * @param <T> The class type of the object to create.
	 * @param partParser
	 * 	The part parser.
	 * 	<br>Can be <jk>null</jk>.
	 * @param schema
	 * 	The schema information about the body of the response.
	 * 	<br>Can be <jk>null</jk>.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws IOException If a connection error occurred.
	 * @see BeanSession#getClassMeta(Class) for argument syntax for maps and collections.
	 */
	public <T> T getResponse(HttpPartParser partParser, HttpPartSchema schema, Type type, Type...args) throws IOException, ParseException {
		BeanContext bc = parser;
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return (T)getResponseInner(partParser, schema, bc.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #getResponse(Class)} but allows you to run the call asynchronously.
	 *
	 * @param <T>
	 * 	The class type of the object being created.
	 * 	See {@link #getResponse(Type, Type...)} for details.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws RestCallException If the executor service was not defined.
	 * @see
	 * 	RestClientBuilder#executorService(ExecutorService, boolean) for defining the executor service for creating
	 * 	{@link Future Futures}.
	 */
	public <T> Future<T> getResponseFuture(final Type type, final Type...args) throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<T>() {
				@Override /* Callable */
				public T call() throws Exception {
					return getResponse(type, args);
				}
			}
		);
	}

	/**
	 * Parses the output from the connection into the specified type and then wraps that in a {@link PojoRest}.
	 *
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @param innerType The class type of the POJO being wrapped.
	 * @return The parsed output wrapped in a {@link PojoRest}.
	 * @throws IOException If a connection error occurred.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed for the <code>Content-Type</code> header.
	 */
	public PojoRest getResponsePojoRest(Class<?> innerType) throws IOException, ParseException {
		return new PojoRest(getResponse(innerType));
	}

	/**
	 * Converts the output from the connection into an {@link ObjectMap} and then wraps that in a {@link PojoRest}.
	 *
	 * <p>
	 * Useful if you want to quickly retrieve a single value from inside of a larger JSON document.
	 *
	 * @return The parsed output wrapped in a {@link PojoRest}.
	 * @throws IOException If a connection error occurred.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed for the <code>Content-Type</code> header.
	 */
	public PojoRest getResponsePojoRest() throws IOException, ParseException {
		return getResponsePojoRest(ObjectMap.class);
	}

	<T> T getResponseInner(HttpPartParser partParser, HttpPartSchema schema, ClassMeta<T> type) throws IOException, ParseException {
		try {
			if (partParser == null)
				partParser = this.partParser;

			Class<?> ic = type.getInnerClass();

			if (ic.equals(HttpResponse.class))
				return (T)response;
			if (ic.equals(Reader.class))
				return (T)getReader();
			if (ic.equals(InputStream.class))
				return (T)getInputStream();

			connect();
			Header h = response.getFirstHeader("Content-Type");
			MediaType mt = MediaType.forString(h == null ? null : h.getValue());

			if ((isEmpty(mt) || mt.toString().equals("text/plain"))) {
				if (type.hasStringTransform())
					return type.getStringTransform().transform(getResponseAsString());
				if (partParser != null)
					return partParser.createSession(null).parse(HttpPartType.BODY, schema, getResponseAsString(), type);
			}

			if (parser != null) {
				try (Closeable in = parser.isReaderParser() ? getReader() : getInputStream()) {
					return parser.parse(in, type);
				}
			}

			if (type.hasReaderTransform())
				return type.getReaderTransform().transform(getReader());

			if (type.hasInputStreamTransform())
				return type.getInputStreamTransform().transform(getInputStream());

			throw new ParseException(
				"Unsupported media-type in request header ''Content-Type'': ''{0}''\n\tSupported media-types: {1}",
				getResponseHeader("Content-Type"), parser == null ? null : parser.getMediaTypes()
			);

		} catch (ParseException e) {
			isFailed = true;
			throw e;
		} catch (IOException e) {
			isFailed = true;
			throw e;
		} finally {
			close();
		}
	}

	BeanContext getBeanContext() {
		BeanContext bc = parser;
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return bc;
	}

	/**
	 * Returns access to the {@link HttpUriRequest} passed to {@link HttpClient#execute(HttpUriRequest)}.
	 *
	 * @return The {@link HttpUriRequest} object.
	 */
	public HttpUriRequest getRequest() {
		return request;
	}

	/**
	 * Returns access to the {@link HttpResponse} returned by {@link HttpClient#execute(HttpUriRequest)}.
	 *
	 * <p>
	 * Returns <jk>null</jk> if {@link #connect()} has not yet been called.
	 *
	 * @return The HTTP response object.
	 * @throws IOException
	 */
	public HttpResponse getResponse() throws IOException {
		connect();
		return response;
	}

	/**
	 * Shortcut for calling <code>getRequest().setHeader(header)</code>
	 *
	 * @param header The header to set on the request.
	 * @return This object (for method chaining).
	 */
	public RestCall header(Header header) {
		request.setHeader(header);
		return this;
	}

	/** Use close() */
	@Deprecated
	public void consumeResponse() {
		if (response != null)
			EntityUtils.consumeQuietly(response.getEntity());
	}

	/**
	 * Cleans up this HTTP call.
	 *
	 * @throws RestCallException Can be thrown by one of the {@link RestCallInterceptor#onClose(RestCall)} calls.
	 */
	@Override /* Closeable */
	public void close() throws RestCallException {
		if (response != null)
			EntityUtils.consumeQuietly(response.getEntity());
		isClosed = true;
		if (! isFailed)
			for (RestCallInterceptor r : interceptors)
				r.onClose(this);
	}

	/**
	 * Adds a {@link RestCallLogger} to the list of interceptors on this class.
	 *
	 * @param level The log level to log events at.
	 * @param log The logger.
	 * @return This object (for method chaining).
	 */
	public RestCall logTo(Level level, Logger log) {
		interceptor(new RestCallLogger(level, log));
		return this;
	}

	/**
	 * Sets <code>Debug: value</code> header on this request.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException
	 */
	public RestCall debug() throws RestCallException {
		header("Debug", true);
		return this;
	}
}
