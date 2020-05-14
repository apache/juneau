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
package org.apache.juneau.rest.client2;

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.AddFlag.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.client2.RestClientUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.concurrent.*;
import org.apache.http.entity.*;
import org.apache.http.entity.ContentType;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;

/**
 * Represents a request to a remote REST resource.
 *
 * <p>
 * Instances of this class are created by the various creator methods on the {@link RestClient} class.
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RestClient}
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
public final class RestRequest extends BeanSession implements HttpUriRequest, Configurable {

	private static final ContentType TEXT_PLAIN = ContentType.create("text/plain");

	private final RestClient client;                       // The client that created this call.
	private final HttpRequestBase request;                 // The request.
	private RestResponse response;                         // The response.
	List<RestCallInterceptor> interceptors = new ArrayList<>();   // Used for intercepting and altering requests.

	private boolean ignoreErrors;

	private Object input;
	private boolean hasInput;                              // input() was called, even if it's setting 'null'.
	private Serializer serializer;
	private Parser parser;
	private HttpPartSerializerSession partSerializer;
	private HttpPartSchema requestBodySchema;
	private URIBuilder uriBuilder;
	private NameValuePairs formData;
	private Predicate<Integer> errorCodes;
	private HttpHost target;
	private HttpContext context;

	/**
	 * Constructs a REST call with the specified method name.
	 *
	 * @param client The client that created this request.
	 * @param request The wrapped Apache HTTP client request object.
	 * @param uri The URI for this call.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected RestRequest(RestClient client, HttpRequestBase request, URI uri) throws RestCallException {
		super(client, BeanSessionArgs.DEFAULT);
		this.client = client;
		this.request = request;
		this.errorCodes = client.errorCodes;
		this.partSerializer = client.getPartSerializerSession();
		this.uriBuilder = new URIBuilder(uri);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Configuration
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Specifies the serializer to use on the request body.
	 *
	 * <p>
	 * Overrides the serializers specified on the {@link RestClient}.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not set on the request, it will be set to the media type of this serializer.
	 *
	 * @param serializer The serializer used to serialize POJOs to the body of the HTTP request.
	 * @return This object (for method chaining).
	 */
	public RestRequest serializer(Serializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Specifies the parser to use on the response body.
	 *
	 * <p>
	 * Overrides the parsers specified on the {@link RestClient}.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not set on the request, it will be set to the media type of this parser.
	 *
	 * @param parser The parser used to parse POJOs from the body of the HTTP response.
	 * @return This object (for method chaining).
	 */
	public RestRequest parser(Parser parser) {
		this.parser = parser;
		return this;
	}

	/**
	 * Allows you to override what status codes are considered error codes that would result in a {@link RestCallException}.
	 *
	 * <p>
	 * The default error code predicate is: <code>x -&gt; x &gt;= 400</code>.
	 *
	 * @param value The new predicate for calculating error codes.
	 * @return This object (for method chaining).
	 */
	public RestRequest errorCodes(Predicate<Integer> value) {
		this.errorCodes = value;
		return this;
	}

	/**
	 * Add one or more interceptors for this call only.
	 *
	 * @param interceptors The interceptors to add to this call.
	 * @return This object (for method chaining).
	 * @throws RestCallException If init method on interceptor threw an exception.
	 */
	public RestRequest interceptors(RestCallInterceptor...interceptors) throws RestCallException {
		for (RestCallInterceptor i : interceptors) {
			this.interceptors.add(i);
			try {
				i.onInit(this);
			} catch (Exception e) {
				throw RestCallException.create(e);
			}
		}
		return this;
	}

	/**
	 * Prevent {@link RestCallException RestCallExceptions} from being thrown when HTTP status 400+ is encountered.
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest ignoreErrors() {
		this.ignoreErrors = true;
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
	public RestRequest requestConfig(RequestConfig config) {
		setConfig(config);
		return this;
	}

	/**
	 * Sets <c>Debug: value</c> header on this request.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest debug() throws RestCallException {
		header("Debug", true);
		return this;
	}

	/**
	 * Specifies the target host for the request.
	 *
	 * @param target The target host for the request.
	 * 	Implementations may accept <jk>null</jk> if they can still determine a route, for example to a default
	 * 	target or by inspecting the request.
	 * @return This object (for method chaining).
	 */
	public RestRequest target(HttpHost target) {
		this.target = target;
		return this;
	}

	/**
	 * Override the context to use for the execution.
	 *
	 * @param context The context to use for the execution, or <jk>null</jk> to use the default context.
	 * @return This object (for method chaining).
	 */
	public RestRequest context(HttpContext context) {
		this.context = context;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// URI
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the URI for this request.
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
	 * 	The URL of the remote REST resource.
	 * 	<br>This overrides the URI passed in from the client.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>{@link URIBuilder}
	 * 		<li class='jc'>{@link URI}
	 * 		<li class='jc'>{@link URL}
	 * 		<li class='jc'>{@link String}
	 * 		<li class='jc'>{@link Object} - Converted to <c>String</c> using <c>toString()</c>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid URI syntax detected.
	 */
	public RestRequest uri(Object uri) throws RestCallException {
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
	public RestRequest scheme(String scheme) {
		uriBuilder.setScheme(scheme);
		return this;
	}

	/**
	 * Sets the URI host.
	 *
	 * @param host The new URI host.
	 * @return This object (for method chaining).
	 */
	public RestRequest host(String host) {
		uriBuilder.setHost(host);
		return this;
	}

	/**
	 * Sets the URI port.
	 *
	 * @param port The new URI port.
	 * @return This object (for method chaining).
	 */
	public RestRequest port(int port) {
		uriBuilder.setPort(port);
		return this;
	}

	/**
	 * Sets the URI user info.
	 *
	 * @param userInfo The new URI user info.
	 * @return This object (for method chaining).
	 */
	public RestRequest userInfo(String userInfo) {
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
	public RestRequest userInfo(String username, String password) {
		uriBuilder.setUserInfo(username, password);
		return this;
	}

	/**
	 * Sets the URI fragment.
	 *
	 * @param fragment The URI fragment.  The value is expected to be unescaped and may contain non ASCII characters.
	 * @return This object (for method chaining).
	 */
	public RestRequest fragment(String fragment) {
		uriBuilder.setFragment(fragment);
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Path
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Error occurred.
	 */
	@SuppressWarnings("unchecked")
	public RestRequest path(String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			path(new SerializedNameValuePair(name, value, PATH, serializer, schema, false));
		} else if (value instanceof NameValuePairs) {
			for (NameValuePair p : (NameValuePairs)value)
				path(p);
		} else if (value instanceof Map) {
			for (Map.Entry<String,Object> p : ((Map<String,Object>) value).entrySet()) {
				String n = p.getKey();
				Object v = p.getValue();
				HttpPartSchema s = schema == null ? null : schema.getProperty(n);
				path(new SerializedNameValuePair(n, v, PATH, serializer, s, false));
			}
		} else if (isBean(value)) {
			return path(name, toBeanMap(value), serializer, schema);
		} else if (value != null) {
			throw new RestCallException("Invalid name ''{0}'' passed to path(name,value) for data type ''{1}''", name, className(value));
		}
		return this;
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(String name, Object value) throws RestCallException {
		return path(name, value, null, null);
	}

	/**
	 * Replaces a path parameter of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<jk>new</jk> BasicNameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param param The path parameter.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(NameValuePair param) throws RestCallException {
		String path = uriBuilder.getPath();
		String name = param.getName(), value = param.getValue();
		String var = "{" + name + "}";
		if (path.indexOf(var) == -1 && ! name.equals("/*"))
			throw new RestCallException("Path variable {"+name+"} was not found in path.");
		String p = null;
		if (name.equals("/*"))
			p = path.replaceAll("\\/\\*$", value);
		else
			p = path.replace(var, String.valueOf(value));
		uriBuilder.setPath(p);
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(OMap.<jsm>of</js>(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The path parameters.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(OMap params) throws RestCallException {
		return path((Map<String,Object>)params);
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(AMap.<jsm>create</jsm>().append(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The path parameters.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(Map<String,Object> params) throws RestCallException {
		for (Map.Entry<String,Object> e : params.entrySet())
			path(e.getKey(), e.getValue(), null, null);
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<jk>new</jk> NameValuePairs(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The path parameters.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(NameValuePairs params) throws RestCallException {
		for (NameValuePair p : params)
			path(p);
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<jk>new</jk> BasicNameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The path parameters.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(NameValuePair...params) throws RestCallException {
		for (NameValuePair p : params)
			path(p);
		return this;
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL using a bean with key/value properties.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}"</js>)
	 * 		.path(<jk>new</jk> MyBean())
	 * 		.run();
	 * </p>
	 *
	 * @param bean The path bean.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest path(Object bean) throws RestCallException {
		return path(toBeanMap(bean));
	}

	/**
	 * Replaces path parameters of the form <js>"{name}"</js> in the URL using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<js>"/{foo}/{bar}"</js>)
	 * 		.path(<js>"foo"</js>,<js>"val1"</js>,<js>"bar"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The path key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest pathPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into path(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			path(new SerializedNameValuePair(stringify(pairs[i]), pairs[i+1], PATH, partSerializer, null, false));
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Query
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a query parameter on the URI.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("unchecked")
	public RestRequest query(EnumSet<AddFlag> flags, String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerQuery(flags, toQuery(flags, name, value, serializer, schema));
		} else if (value instanceof NameValuePairs) {
			innerQuery(flags, AList.of((NameValuePairs)value));
		} else if (value instanceof Map) {
			innerQuery(flags, toQuery(flags, (Map<String,Object>)value, serializer, schema));
		} else if (isBean(value)) {
			query(flags, name, toBeanMap(value), serializer, schema);
		} else if (value instanceof Reader || value instanceof InputStream || value instanceof CharSequence) {
			queryCustom(value);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to query() for data type ''{1}''", name, className(value));
		}
		return this;
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(String name, Object value) throws RestCallException {
		return query(DEFAULT_FLAGS, name, value, partSerializer, null);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(EnumSet<AddFlag> flags, String name, Object value) throws RestCallException {
		return query(flags, name, value, partSerializer, null);
	}

	/**
	 * Sets multiple parameters on the query string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<jk>new</jk> BasicNameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest query(Object...params) throws RestCallException {
		return query(DEFAULT_FLAGS, params);
	}

	/**
	 * Sets multiple parameters on the query string.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),<jk>new</jk> BasicNameValuePair(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest query(EnumSet<AddFlag> flags, Object...params) throws RestCallException {
		List<NameValuePair> l = new ArrayList<>();
		boolean skipIfEmpty = flags.contains(SKIP_IF_EMPTY);
		for (Object o : params) {
			if (o instanceof NameValuePair) {
				l.add((NameValuePair)o);
			} else if (o instanceof NameValuePairs) {
				l.addAll((NameValuePairs)o);
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), QUERY, partSerializer, null, skipIfEmpty));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), QUERY, partSerializer, null, skipIfEmpty));
			} else if (o instanceof Reader || o instanceof InputStream  || o instanceof CharSequence) {
				queryCustom(o);
			} else {
				throw new RestCallException("Invalid type passed to query(): " + o.getClass().getName());
			}
		}
		return innerQuery(flags, l);
	}

	/**
	 * Adds query parameters to the URI query using free-form key/value pairs..
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.queryPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The query key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queryPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into query(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			query(new SerializedNameValuePair(stringify(pairs[i]), pairs[i+1], QUERY, partSerializer, null, false));
		return this;
	}

	/**
	 * Adds form-data parameters as the entire body of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.customQuery(<js>"key1=val1&key2=val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param value The parameter value.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link CharSequence}
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded query.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest queryCustom(Object value) throws RestCallException {
		try {
			String q = null;
			if (value instanceof Reader)
				q = IOUtils.read((Reader)value);
			else if (value instanceof InputStream)
				q = IOUtils.read((InputStream)value);
			else
				q = value.toString();  // Works for NameValuePairs.
			uriBuilder.setCustomQuery(q);
		} catch (IOException e) {
			throw new RestCallException(e);
		}
		return this;
	}

	private RestRequest innerQuery(EnumSet<AddFlag> flags, NameValuePair param) {
		return innerQuery(flags, AList.of(param));
	}

	private RestRequest innerQuery(EnumSet<AddFlag> flags, List<NameValuePair> params) {
		flags = AddFlag.orDefault(flags);
		params.removeIf(x -> x == null || x.getValue() == null);
		if (flags.contains(REPLACE)) {
			List<NameValuePair> l = uriBuilder.getQueryParams();
			for (NameValuePair p : params)
				for (Iterator<NameValuePair> i = l.iterator(); i.hasNext();)
					if (i.next().getName().equals(p.getName()))
						i.remove();
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			l.addAll(params);
			uriBuilder.setParameters(l);
		} else if (flags.contains(PREPEND)) {
			List<NameValuePair> l = uriBuilder.getQueryParams();
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			l.addAll(0, params);
			uriBuilder.setParameters(l);
		} else {
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			uriBuilder.addParameters(params);
		}
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Form data
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence} / {@link HttpEntity}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("unchecked")
	public RestRequest formData(EnumSet<AddFlag> flags, String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerFormData(flags, toQuery(flags, name, value, serializer, schema));
		} else if (value instanceof NameValuePairs) {
			innerFormData(flags, AList.of((NameValuePairs)value));
		} else if (value instanceof Map) {
			innerFormData(flags, toQuery(flags, (Map<String,Object>)value, serializer, schema));
		} else if (isBean(value)) {
			formData(flags, name, toBeanMap(value), serializer, schema);
		} else if (value instanceof Reader || value instanceof InputStream || value instanceof CharSequence || value instanceof HttpEntity) {
			formDataCustom(value);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to formData() for data type ''{1}''", name, className(value));
		}
		return this;
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(String name, Object value) throws RestCallException {
		return formData(DEFAULT_FLAGS, name, value, partSerializer, null);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>), <js>"foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The parameter name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of parameters.
	 * 	</ul>
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>For single value parameters:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value parameters:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 			<li>{@link Reader} / {@link InputStream} / {@link CharSequence}
	 * 			<ul>
	 * 				<li>Sets the entire query string to the contents of the input.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(EnumSet<AddFlag> flags, String name, Object value) throws RestCallException {
		return formData(flags, name, value, partSerializer, null);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<jk>new</jk> BasicNameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formData(Object...params) throws RestCallException {
		return formData(DEFAULT_FLAGS, params);
	}

	/**
	 * Adds a form-data parameter to the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>), <jk>new</jk> BasicNameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param params The parameters to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest formData(EnumSet<AddFlag> flags, Object...params) throws RestCallException {
		List<NameValuePair> l = new ArrayList<>();
		boolean skipIfEmpty = flags.contains(SKIP_IF_EMPTY);
		for (Object o : params) {
			if (o instanceof NameValuePair) {
				l.add((NameValuePair)o);
			} else if (o instanceof NameValuePairs) {
				l.addAll((NameValuePairs)o);
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), FORMDATA, partSerializer, null, skipIfEmpty));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(new SerializedNameValuePair(stringify(e.getKey()), e.getValue(), FORMDATA, partSerializer, null, skipIfEmpty));
			} else if (o instanceof Reader || o instanceof InputStream  || o instanceof CharSequence || o instanceof HttpEntity) {
				formDataCustom(o);
			} else {
				throw new RestCallException("Invalid type passed to formData(): " + o.getClass().getName());
			}
		}
		return innerFormData(flags, l);
	}

	/**
	 * Adds form-data parameters to the request body using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formDataPairs(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The form-data key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDataPairs(Object...pairs) throws RestCallException {
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into formData(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			formData(new SerializedNameValuePair(stringify(pairs[i]), pairs[i+1], FORMDATA, partSerializer, null, false));
		return this;
	}

	/**
	 * Adds form-data parameters as the entire body of the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.customFormData(<js>"key1=val1&key2=val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param value The parameter value.
	 * 	<br>Can be any of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link CharSequence}
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest formDataCustom(Object value) throws RestCallException {
		contentType("application/x-www-form-urlencoded");
		body(value instanceof CharSequence ? new StringReader(value.toString()) : value);
		return this;
	}

	private RestRequest innerFormData(EnumSet<AddFlag> flags, NameValuePair param) {
		return innerFormData(flags, AList.of(param));
	}

	private RestRequest innerFormData(EnumSet<AddFlag> flags, List<NameValuePair> params) {
		flags = AddFlag.orDefault(flags);
		params.removeIf(x -> x == null|| x.getValue() == null);
		if (formData == null)
			formData = new NameValuePairs();
		if (flags.contains(REPLACE)) {
			for (NameValuePair p : params)
				for (Iterator<NameValuePair> i = formData.iterator(); i.hasNext();)
					if (i.next().getName().equals(p.getName()))
						i.remove();
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			formData.addAll(params);
		} else if (flags.contains(PREPEND)) {
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			formData.addAll(0, params);
		} else {
			if (flags.contains(SKIP_IF_EMPTY))
				params.removeIf(x -> isEmpty(x.getValue()));
			formData.addAll(params);
		}
		return this;
	}


	//------------------------------------------------------------------------------------------------------------------
	// Request body
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the body of this request.
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT/POST/PATCH) requests.
	 * 	<br>Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestRequest body(Object input) throws RestCallException {
		this.input = input;
		this.hasInput = true;
		this.formData = null;
		return this;
	}

	/**
	 * Sets the body of this request.
	 *
	 * @param input
	 * 	The input to be sent to the REST resource (only valid for PUT/POST/PATCH) requests.
	 * 	<br>Can be of the following types:
	 * 	<ul class='spaced-list'>
	 * 		<li class='jc'>
	 * 			{@link Reader} - Raw contents of {@code Reader} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link InputStream} - Raw contents of {@code InputStream} will be serialized to remote resource.
	 * 		<li class='jc'>
	 * 			{@link ReaderResource} - Raw contents of {@code Reader} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link StreamResource} - Raw contents of {@code InputStream} will be serialized to remote resource.  Additional headers and media type will be set on request.
	 * 		<li class='jc'>
	 * 			{@link Object} - POJO to be converted to text using the {@link Serializer} registered with the
	 * 			{@link RestClient}.
	 * 		<li class='jc'>
	 * 			{@link HttpEntity} - Bypass Juneau serialization and pass HttpEntity directly to HttpClient.
	 * 		<li class='jc'>
	 * 			{@link NameValuePairs} - Converted to a URL-encoded FORM post.
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException If a retry was attempted, but the entity was not repeatable.
	 */
	public RestRequest body(Object input, HttpPartSchema schema) throws RestCallException {
		this.input = input;
		this.hasInput = true;
		this.formData = null;
		this.requestBodySchema = schema;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a header on the request.
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @param serializer The serializer to use for serializing the value to a string.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
	 * 	</ul>
	 * @param schema The schema object that defines the format of the output.
	 * 	<ul>
	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("unchecked")
	public RestRequest header(EnumSet<AddFlag> flags, String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema) throws RestCallException {
		serializer = (serializer == null ? partSerializer : serializer);
		flags = AddFlag.orDefault(flags);
		boolean isMulti = isEmpty(name) || "*".equals(name) || value instanceof NameValuePairs;
		if (! isMulti) {
			innerHeader(flags, toHeader(flags, name, value, serializer, schema));
		} else if (value instanceof NameValuePairs) {
			innerHeaders(flags, toHeaders((NameValuePairs)value));
		} else if (value instanceof Map) {
			innerHeaders(flags, toHeaders(flags, (Map<String,Object>)value, serializer, schema));
		} else if (isBean(value)) {
			return header(flags, name, toBeanMap(value), serializer, schema);
		} else {
			throw new RestCallException("Invalid name ''{0}'' passed to header(name,value,skipIfEmpty) for data type ''{1}''", name, className(value));
		}
		return this;
	}

	/**
	 * Appends a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(String name, Object value) throws RestCallException {
		return header(DEFAULT_FLAGS, name, value, partSerializer, null);
	}

	/**
	 * Sets a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),<js>"Foo"</js>, <js>"bar"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 *	 	<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param name The header name.
	 * 	<ul>
	 * 		<li>If the name is <js>"*"</js>, the value is assumed to be a collection of headers.
	 * 	</ul>
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>For single value headers:
	 * 		<ul>
	 * 			<li>Can be any POJO.
	 * 			<li>Converted to a string using the specified part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>For multi-value headers:
	 * 		<ul>
	 * 			<li>{@link Map} / {@link OMap} / bean
	 * 			<ul>
	 * 				<li>Values can be any POJO.
	 * 				<li>Values converted to a string using the configured part serializer.
	 * 				<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 			</ul>
	 * 			<li>{@link NameValuePairs}
	 * 			<ul>
	 * 				<li>Values converted directly to strings.
	 * 			</ul>
	 * 		</ul>
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, String name, Object value) throws RestCallException {
		return header(flags, name, value, partSerializer, null);
	}

	/**
	 * Appends a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param header The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(Object header) throws RestCallException {
		return header(DEFAULT_FLAGS, header);
	}

	/**
	 * Sets a header on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.header(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param header The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest header(EnumSet<AddFlag> flags, Object header) throws RestCallException {
		if (header == null)
			return this;
		Header h = toHeader(header);
		if (h == null)
			throw new RestCallException("Invalid type passed to header(): " + header.getClass().getName());
		return innerHeader(flags, h);
	}

	/**
	 * Appends multiple headers to the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.headers(<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param headers The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest headers(Object...headers) throws RestCallException {
		return headers(DEFAULT_FLAGS, headers);
	}

	/**
	 * Sets multiple headers on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.headers(EnumSet.<jsm>of</jsm>(<jsf>REPLACE</jsf>,<jsf>SKIP_IF_EMPTY</jsf>),<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param flags Instructions on how to add this parameter.
	 * 	<ul>
	 * 		<li>{@link AddFlag#APPEND APPEND} (default) - Append to end.
	 * 		<li>{@link AddFlag#PREPEND PREPEND} - Prepend to beginning.
	 * 		<li>{@link AddFlag#REPLACE REPLACE} - Delete any existing with same name and append to end.
	 * 		<li>{@link AddFlag#SKIP_IF_EMPTY} - Don't add if value is an empty string.
	 * 	</ul>
	 * @param headers The header to set.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li>{@link Header} (including any subclasses such as {@link Accept})
	 * 		<li>{@link NameValuePair}
	 * 		<li>{@link Map} / {@link OMap} / bean
	 * 		<ul>
	 * 			<li>Values can be any POJO.
	 * 			<li>Values converted to a string using the configured part serializer.
	 * 			<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 		</ul>
	 * 		<li>{@link NameValuePairs}
	 * 		<ul>
	 * 			<li>Values converted directly to strings.
	 * 		</ul>
	 * 		<li><jk>null</jk> - Will be a no-op.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	@SuppressWarnings("rawtypes")
	public RestRequest headers(EnumSet<AddFlag> flags, Object...headers) throws RestCallException {
		List<Header> l = new ArrayList<>();
		for (Object o : headers) {
			if (o instanceof Header || o instanceof NameValuePair) {
				l.add(toHeader(o));
			} else if (o instanceof NameValuePairs) {
				for (NameValuePair p : (NameValuePairs)o)
					l.add(toHeader(p));
			} else if (o instanceof Map) {
				Map m = (Map)o;
				for (Map.Entry e : (Set<Map.Entry>)m.entrySet())
					l.add(new SerializedHeader(stringify(e.getKey()), e.getValue(), partSerializer, null, flags.contains(SKIP_IF_EMPTY)));
			} else if (isBean(o)) {
				for (Map.Entry<String,Object> e : toBeanMap(o).entrySet())
					l.add(new SerializedHeader(stringify(e.getKey()), e.getValue(), partSerializer, null, flags.contains(SKIP_IF_EMPTY)));
			} else {
				throw new RestCallException("Invalid type passed to header(): " + headers.getClass().getName());
			}
		}
		return innerHeaders(flags, l);
	}

	/**
	 * Appends multiple headers on the request using freeform key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.headers(<js>"Header1"</js>,<js>"val1"</js>,<js>"Header2"</js>,<js>"val2"</js>)
	 * 		.run();
	 * </p>
	 *
	 * @param pairs The header key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest headerPairs(Object...pairs) throws RestCallException {
		List<Header> l = new ArrayList<>();
		if (pairs.length % 2 != 0)
			throw new RestCallException("Odd number of parameters passed into headerPairs(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			l.add(new SerializedHeader(stringify(pairs[i]), pairs[i+1], partSerializer, null, false));
		return innerHeaders(DEFAULT_FLAGS, l);
	}

	private RestRequest innerHeader(EnumSet<AddFlag> flags, Header header) {
		return innerHeaders(flags, AList.of(header));
	}

	private RestRequest innerHeaders(EnumSet<AddFlag> flags, Collection<Header> headers) {
		flags = AddFlag.orDefault(flags);
		headers.removeIf(x -> x == null || x.getValue() == null);
		if (flags.contains(REPLACE)) {
			for (Header h : headers)
				removeHeaders(h.getName());
		} else if (flags.contains(PREPEND)) {
			for (Header h : headers) {
				for (Header h2 : getHeaders(h.getName()))
					headers.add(h2);
				removeHeaders(h.getName());
			}
		}
		for (Header h : headers) {
			if ((! flags.contains(SKIP_IF_EMPTY)) || ! isEmpty(h.getValue()))
				addHeader(h);
		}
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Specialized headers.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the value for the <c>Accept</c> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling
	 * <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest accept(Object value) throws RestCallException {
		return header("Accept", value);
	}

	/**
	 * Sets the value for the <c>Accept-Charset</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest acceptCharset(Object value) throws RestCallException {
		return header("Accept-Charset", value);
	}

	/**
	 * Sets the value for the <c>Accept-Encoding</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest acceptEncoding(Object value) throws RestCallException {
		return header("Accept-Encoding", value);
	}

	/**
	 * Sets the value for the <c>Accept-Language</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest acceptLanguage(Object value) throws RestCallException {
		return header("Accept-Language", value);
	}

	/**
	 * Sets the value for the <c>Authorization</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest authorization(Object value) throws RestCallException {
		return header("Authorization", value);
	}

	/**
	 * Sets the value for the <c>Cache-Control</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest cacheControl(Object value) throws RestCallException {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the value for the <c>Connection</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest connection(Object value) throws RestCallException {
		return header("Connection", value);
	}

	/**
	 * Sets the value for the <c>Content-Length</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest contentLength(Object value) throws RestCallException {
		return header("Content-Length", value);
	}

	/**
	 * Sets the value for the <c>Content-Type</c> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the serializer, but is overridden by calling
	 * <code>header(<js>"Content-Type"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest contentType(Object value) throws RestCallException {
		return header("Content-Type", value);
	}

	/**
	 * Sets the value for the <c>Date</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest date(Object value) throws RestCallException {
		return header("Date", value);
	}

	/**
	 * Sets the value for the <c>Expect</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest expect(Object value) throws RestCallException {
		return header("Expect", value);
	}

	/**
	 * Sets the value for the <c>Forwarded</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest forwarded(Object value) throws RestCallException {
		return header("Forwarded", value);
	}

	/**
	 * Sets the value for the <c>From</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest from(Object value) throws RestCallException {
		return header("From", value);
	}

	/**
	 * Sets the value for the <c>Host</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest host(Object value) throws RestCallException {
		return header("Host", value);
	}

	/**
	 * Sets the value for the <c>If-Match</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifMatch(Object value) throws RestCallException {
		return header("If-Match", value);
	}

	/**
	 * Sets the value for the <c>If-Modified-Since</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifModifiedSince(Object value) throws RestCallException {
		return header("If-Modified-Since", value);
	}

	/**
	 * Sets the value for the <c>If-None-Match</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifNoneMatch(Object value) throws RestCallException {
		return header("If-None-Match", value);
	}

	/**
	 * Sets the value for the <c>If-Range</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifRange(Object value) throws RestCallException {
		return header("If-Range", value);
	}

	/**
	 * Sets the value for the <c>If-Unmodified-Since</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest ifUnmodifiedSince(Object value) throws RestCallException {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <c>Max-Forwards</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest maxForwards(Object value) throws RestCallException {
		return header("Max-Forwards", value);
	}

	/**
	 * Sets the value for the <c>Origin</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest origin(Object value) throws RestCallException {
		return header("Origin", value);
	}

	/**
	 * Sets the value for the <c>Pragma</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest pragma(Object value) throws RestCallException {
		return header("Pragma", value);
	}

	/**
	 * Sets the value for the <c>Proxy-Authorization</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest proxyAuthorization(Object value) throws RestCallException {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Sets the value for the <c>Range</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest range(Object value) throws RestCallException {
		return header("Range", value);
	}

	/**
	 * Sets the value for the <c>Referer</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest referer(Object value) throws RestCallException {
		return header("Referer", value);
	}

	/**
	 * Sets the value for the <c>TE</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest te(Object value) throws RestCallException {
		return header("TE", value);
	}

	/**
	 * Sets the value for the <c>User-Agent</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest userAgent(Object value) throws RestCallException {
		return header("User-Agent", value);
	}

	/**
	 * Sets the value for the <c>Upgrade</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest upgrade(Object value) throws RestCallException {
		return header("Upgrade", value);
	}

	/**
	 * Sets the value for the <c>Via</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest via(Object value) throws RestCallException {
		return header("Via", value);
	}

	/**
	 * Sets the value for the <c>Warning</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest warning(Object value) throws RestCallException {
		return header("Warning", value);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param value The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 * @throws RestCallException Invalid input.
	 */
	public RestRequest clientVersion(Object value) throws RestCallException {
		return header("X-Client-Version", value);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Execution methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Runs this request and returns the resulting response object.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>try</jk> {
	 * 		<jk>int</jk> rc = client.get(<jsf>URL</jsf>).execute().getResponseStatus();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Calling this method multiple times will return the same original response object.
	 * 	<li>You must close the returned object if you do not consume the response or execute a method that consumes
	 * 		the response.
	 * 	<li>If you are only interested in the response code, use the {@link #complete()} method which will automatically
	 * 		consume the response so that you don't need to call {@link InputStream#close()} on the response body.
	 * </ul>
	 *
	 * @return The response object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	public RestResponse run() throws RestCallException {
		if (response != null)
			return response;

		try {
			HttpEntityEnclosingRequestBase request2 = request instanceof HttpEntityEnclosingRequestBase ? (HttpEntityEnclosingRequestBase)request : null;

			request.setURI(uriBuilder.build());

			// Pick the serializer if it hasn't been overridden.
			Header h = getLastHeader("Content-Type");
			String contentType = h == null ? null : h.getValue();
			Serializer serializer = this.serializer;
			if (serializer == null)
				serializer = client.getMatchingSerializer(contentType);
			if (contentType == null && serializer != null)
				contentType = serializer.getPrimaryMediaType().toString();

			// Pick the parser if it hasn't been overridden.
			h = getLastHeader("Accept");
			String accept = h == null ? null : h.getValue();
			Parser parser = this.parser;
			if (parser == null)
				parser = client.getMatchingParser(accept);
			if (accept == null && parser != null)
				setHeader("Accept", parser.getPrimaryMediaType().toString());

			if (hasInput || formData != null) {

				if (hasInput && formData != null)
					throw new RestCallException("Both input and form-data found on same request.");

				if (request2 == null)
					throw new RestCallException(0, "Method does not support content entity.", getMethod(), getURI(), null);

				HttpEntity entity = null;
				if (formData != null)
					entity = new UrlEncodedFormEntity(formData);
				else if (input instanceof NameValuePairs)
					entity = new UrlEncodedFormEntity((NameValuePairs)input);
				else if (input instanceof HttpEntity)
					entity = (HttpEntity)input;
				else if (input instanceof Reader)
					entity = new StringEntity(IOUtils.read((Reader)input), getRequestContentType(TEXT_PLAIN));
				else if (input instanceof InputStream)
					entity = new InputStreamEntity((InputStream)input, getRequestContentType(ContentType.APPLICATION_OCTET_STREAM));
				else if (input instanceof ReaderResource) {
					ReaderResource r = (ReaderResource)input;
					contentType(r.getMediaType());
					headers(r.getHeaders());
					entity = new StringEntity(IOUtils.read(r.getContents()), getRequestContentType(TEXT_PLAIN));
				}
				else if (input instanceof StreamResource) {
					StreamResource r = (StreamResource)input;
					contentType(r.getMediaType());
					headers(r.getHeaders());
					entity = new InputStreamEntity(r.getContents(), getRequestContentType(ContentType.APPLICATION_OCTET_STREAM));
				}
				else if (serializer != null)
					entity = new SerializedHttpEntity(input, serializer, requestBodySchema, contentType);
				else
					entity = new StringEntity(getBeanContext().getClassMetaForObject(input).toString(input), getRequestContentType(TEXT_PLAIN));

				request2.setEntity(entity);
			}

			try {
				if (request2 != null)
					response = new RestResponse(client, this, client.execute(target, request2, context), parser);
				else
					response = new RestResponse(client, this, client.execute(target, this.request, context), parser);
			} catch (Exception e) {
				throw e;
			}

			if (client.logRequests == DetailLevel.FULL)
				response.getBody().cache();

			for (RestCallInterceptor rci : interceptors)
				rci.onConnect(this, response);

			if (response.getStatusCode() == 0)
				throw new RestCallException("HttpClient returned a null response");

			String method = getMethod();
			int sc = response.getStatusCode();

			if (errorCodes.test(sc) && ! ignoreErrors) {
				throw new RestCallException(sc, response.getReasonPhrase(), method, getURI(), response.getBody().asAbbreviatedString(1000))
					.setServerException(response.getStringHeader("Exception-Name"), response.getStringHeader("Exception-Message"), response.getStringHeader("Exception-Trace"))
					.setRestResponse(response);
			}

		} catch (Exception e) {
			if (response != null)
				response.close();
			throw RestCallException.create(e).setRestResponse(response);
		}

		return this.response;
	}

	/**
	 * Same as {@link #run()} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Future&lt;RestResponse&gt; f = client.get(<jsf>URL</jsf>).runFuture();
	 * 	<jc>// Do some other stuff</jc>
	 * 	<jk>try</jk> {
	 * 		String body = f.get().getBody().asString();
	 * 		<jc>// Succeeded!</jc>
	 * 	} <jk>catch</jk> (RestCallException e) {
	 * 		<jc>// Failed!</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Use the {@link RestClientBuilder#executorService(ExecutorService, boolean)} method to customize the
	 * 		executor service used for creating {@link Future Futures}.
	 * </ul>
	 *
	 * @return The HTTP status code.
	 * @throws RestCallException If the executor service was not defined.
	 */
	public Future<RestResponse> runFuture() throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<RestResponse>() {
				@Override /* Callable */
				public RestResponse call() throws Exception {
					return run();
				}
			}
		);
	}

	/**
	 * Same as {@link #run()} but immediately calls {@link RestResponse#consume()} to clean up the response.
	 *
	 * <p>
	 * Use this method if you're only interested in the status line of the response and not the response entity.
	 * Attempts to call any of the methods on the response object that retrieve the body (e.g. {@link RestResponseBody#asReader()}
	 * will cause a {@link RestCallException} to be thrown.
	 *
	 * <ul class='notes'>
	 * 	<li>You do not need to execute {@link InputStream#close()} on the response body to consume the response.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 *  <jc>// Get the response code.
	 *  // No need to call close() on the RestResponse object.</jc>
	 *  <jk>int</jk> rc = client.get(<jsf>URL</jsf>).complete().getResponseCode();
	 * </p>
	 *
	 * @return The response object.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	public RestResponse complete() throws RestCallException {
		return run().consume();
	}

	/**
	 * Same as {@link #complete()} but allows you to run the call asynchronously.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Future&lt;RestResponse&gt; f = client.get(<jsf>URL</jsf>).completeFuture();
	 * 	<jc>// Do some other stuff</jc>
	 * 	<jk>int</jk> rc = f.get().getResponseStatus();
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Use the {@link RestClientBuilder#executorService(ExecutorService, boolean)} method to customize the
	 * 		executor service used for creating {@link Future Futures}.
	 * 	<li>You do not need to execute {@link InputStream#close()} on the response body to consume the response.
	 * </ul>
	 *
	 * @return The HTTP status code.
	 * @throws RestCallException If the executor service was not defined.
	 */
	public Future<RestResponse> completeFuture() throws RestCallException {
		return client.getExecutorService(true).submit(
			new Callable<RestResponse>() {
				@Override /* Callable */
				public RestResponse call() throws Exception {
					return complete();
				}
			}
		);
	}

	/**
	 * Returns <jk>true</jk> if this request has a body.
	 *
	 * @return <jk>true</jk> if this request has a body.
	 */
	public boolean hasHttpEntity() {
		return request instanceof HttpEntityEnclosingRequestBase;
	}

	/**
	 * Returns the body of this request.
	 *
	 * @return The body of this request, or <jk>null</jk> if it doesn't have a body.
	 */
	public HttpEntity getHttpEntity() {
		return (request instanceof HttpEntityEnclosingRequestBase ? ((HttpEntityEnclosingRequestBase)request).getEntity() : null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// HttpRequestBase pass-through methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the actual request configuration.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public RestRequest setConfig(RequestConfig value) {
		request.setConfig(value);
		return this;
	}

	/**
	 * Sets {@link Cancellable} for the ongoing operation.
	 *
	 * @param cancellable The cancellable object.
	 * @return This object (for method chaining).
	 */
	public RestRequest setCancellable(Cancellable cancellable) {
		request.setCancellable(cancellable);
		return this;
	}

	/**
	 * Sets the protocol version for this request.
	 *
	 * @param version The protocol version for this request.
	 */
	public void setProtocolVersion(ProtocolVersion version) {
		request.setProtocolVersion(version);
	}

	/**
	 * Used in combination with {@link #setCancellable(Cancellable)}.
	 */
	public void completed() {
		request.completed();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// HttpUriRequest pass-through methods.
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the HTTP method this request uses, such as GET, PUT, POST, or other.
	 *
	 * @return The HTTP method this request uses, such as GET, PUT, POST, or other.
	 */
	@Override /* HttpUriRequest */
	public String getMethod() {
		return request.getMethod();
	}

	/**
	 * Returns the original request URI.
	 *
	 * <ul class='notes'>
	 * 	<li>URI remains unchanged in the course of request execution and is not updated if the request is redirected to another location.
	 * </ul>
	 *
	 * @return The original request URI.
	 */
	@Override /* HttpUriRequest */
	public URI getURI() {
		return request.getURI();
	}

	/**
	 * Aborts this http request. Any active execution of this method should return immediately.
	 *
	 * If the request has not started, it will abort after the next execution.
	 * <br>Aborting this request will cause all subsequent executions with this request to fail.
	 */
	@Override /* HttpUriRequest */
	public void abort() throws UnsupportedOperationException {
		request.abort();
	}

	@Override /* HttpUriRequest */
	public boolean isAborted() {
		return request.isAborted();
	}

	/**
	 * Returns the request line of this request.
	 *
	 * @return The request line.
	 */
	@Override /* HttpRequest */
	public RequestLine getRequestLine() {
		return request.getRequestLine();
	}

	/**
	 * Returns the protocol version this message is compatible with.
	 *
	 * @return The protocol version.
	 */
	@Override /* HttpMessage */
	public ProtocolVersion getProtocolVersion() {
		return request.getProtocolVersion();
	}

	/**
	 * Checks if a certain header is present in this message.
	 *
	 * Header values are ignored.
	 *
	 * @param name The header name to check for.
	 * @return <jk>true</jk> if at least one header with this name is present.
	 */
	@Override /* HttpMessage */
	public boolean containsHeader(String name) {
		return request.containsHeader(name);
	}

	/**
	 * Returns all the headers with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>Headers are ordered in the sequence they will be sent over a connection.
	 *
	 * @param name The name of the headers to return.
	 * @return The headers whose name property equals name.
	 */
	@Override /* HttpMessage */
	public Header[] getHeaders(String name) {
		return request.getHeaders(name);
	}

	/**
	 * Returns the first header with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>If there is more than one matching header in the message the first element of {@link #getHeaders(String)} is returned.
	 * <br>If there is no matching header in the message <jk>null</jk> is returned.
	 *
	 * @param name The name of the header to return.
	 * @return The first header whose name property equals name or <jk>null</jk> if no such header could be found.
	 */
	@Override /* HttpMessage */
	public Header getFirstHeader(String name) {
		return request.getFirstHeader(name);
	}

	/**
	 * Returns the last header with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>If there is more than one matching header in the message the last element of {@link #getHeaders(String)} is returned.
	 * <br>If there is no matching header in the message null is returned.
	 *
	 * @param name The name of the header to return.
	 * @return The last header whose name property equals name or <jk>null</jk> if no such header could be found.
	 */
	@Override /* HttpMessage */
	public Header getLastHeader(String name) {
		return request.getLastHeader(name);
	}

	/**
	 * Returns all the headers of this message.
	 *
	 * Headers are ordered in the sequence they will be sent over a connection.
	 *
	 * @return All the headers of this message
	 */
	@Override /* HttpMessage */
	public Header[] getAllHeaders() {
		return request.getAllHeaders();
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * <ul class='notes'>
	 * 	<li>{@link #header(Object)} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param header The header to append.
	 */
	@Override /* HttpMessage */
	public void addHeader(Header header) {
		request.addHeader(header);
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * <ul class='notes'>
	 * 	<li>{@link #header(String,Object)} is an equivalent method and the preferred method for fluent-style coding.
	 * </ul>
	 *
	 * @param name The name of the header.
	 * @param value The value of the header.
	 */
	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		request.addHeader(name, value);
	}

	/**
	 * Overwrites the first header with the same name.
	 *
	 * The new header will be appended to the end of the list, if no header with the given name can be found.
	 *
	 * @param header The header to set.
	 */
	@Override /* HttpMessage */
	public void setHeader(Header header) {
		request.setHeader(header);
	}

	/**
	 * Overwrites the first header with the same name.
	 *
	 * The new header will be appended to the end of the list, if no header with the given name can be found.
	 *
	 * @param name The name of the header.
	 * @param value The value of the header.
	 */
	@Override /* HttpMessage */
	public void setHeader(String name, String value) {
		request.setHeader(name, value);
	}

	/**
	 * Overwrites all the headers in the message.
	 *
	 * @param headers The array of headers to set.
	 */
	@Override /* HttpMessage */
	public void setHeaders(Header[] headers) {
		request.setHeaders(headers);
	}

	/**
	 * Removes a header from this message.
	 *
	 * @param header The header to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeader(Header header) {
		request.removeHeader(header);
	}

	/**
	 * Removes all headers with a certain name from this message.
	 *
	 * @param name The name of the headers to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		request.removeHeaders(name);
	}

	/**
	 * Returns an iterator of all the headers.
	 *
	 * @return Iterator that returns {@link Header} objects in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return request.headerIterator();
	}

	/**
	 * Returns an iterator of the headers with a given name.
	 *
	 * @param name the name of the headers over which to iterate, or <jk>null</jk> for all headers.
	 * @return Iterator that returns {@link Header} objects with the argument name in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return request.headerIterator(name);
	}

	/**
	 * Returns the parameters effective for this message as set by {@link #setParams(HttpParams)}.
	 *
	 * @return The parameters effective for this message as set by {@link #setParams(HttpParams)}.
	 * @deprecated Use constructor parameters of configuration API provided by HttpClient.
	 */
	@Override /* HttpMessage */
	@Deprecated
	public HttpParams getParams() {
		return request.getParams();
	}

	/**
	 * Provides parameters to be used for the processing of this message.
	 *
	 * @param params The parameters.
	 * @deprecated Use constructor parameters of configuration API provided by HttpClient.
	 */
	@Override /* HttpMessage */
	@Deprecated
	public void setParams(HttpParams params) {
		request.setParams(params);
	}

	/**
	 * Returns the actual request configuration.
	 *
	 * @return The actual request configuration.
	 */
	@Override /* Configurable */
	public RequestConfig getConfig() {
		return request.getConfig();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Utility methods
	// -----------------------------------------------------------------------------------------------------------------

	private BeanContext getBeanContext() {
		BeanContext bc = serializer;
		if (bc == null)
			bc = BeanContext.DEFAULT;
		return bc;
	}

	private ContentType getRequestContentType(ContentType def) {
		Header h = request.getFirstHeader("Content-Type");
		if (h != null) {
			String s = h.getValue();
			if (! isEmpty(s))
				return ContentType.create(s);
		}
		return def;
	}

	@Override
	public OMap getProperties() {
		return super.getProperties();
	}

	private static String className(Object o) {
		return ClassInfo.of(o).getFullName();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Session */
	public OMap toMap() {
		return super.toMap()
			.a("RestCall", new DefaultFilteringOMap()
				.a("client", client)
				.a("hasInput", hasInput)
				.a("ignoreErrors", ignoreErrors)
				.a("interceptors", interceptors)
				.a("partSerializer", partSerializer)
				.a("requestBodySchema", requestBodySchema)
				.a("response", response)
				.a("serializer", serializer)
			);
	}
}
