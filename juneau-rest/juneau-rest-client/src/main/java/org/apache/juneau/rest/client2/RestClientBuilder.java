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

import static org.apache.juneau.parser.InputStreamParser.*;
import static org.apache.juneau.parser.ReaderParser.*;
import static org.apache.juneau.rest.client2.RestClient.*;
import static org.apache.juneau.BeanTraverseContext.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.serializer.OutputStreamSerializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;
import static org.apache.juneau.uon.UonSerializer.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.net.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import javax.net.ssl.*;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.*;
import org.apache.http.client.entity.*;
import org.apache.http.config.*;
import org.apache.http.conn.*;
import org.apache.http.conn.routing.*;
import org.apache.http.conn.socket.*;
import org.apache.http.conn.util.*;
import org.apache.http.cookie.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.client2.ext.*;
import org.apache.juneau.rest.client2.logging.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Builder class for the {@link RestClient} class.
 *
 * <p>
 * Instances of this class are created by the following methods:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestClient}
 * 	<ul>
 * 		<li class='jm'>{@link RestClient#create() create()} - Create from scratch.
 * 		<li class='jm'>{@link RestClient#create(Serializer,Parser) create(Serializer,Parser)} - Create from scratch using specified serializer/parser.
 * 		<li class='jm'>{@link RestClient#create(Class,Class) create(Class,Class)} - Create from scratch using specified serializer/parser classes.
 * 		<li class='jm'>{@link RestClient#builder() builder()} - Copy settings from an existing client.
 * 	</ul>
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RestClient}
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
public class RestClientBuilder extends BeanContextBuilder {

	private HttpClientBuilder httpClientBuilder;
	private CloseableHttpClient httpClient;
	private HttpClientConnectionManager httpClientConnectionManager;
	private boolean pooled;

	/**
	 * Constructor.
	 * @param ps
	 * 	Initial configuration properties for this builder.
	 * 	<br>Can be <jk>null</jk>.
	 * @param httpClientBuilder
	 * 	The HTTP client builder to use for this REST client builder.
	 * 	<br>Can be <jk>null</jk> to just call {@link #createHttpClientBuilder()} to instantiate it again.
	 */
	protected RestClientBuilder(PropertyStore ps, HttpClientBuilder httpClientBuilder) {
		super(ps);
		this.httpClientBuilder = httpClientBuilder != null ? httpClientBuilder : createHttpClientBuilder();
	}

	@Override /* ContextBuilder */
	public RestClient build() {
		return new RestClient(this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Convenience marshalling support methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Convenience method for specifying JSON as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(JsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder json() {
		return serializer(JsonSerializer.class).parser(JsonParser.class);
	}

	/**
	 * Convenience method for specifying Simple JSON as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(SimpleJsonSerializer.<jk>class</jk>).parser(JsonParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder simpleJson() {
		return serializer(SimpleJsonSerializer.class).parser(JsonParser.class);
	}

	/**
	 * Convenience method for specifying XML as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(XmlSerializer.<jk>class</jk>).parser(XmlParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder xml() {
		return serializer(XmlSerializer.class).parser(XmlParser.class);
	}

	/**
	 * Convenience method for specifying HTML as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(HtmlSerializer.<jk>class</jk>).parser(HtmlParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder html() {
		return serializer(HtmlSerializer.class).parser(HtmlParser.class);
	}

	/**
	 * Convenience method for specifying plain-text as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(PlainTextSerializer.<jk>class</jk>).parser(PlainTextParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder plainText() {
		return serializer(PlainTextSerializer.class).parser(PlainTextParser.class);
	}

	/**
	 * Convenience method for specifying MessagePack as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(MsgPackSerializer.<jk>class</jk>).parser(MsgPackParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder msgpack() {
		return serializer(MsgPackSerializer.class).parser(MsgPackParser.class);
	}

	/**
	 * Convenience method for specifying UON as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(UonSerializer.<jk>class</jk>).parser(UonParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uon() {
		return serializer(UonSerializer.class).parser(UonParser.class);
	}

	/**
	 * Convenience method for specifying URL-Encoding as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(UrlEncodingSerializer.<jk>class</jk>).parser(UrlEncodingParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder urlEnc() {
		return serializer(UrlEncodingSerializer.class).parser(UrlEncodingParser.class);
	}

	/**
	 * Convenience method for specifying URL-Encoding as the transmission media type.
	 *
	 * <p>
	 * Identical to calling <code>serializer(OpenApiSerializer.<jk>class</jk>).parser(OpenApiParser.<jk>class</jk>)</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder openapi() {
		return serializer(OpenApiSerializer.class).parser(OpenApiParser.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClientBuilder
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of an {@link HttpClientBuilder} to be used to create the {@link HttpClient}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own client builder.
	 * The builder can also be specified using the {@link #httpClientBuilder(HttpClientBuilder)} method.
	 *
	 * <p>
	 * The predefined method returns an {@link HttpClientBuilder} with the following settings:
	 * <ul>
	 * 	<li>Lax redirect strategy.
	 * </ul>
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	protected HttpClientBuilder createHttpClientBuilder() {
		return HttpClientBuilder.create().setRedirectStrategy(new AllRedirectsStrategy());
	}

	/**
	 * Returns the {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
	 *
	 * <p>
	 * This method can be used to make customizations to the {@link HttpClient}.
	 *
	 * <p>
	 * If not set via {@link #httpClientBuilder(HttpClientBuilder)}, then this object is the one created by {@link #createHttpClientBuilder()}.
	 *
	 * @return The {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
	 */
	public HttpClientBuilder getHttpClientBuilder() {
		if (httpClientBuilder == null)
			httpClientBuilder = createHttpClientBuilder();
		return httpClientBuilder;
	}

	/**
	 * Sets the {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
	 *
	 * <p>
	 * This can be used to bypass the builder created by {@link #createHttpClientBuilder()} method.
	 *
	 * @param value The {@link HttpClientBuilder} that will be used to create the {@link HttpClient} used by {@link RestClient}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder httpClientBuilder(HttpClientBuilder value) {
		this.httpClientBuilder = value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClient
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of an {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 *
	 * <p>
	 * This HTTP client is used when the HTTP client is not specified through one of the constructors or the
	 * {@link #httpClient(CloseableHttpClient)} method.
	 *
	 * <p>
	 * Subclasses can override this method to provide specially-configured HTTP clients to handle stuff such as
	 * SSL/TLS certificate handling, authentication, etc.
	 *
	 * <p>
	 * The default implementation returns an instance of {@link HttpClient} using the client builder returned by
	 * {@link #createHttpClientBuilder()}.
	 *
	 * @return The HTTP client to use.
	 * @throws Exception Error occurred.
	 */
	protected CloseableHttpClient createHttpClient() throws Exception {
		// Don't call createConnectionManager() if RestClient.setConnectionManager() was called.
		if (httpClientConnectionManager == null)
			httpClientBuilder.setConnectionManager(createConnectionManager());
		else
			httpClientBuilder.setConnectionManager(httpClientConnectionManager);
		return httpClientBuilder.build();
	}

	/**
	 * Returns the {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 *
	 * @return The {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 */
	public CloseableHttpClient getHttpClient() {
		try {
			return httpClient != null ? httpClient : createHttpClient();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 *
	 * <p>
	 * This can be used to bypass the client created by {@link #createHttpClient()} method.
	 *
	 * @param value The {@link HttpClient} to be used to handle all HTTP communications with the target server.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder httpClient(CloseableHttpClient value) {
		this.httpClient = value;
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Causes requests/responses to be logged to the specified logger at the specified log level.
	 *
	 * @param level The log level to log messages at.
	 * @param log The logger to log messages to.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder logTo(Level level, Logger log) {
		return interceptors(new BasicRestCallLogger(level, log));
	}

	/**
	 * Causes requests/responses to be logged to the console.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder logToConsole() {
		return interceptors(ConsoleRestCallLogger.DEFAULT);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpClientConnectionManager methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Sets the internal {@link HttpClientConnectionManager}.
	 *
	 * @param httpClientConnectionManager The HTTP client connection manager.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder httpClientConnectionManager(HttpClientConnectionManager httpClientConnectionManager) {
		this.httpClientConnectionManager = httpClientConnectionManager;
		return this;
	}

	/**
	 * Creates the {@link HttpClientConnectionManager} returned by {@link #createConnectionManager()}.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own connection manager.
	 *
	 * <p>
	 * The default implementation returns an instance of a {@link PoolingHttpClientConnectionManager}.
	 *
	 * @return The HTTP client builder to use to create the HTTP client.
	 */
	@SuppressWarnings("resource")
	protected HttpClientConnectionManager createConnectionManager() {
		return (pooled ? new PoolingHttpClientConnectionManager() : new BasicHttpClientConnectionManager());
	}

	/**
	 * When called, the {@link #createConnectionManager()} method will return a {@link PoolingHttpClientConnectionManager}
	 * instead of a {@link BasicHttpClientConnectionManager}.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder pooled() {
		this.pooled = true;
		return this;
	}

	/**
	 * Set up this client to use BASIC auth.
	 *
	 * @param host The auth scope hostname.
	 * @param port The auth scope port.
	 * @param user The username.
	 * @param pw The password.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder basicAuth(String host, int port, String user, String pw) {
		AuthScope scope = new AuthScope(host, port);
		Credentials up = new UsernamePasswordCredentials(user, pw);
		CredentialsProvider p = new BasicCredentialsProvider();
		p.setCredentials(scope, up);
		defaultCredentialsProvider(p);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a header on all requests.
	 *
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>, myPartSerializer, headerSchema);
	 * 		.build();
	 * </p>
	 *
	 * @param name The header name.
	 * @param value The header value.
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
	 */
	public RestClientBuilder header(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		return addTo(RESTCLIENT_headers, name, SerializedNameValuePair.create().name(name).value(value).type(HEADER).serializer(serializer).schema(schema));
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>);
	 * 		.build();
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_headers}
	 * </ul>
	 *
	 * @param name The header name.
	 * @param value The header value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder header(String name, Object value) {
		return header(name, value, null, null);
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder header(Header header) {
		return addTo(RESTCLIENT_headers, header.getName(), header);
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<jk>new</jk> NameValuePair(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder header(NameValuePair header) {
		return addTo(RESTCLIENT_headers, header.getName(), header);
	}

	/**
	 * Sets a header on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.header(<jk>new</jk> Accept(<js>"Content-Type"</js>, <js>"application/json"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param header The header to set.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder header(HttpHeader header) {
		return addTo(RESTCLIENT_headers, header.getName(), header);
	}

	/**
	 * Sets multiple headers on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param headers The header to set.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(Header...headers) {
		for (Header h : headers)
			header(h);
		return this;
	}

	/**
	 * Sets multiple headers on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(<jk>new</jk> ObjectMap(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param headers The header pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(ObjectMap headers) {
		return headers((Map<String,Object>)headers);
	}

	/**
	 * Sets multiple headers on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(AMap.<jsm>create</jsm>().append(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param headers The header pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(Map<String,Object> headers) {
		for (Map.Entry<String,Object> e : headers.entrySet())
			header(e.getKey(), e.getValue(), null, null);
		return this;
	}

	/**
	 * Sets multiple headers on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(<jk>new</jk> NameValuePairs(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param headers The header pairs.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(NameValuePairs headers) {
		for (NameValuePair p : headers)
			header(p);
		return this;
	}

	/**
	 * Sets multiple headers on all requests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(<jk>new</jk> NameValuePair(<js>"Foo"</js>, <js>"bar"</js>))
	 * 		.build();
	 * </p>
	 *
	 * @param headers The header pairs.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(NameValuePair...headers) {
		for (NameValuePair p : headers)
			header(p);
		return this;
	}

	/**
	 * Sets multiple headers on all requests using freeform key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(<js>"Header1"</js>,<js>"val1"</js>,<js>"Header2"</js>,<js>"val2"</js>)
	 * 		.build();
	 * </p>
	 *
	 * @param pairs The header key/value pairs.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into headers(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			header(stringify(pairs[i]), pairs[i+1]);
		return this;
	}

	/**
	 * Sets multiple headers on all requests using header beans.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	RestClient c = RestClient
	 * 		.<jsm>create</jsm>()
	 * 		.headers(
	 * 			<jk>new</jk> AcceptEncoding(<js>"gzip"</js>),
	 * 			<jk>new</jk> AcceptLanguage(<js>"da, en-gb;q=0.8, en;q=0.7"</js>)
	 * 		)
	 * 		.build();
	 * </p>
	 *
	 * @param headers
	 * 	The headers.
	 * 	The header values are converted to strings using the configured {@link HttpPartSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder headers(HttpHeader...headers) {
		for (HttpHeader h : headers)
			header(h.getName(), h.getValue());
		return this;
	}

	/**
	 * Sets the value for the <c>Accept</c> request header.
	 *
	 * <p>
	 * This overrides the media type specified on the parser, but is overridden by calling
	 * <code>header(<js>"Accept"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder accept(Object value) {
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
	 */
	public RestClientBuilder acceptCharset(Object value) {
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
	 */
	public RestClientBuilder acceptEncoding(Object value) {
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
	 */
	public RestClientBuilder acceptLanguage(Object value) {
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
	 */
	public RestClientBuilder authorization(Object value) {
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
	 */
	public RestClientBuilder cacheControl(Object value) {
		return header("Cache-Control", value);
	}

	/**
	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
	 *
	 * @param value The version string (e.g. <js>"1.2.3"</js>)
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder clientVersion(Object value) {
		return header("X-Client-Version", value);
	}

	/**
	 * Sets the value for the <c>Connection</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder connection(Object value) {
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
	 */
	public RestClientBuilder contentLength(Object value) {
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
	 */
	public RestClientBuilder contentType(Object value) {
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
	 */
	public RestClientBuilder date(Object value) {
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
	 */
	public RestClientBuilder expect(Object value) {
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
	 */
	public RestClientBuilder forwarded(Object value) {
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
	 */
	public RestClientBuilder from(Object value) {
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
	 */
	public RestClientBuilder host(Object value) {
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
	 */
	public RestClientBuilder ifMatch(Object value) {
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
	 */
	public RestClientBuilder ifModifiedSince(Object value) {
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
	 */
	public RestClientBuilder ifNoneMatch(Object value) {
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
	 */
	public RestClientBuilder ifRange(Object value) {
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
	 */
	public RestClientBuilder ifUnmodifiedSince(Object value) {
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
	 */
	public RestClientBuilder maxForwards(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * When called, <c>No-Trace: true</c> is added to requests.
	 *
	 * <p>
	 * This gives the opportunity for the servlet to not log errors on invalid requests.
	 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
	 * results of testing.
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder noTrace() {
		return header("No-Trace", true);
	}

	/**
	 * Sets the value for the <c>Origin</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder origin(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Sets the value for the <c>Pragma</c> request header.
	 *
	 * <p>
	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
	 *
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder pragma(Object value) {
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
	 */
	public RestClientBuilder proxyAuthorization(Object value) {
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
	 */
	public RestClientBuilder range(Object value) {
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
	 */
	public RestClientBuilder referer(Object value) {
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
	 */
	public RestClientBuilder te(Object value) {
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
	 */
	public RestClientBuilder userAgent(Object value) {
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
	 */
	public RestClientBuilder upgrade(Object value) {
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
	 */
	public RestClientBuilder via(Object value) {
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
	 */
	public RestClientBuilder warning(Object value) {
		return header("Warning", value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a query parameter to the URI.
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
	 */
	public RestClientBuilder query(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		return addTo(RESTCLIENT_query, name, SerializedNameValuePair.create().name(name).value(value).type(QUERY).serializer(serializer).schema(schema));
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
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(String name, Object value) {
		return query(name, value, null, null);
	}

	/**
	 * Adds a query parameter to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param param The query parameter.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(NameValuePair param) {
		return addTo(RESTCLIENT_query, param.getName(), param);
	}

	/**
	 * Adds query parameters to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<jk>new</jk> ObjectMap(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The query parameters.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(ObjectMap params) {
		return query((Map<String,Object>)params);
	}

	/**
	 * Adds query parameters to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(AMap.<jsm>create</jsm>().append(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The query parameters.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(Map<String,Object> params) {
		for (Map.Entry<String,Object> e : params.entrySet())
			query(e.getKey(), e.getValue());
		return this;
	}

	/**
	 * Adds query parameters to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<jk>new</jk> NameValuePairs(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The query parameters.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(NameValuePairs params) {
		for (NameValuePair p : params)
			query(p);
		return this;
	}

	/**
	 * Adds query parameters to the URI.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The query parameters.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder query(NameValuePair...params) {
		for (NameValuePair p : params)
			query(p);
		return this;
	}

	/**
	 * Adds query parameters to the URI query using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.get(<jsf>URL</jsf>)
	 * 		.query(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
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
	 */
	public RestClientBuilder query(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into query(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			query(stringify(pairs[i]), pairs[i+1]);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Form data
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Adds a form-data parameter to all request bodies.
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
	 */
	public RestClientBuilder formData(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
		return addTo(RESTCLIENT_formData, name, SerializedNameValuePair.create().name(name).value(value).type(FORMDATA).serializer(serializer).schema(schema));
	}

	/**
	 * Adds a form-data parameter to all request bodies.
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
	 * @param value The parameter value.
	 * 	<ul>
	 * 		<li>Can be any POJO.
	 * 		<li>Converted to a string using the specified part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder formData(String name, Object value) {
		return formData(name, value, null, null);
	}

	/**
	 * Adds a form-data parameter to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param param The form-data parameter.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder formData(NameValuePair param) {
		return addTo(RESTCLIENT_formData, param.getName(), param);
	}

	/**
	 * Adds form-data parameters to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<jk>new</jk> ObjectMap(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The form-data parameters.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder formData(ObjectMap params) {
		return formData((Map<String,Object>)params);
	}

	/**
	 * Adds form-data parameters to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(AMap.<jsm>create</jsm>().append(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The form-data parameters.
	 * 	<ul>
	 * 		<li>Values can be any POJO.
	 * 		<li>Values converted to a string using the configured part serializer.
	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder formData(Map<String,Object> params) {
		for (Map.Entry<String,Object> e : params.entrySet())
			formData(e.getKey(), e.getValue());
		return this;
	}

	/**
	 * Adds form-data parameters to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<jk>new</jk> NameValuePairs(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The form-data parameters.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder formData(NameValuePairs params) {
		for (NameValuePair p : params)
			formData(p);
		return this;
	}

	/**
	 * Adds form-data parameters to all request bodies.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
	 * 		.run();
	 * </p>
	 *
	 * @param params The form-data parameters.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder formData(NameValuePair...params) {
		for (NameValuePair p : params)
			formData(p);
		return this;
	}

	/**
	 * Adds form-data parameters to all request bodies using free-form key/value pairs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	client
	 * 		.formPost(<jsf>URL</jsf>)
	 * 		.formData(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
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
	 */
	public RestClientBuilder formData(Object...pairs) {
		if (pairs.length % 2 != 0)
			throw new RuntimeException("Odd number of parameters passed into formData(Object...)");
		for (int i = 0; i < pairs.length; i+=2)
			formData(stringify(pairs[i]), pairs[i+1]);
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <p>
	 * Allows you to provide a custom handler for making HTTP calls.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jic'>{@link RestCallHandler}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder callHandler(Class<? extends RestCallHandler> value) {
		return set(RESTCLIENT_callHandler, value);
	}

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <p>
	 * Allows you to provide a custom handler for making HTTP calls.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jic'>{@link RestCallHandler}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder callHandler(RestCallHandler value) {
		return set(RESTCLIENT_callHandler, value);
	}

	/**
	 * Configuration property:  Errors codes predicate.
	 *
	 * <p>
	 * Defines a predicate to test for error codes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_errorCodes}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is <code>x -&gt; x &gt;= 400</code>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder errorCodes(Predicate<Integer> value) {
		return set(RESTCLIENT_errorCodes, value);
	}

	/**
	 * Configuration property:  Executor service.
	 *
	 * <p>
	 * Defines the executor service to use when calling future methods on the {@link RestRequest} class.
	 *
	 * <p>
	 * This executor service is used to create {@link Future} objects on the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#runFuture()}
	 * </ul>
	 *
	 * <p>
	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
	 * and a queue size of 10.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorService}
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorServiceShutdownOnClose}
	 * </ul>
	 *
	 * @param executorService The executor service.
	 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
		set(RESTCLIENT_executorService, executorService);
		set(RESTCLIENT_executorServiceShutdownOnClose, shutdownOnClose);
		return this;
	}

	/**
	 * Configuration property:  Keep HttpClient open.
	 *
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_keepHttpClientOpen}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder keepHttpClientOpen(boolean value) {
		return set(RESTCLIENT_keepHttpClientOpen, value);
	}

	/**
	 * Configuration property:  Keep HttpClient open.
	 *
	 * <p>
	 * Don't close this client when the {@link RestClient#close()} method is called.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_keepHttpClientOpen}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder keepHttpClientOpen() {
		return keepHttpClientOpen(true);
	}

	/**
	 * Configuration property:  Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * </ul>
	 *
	 * @param values The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	@SuppressWarnings("unchecked")
	public RestClientBuilder interceptors(Class<? extends RestCallInterceptor>...values) {
		return addTo(RESTCLIENT_interceptors, values);
	}

	/**
	 * Configuration property:  Call interceptors.
	 *
	 * <p>
	 * Adds an interceptor that gets called immediately after a connection is made.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
	 * </ul>
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder interceptors(RestCallInterceptor...value) {
		return addTo(RESTCLIENT_interceptors, value);
	}

	/**
	 * Configuration property:  Enable leak detection.
	 *
	 * <p>
	 * Enable client and request/response leak detection.
	 *
	 * <p>
	 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
	 * when the <c>finalize</c> methods are invoked.
	 *
	 * <p>
	 * Automatically enabled with {@link RestClient#RESTCLIENT_debug}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_leakDetection}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder leakDetection() {
		return leakDetection(true);
	}

	/**
	 * Configuration property:  Enable leak detection.
	 *
	 * <p>
	 * Enable client and request/response leak detection.
	 *
	 * <p>
	 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
	 * when the <c>finalize</c> methods are invoked.
	 *
	 * <p>
	 * Automatically enabled with {@link RestClient#RESTCLIENT_debug}.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_leakDetection}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder leakDetection(boolean value) {
		return set(RESTCLIENT_leakDetection, value);
	}

	/**
	 * Configuration property:  Marshall
	 *
	 * <p>
	 * Shortcut for specifying the {@link RestClient#RESTCLIENT_serializer} and {@link RestClient#RESTCLIENT_parser}
	 * using the serializer and parser defined in a marshall.
	 *
	 * @param value The values to add to this setting.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder marshall(Marshall value) {
		if (value == null)
			serializer((Serializer)null).parser((Parser)null);
		else
			serializer(value.getSerializer()).parser(value.getParser());
		return this;
	}

	/**
	 * Configuration property:  Parser.
	 *
	 * <p>
	 * The parser to use for parsing POJOs in response bodies.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder parser(Class<? extends Parser> value) {
		return set(RESTCLIENT_parser, value);
	}

	/**
	 * Configuration property:  Parser.
	 *
	 * <p>
	 * Same as {@link #parser(Parser)} except takes in a parser instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder parser(Parser value) {
		return set(RESTCLIENT_parser, value);
	}

	/**
	 * Configuration property:  Part parser.
	 *
	 * <p>
	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partParser(Class<? extends HttpPartParser> value) {
		return set(RESTCLIENT_partParser, value);
	}

	/**
	 * Configuration property:  Part parser.
	 *
	 * <p>
	 * Same as {@link #partParser(Class)} but takes in a parser instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiParser}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partParser(HttpPartParser value) {
		return set(RESTCLIENT_partParser, value);
	}

	/**
	 * Configuration property:  Part serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
		return set(RESTCLIENT_partSerializer, value);
	}

	/**
	 * Configuration property:  Part serializer.
	 *
	 * <p>
	 * Same as {@link #partSerializer(Class)} but takes in a parser instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default value is {@link OpenApiSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder partSerializer(HttpPartSerializer value) {
		return set(RESTCLIENT_partSerializer, value);
	}

	/**
	 * Configuration property:  Root URI.
	 *
	 * <p>
	 * When set, relative URL strings passed in through the various rest call methods (e.g. {@link RestClient#get(Object)}
	 * will be prefixed with the specified root.
	 * <br>This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_rootUri}
	 * </ul>
	 *
	 * @param value
	 * 	The root URL to prefix to relative URL strings.
	 * 	<br>Trailing slashes are trimmed.
	 * 	<br>Usually a <c>String</c> but you can also pass in <c>URI</c> and <c>URL</c> objects as well.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder rootUrl(Object value) {
		return set(RESTCLIENT_rootUri, value);
	}

	/**
	 * Configuration property:  Serializer.
	 *
	 * <p>
	 * The serializer to use for serializing POJOs in request bodies.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder serializer(Class<? extends Serializer> value) {
		return set(RESTCLIENT_serializer, value);
	}

	/**
	 * Configuration property:  Serializer.
	 *
	 * <p>
	 * Same as {@link #serializer(Class)} but takes in a serializer instance.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializer}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this setting.
	 * 	<br>The default is {@link JsonSerializer}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder serializer(Serializer value) {
		return set(RESTCLIENT_serializer, value);
	}

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addBeanTypes(boolean value) {
		return set(SERIALIZER_addBeanTypes, value);
	}

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <p>
	 * Shortcut for calling <code>addBeanTypes(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addBeanTypes() {
		return set(SERIALIZER_addBeanTypes, true);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * <p>
	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
	 * type information that might normally be included to determine the data type will not be serialized.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addRootType(boolean value) {
		return set(SERIALIZER_addRootType, value);
	}

	/**
	 * Configuration property:  Add type attribute to root nodes.
	 *
	 * <p>
	 * Shortcut for calling <code>addRootType(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder addRootType() {
		return set(SERIALIZER_addRootType, true);
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * <p>
	 * Specifies that recursions should be checked for during serialization.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder detectRecursions(boolean value) {
		return set(BEANTRAVERSE_detectRecursions, value);
	}

	/**
	 * Configuration property:  Automatically detect POJO recursions.
	 *
	 * <p>
	 * Shortcut for calling <code>detectRecursions(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder detectRecursions() {
		return set(BEANTRAVERSE_detectRecursions, true);
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <p>
	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
	 * Otherwise, an exception is thrown.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Checking for recursion can cause a small performance penalty.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ignoreRecursions(boolean value) {
		return set(BEANTRAVERSE_ignoreRecursions, value);
	}

	/**
	 * Configuration property:  Ignore recursion errors.
	 *
	 * <p>
	 * Shortcut for calling <code>ignoreRecursions(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ignoreRecursions() {
		return set(BEANTRAVERSE_ignoreRecursions, true);
	}

	/**
	 * Configuration property:  Initial depth.
	 *
	 * <p>
	 * The initial indentation level at the root.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_initialDepth}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>0</c>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder initialDepth(int value) {
		return set(BEANTRAVERSE_initialDepth, value);
	}

	/**
	 * Configuration property:  Serializer listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during serialization.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder listenerS(Class<? extends SerializerListener> value) {
		return set(SERIALIZER_listener, value);
	}

	/**
	 * Configuration property:  Max serialization depth.
	 *
	 * <p>
	 * Abort serialization if specified depth is reached in the POJO tree.
	 * <br>If this depth is exceeded, an exception is thrown.
	 * <br>This prevents stack overflows from occurring when trying to serialize models with recursive references.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_maxDepth}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>100</c>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder maxDepth(int value) {
		return set(BEANTRAVERSE_maxDepth, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Copies and sorts the contents of arrays and collections before serializing them.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortCollections(boolean value) {
		return set(SERIALIZER_sortCollections, value);
	}

	/**
	 * Configuration property:  Sort arrays and collections alphabetically.
	 *
	 * <p>
	 * Shortcut for calling <code>sortCollections(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortCollections() {
		return set(SERIALIZER_sortCollections, true);
	}

	/**
	 * Sets the {@link Serializer#SERIALIZER_sortMaps} property on all serializers in this group.
	 *
	 * <p>
	 * Copies and sorts the contents of maps before serializing them.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortMaps(boolean value) {
		return set(SERIALIZER_sortMaps, value);
	}

	/**
	 * Configuration property:  Sort maps alphabetically.
	 *
	 * <p>
	 * Shortcut for calling <code>sortMaps(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sortMaps() {
		return set(SERIALIZER_sortMaps, true);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <p>
	 * If <jk>true</jk>, empty list values will not be serialized to the output.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyCollections(boolean value) {
		return set(SERIALIZER_trimEmptyCollections, value);
	}

	/**
	 * Configuration property:  Trim empty lists and arrays.
	 *
	 * <p>
	 * Shortcut for calling <code>trimEmptyCollections(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyCollections() {
		return set(SERIALIZER_trimEmptyCollections, true);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <p>
	 * If <jk>true</jk>, empty map values will not be serialized to the output.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyMaps(boolean value) {
		return set(SERIALIZER_trimEmptyMaps, value);
	}

	/**
	 * Configuration property:  Trim empty maps.
	 *
	 * <p>
	 * Shortcut for calling <code>trimEmptyMaps(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimEmptyMaps() {
		return set(SERIALIZER_trimEmptyMaps, true);
	}

	/**
	 * Configuration property:  Trim null bean property values.
	 *
	 * <p>
	 * If <jk>true</jk>, null bean values will not be serialized to the output.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimNullProperties}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>true</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimNullProperties(boolean value) {
		return set(SERIALIZER_trimNullProperties, value);
	}

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsS(boolean value) {
		return set(SERIALIZER_trimStrings, value);
	}

	/**
	 * Configuration property:  Trim strings.
	 *
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsS() {
		return set(SERIALIZER_trimStrings, true);
	}

	/**
	 * Configuration property:  URI context bean.
	 *
	 * <p>
	 * Bean used for resolution of URIs to absolute or root-relative form.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uriContext(UriContext value) {
		return set(SERIALIZER_uriContext, value);
	}

	/**
	 * Configuration property:  URI relativity.
	 *
	 * <p>
	 * Defines what relative URIs are relative to when serializing URI/URL objects.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriRelativity#RESOURCE}
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uriRelativity(UriRelativity value) {
		return set(SERIALIZER_uriRelativity, value);
	}

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <p>
	 * Defines the resolution level for URIs when serializing URI/URL objects.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link UriResolution#NONE}
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder uriResolution(UriResolution value) {
		return set(SERIALIZER_uriResolution, value);
	}

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_maxIndent}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <c>100</c>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder maxIndent(int value) {
		return set(WSERIALIZER_maxIndent, value);
	}

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * This is the character used for quoting attributes and values.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <js>'"'</js>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder quoteChar(char value) {
		return set(WSERIALIZER_quoteChar, value);
	}

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder sq() {
		return set(WSERIALIZER_quoteChar, '\'');
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * If <jk>true</jk>, newlines and indentation and spaces are added to the output to improve readability.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder useWhitespace(boolean value) {
		return set(WSERIALIZER_useWhitespace, value);
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder useWhitespace() {
		return set(WSERIALIZER_useWhitespace, true);
	}

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder ws() {
		return set(WSERIALIZER_useWhitespace, true);
	}

	/**
	 * Configuration property:  Binary string format.
	 *
	 * <p>
	 * When using the {@link Serializer#serializeToString(Object)} method on stream-based serializers, this defines the format to use
	 * when converting the resulting byte array to a string.
	 *
	 * <ul class='javatree'>
	 * 	<li class='jf'>{@link OutputStreamSerializer#OSSERIALIZER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder binaryOutputFormat(BinaryFormat value) {
		return set(OSSERIALIZER_binaryFormat, value);
	}

	/**
	 * Configuration property:  Auto-close streams.
	 *
	 * If <jk>true</jk>, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
	 * after parsing is complete.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder autoCloseStreams(boolean value) {
		return set(PARSER_autoCloseStreams, value);
	}

	/**
	 * Configuration property:  Auto-close streams.
	 *
	 * <p>
	 * Shortcut for calling <code>autoCloseStreams(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder autoCloseStreams() {
		return set(PARSER_autoCloseStreams, true);
	}

	/**
	 * Configuration property:  Debug output lines.
	 *
	 * When parse errors occur, this specifies the number of lines of input before and after the
	 * error location to be printed as part of the exception message.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_debugOutputLines}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <c>5</c>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder debugOutputLines(int value) {
		set(PARSER_debugOutputLines, value);
		return this;
	}

	/**
	 * Configuration property:  Parser listener.
	 *
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_listener}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder listenerP(Class<? extends ParserListener> value) {
		return set(PARSER_listener, value);
	}

	/**
	 * Configuration property:  Strict mode.
	 *
	 * <p>
	 * If <jk>true</jk>, strict mode for the parser is enabled.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder strict(boolean value) {
		return set(PARSER_strict, value);
	}

	/**
	 * Configuration property:  Strict mode.
	 *
	 * <p>
	 * Shortcut for calling <code>strict(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_strict}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder strict() {
		return set(PARSER_strict, true);
	}

	/**
	 * Configuration property:  Trim parsed strings.
	 *
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsP(boolean value) {
		return set(PARSER_trimStrings, value);
	}

	/**
	 * Configuration property:  Trim parsed strings.
	 *
	 * <p>
	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder trimStringsP() {
		return set(PARSER_trimStrings, true);
	}

	/**
	 * Configuration property:  Unbuffered.
	 *
	 * If <jk>true</jk>, don't use internal buffering during parsing.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder unbuffered(boolean value) {
		return set(PARSER_unbuffered, value);
	}

	/**
	 * Configuration property:  Unbuffered.
	 *
	 * <p>
	 * Shortcut for calling <code>unbuffered(<jk>true</jk>)</code>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder unbuffered() {
		return set(PARSER_unbuffered, true);
	}

	/**
	 * Configuration property:  File charset.
	 *
	 * <p>
	 * The character set to use for reading <c>Files</c> from the file system.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_fileCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <js>"DEFAULT"</js> which causes the system default to be used.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder fileCharset(String value) {
		return set(RPARSER_fileCharset, value);
	}

	/**
	 * Configuration property:  Input stream charset.
	 *
	 * <p>
	 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link ReaderParser#RPARSER_streamCharset}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is <js>"UTF-8"</js>.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder inputStreamCharset(String value) {
		return set(RPARSER_streamCharset, value);
	}

	/**
	 * Configuration property:  Binary input format.
	 *
	 * <p>
	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
	 * when converting the string into a byte array.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link InputStreamParser#ISPARSER_binaryFormat}
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>The default value is {@link BinaryFormat#HEX}.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder binaryInputFormat(BinaryFormat value) {
		return set(ISPARSER_binaryFormat, value);
	}

	/**
	 * Configuration property:  Parameter format.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder paramFormat(String value) {
		return set(UON_paramFormat, value);
	}

	/**
	 * Configuration property:  Parameter format.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 */
	public RestClientBuilder paramFormatPlain() {
		return set(UON_paramFormat, "PLAINTEXT");
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFiltersReplace(Class<?>...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFiltersReplace(Object...values) {
		super.beanFiltersReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFiltersRemove(Class<?>...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpi(Class<?> beanClass, String value) {
		super.bpi(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpi(Map<String,String> values) {
		super.bpi(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpi(String beanClassName, String value) {
		super.bpi(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpx(Class<?> beanClass, String properties) {
		super.bpx(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpx(Map<String,String> values) {
		super.bpx(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpx(String beanClassName, String value) {
		super.bpx(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpro(Class<?> beanClass, String value) {
		super.bpro(beanClass, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpro(Map<String,String> values) {
		super.bpro(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpro(String beanClassName, String value) {
		super.bpro(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpwo(Class<?> beanClass, String properties) {
		super.bpwo(beanClass, properties);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpwo(Map<String,String> values) {
		super.bpwo(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder bpwo(String beanClassName, String value) {
		super.bpwo(beanClassName, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder debug() {
		return debug(true);
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder debug(boolean value) {
		super.debug(value);
		set(RESTCLIENT_debug, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder dictionary(Class<?>...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder dictionary(Object...values) {
		super.dictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder dictionaryReplace(Class<?>...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder dictionaryReplace(Object...values) {
		super.dictionaryReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder dictionaryRemove(Class<?>...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder dictionaryRemove(Object...values) {
		super.dictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestClientBuilder example(Class<T> c, T o) {
		super.example(c, o);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> RestClientBuilder exampleJson(Class<T> c, String value) {
		super.exampleJson(c, value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreTransientFields(boolean value) {
		super.ignoreTransientFields(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClassesReplace(Class<?>...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClassesReplace(Object...values) {
		super.notBeanClassesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClassesRemove(Class<?>...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackagesReplace(String...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackagesReplace(Object...values) {
		super.notBeanPackagesReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackagesRemove(String...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwapsReplace(Class<?>...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwapsReplace(Object...values) {
		super.pojoSwapsReplace(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwapsRemove(Class<?>...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useEnumNames(boolean value) {
		super.useEnumNames(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useEnumNames() {
		super.useEnumNames();
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public RestClientBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder annotations(Annotation...values) {
		super.annotations(values);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder applyAnnotations(AnnotationList al, VarResolverSession vrs) {
		super.applyAnnotations(al, vrs);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder applyAnnotations(Class<?>...fromClasses) {
		super.applyAnnotations(fromClasses);
		return this;
	}

	@Override /* ContextBuilder */
	public RestClientBuilder applyAnnotations(Method...fromMethods) {
		super.applyAnnotations(fromMethods);
		return this;
	}

	//------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------

	/**
	 * Disables automatic redirect handling.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableRedirectHandling()
	 */
	public RestClientBuilder disableRedirectHandling() {
		httpClientBuilder.disableRedirectHandling();
		return this;
	}

	/**
	 * Assigns {@link RedirectStrategy} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #disableRedirectHandling()} method.
	 * </ul>
	 *
	 * @param redirectStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRedirectStrategy(RedirectStrategy)
	 */
	public RestClientBuilder redirectStrategy(RedirectStrategy redirectStrategy) {
		httpClientBuilder.setRedirectStrategy(redirectStrategy);
		return this;
	}

	/**
	 * Assigns default {@link CookieSpec} registry which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param cookieSpecRegistry New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieSpecRegistry(Lookup)
	 */
	public RestClientBuilder defaultCookieSpecRegistry(Lookup<CookieSpecProvider> cookieSpecRegistry) {
		httpClientBuilder.setDefaultCookieSpecRegistry(cookieSpecRegistry);
		return this;
	}

	/**
	 * Assigns {@link HttpRequestExecutor} instance.
	 *
	 * @param requestExec New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRequestExecutor(HttpRequestExecutor)
	 */
	public RestClientBuilder requestExecutor(HttpRequestExecutor requestExec) {
		httpClientBuilder.setRequestExecutor(requestExec);
		return this;
	}

	/**
	 * Assigns {@link javax.net.ssl.HostnameVerifier} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)}
	 * 		and the {@link #sslSocketFactory(LayeredConnectionSocketFactory)} methods.
	 * </ul>
	 *
	 * @param hostnameVerifier New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLHostnameVerifier(HostnameVerifier)
	 */
	public RestClientBuilder sslHostnameVerifier(HostnameVerifier hostnameVerifier) {
		httpClientBuilder.setSSLHostnameVerifier(hostnameVerifier);
		return this;
	}

	/**
	 * Assigns file containing public suffix matcher.
	 *
	 * <ul class='notes'>
	 * 	<li>Instances of this class can be created with {@link PublicSuffixMatcherLoader}.
	 * </ul>
	 *
	 * @param publicSuffixMatcher New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setPublicSuffixMatcher(PublicSuffixMatcher)
	 */
	public RestClientBuilder publicSuffixMatcher(PublicSuffixMatcher publicSuffixMatcher) {
		httpClientBuilder.setPublicSuffixMatcher(publicSuffixMatcher);
		return this;
	}

	/**
	 * Assigns {@link SSLContext} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)}
	 *  	and the {@link #sslSocketFactory(LayeredConnectionSocketFactory)} methods.
	 * </ul>
	 *
	 * @param sslContext New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLContext(SSLContext)
	 */
	public RestClientBuilder sslContext(SSLContext sslContext) {
		httpClientBuilder.setSSLContext(sslContext);
		return this;
	}

	/**
	 * Assigns {@link LayeredConnectionSocketFactory} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param sslSocketFactory New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSSLSocketFactory(LayeredConnectionSocketFactory)
	 */
	public RestClientBuilder sslSocketFactory(LayeredConnectionSocketFactory sslSocketFactory) {
		httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
		return this;
	}

	/**
	 * Assigns maximum total connection value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param maxConnTotal New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnTotal(int)
	 */
	public RestClientBuilder maxConnTotal(int maxConnTotal) {
		httpClientBuilder.setMaxConnTotal(maxConnTotal);
		return this;
	}

	/**
	 * Assigns maximum connection per route value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param maxConnPerRoute New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setMaxConnPerRoute(int)
	 */
	public RestClientBuilder maxConnPerRoute(int maxConnPerRoute) {
		httpClientBuilder.setMaxConnPerRoute(maxConnPerRoute);
		return this;
	}

	/**
	 * Assigns default {@link SocketConfig}.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param config New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultSocketConfig(SocketConfig)
	 */
	public RestClientBuilder defaultSocketConfig(SocketConfig config) {
		httpClientBuilder.setDefaultSocketConfig(config);
		return this;
	}

	/**
	 * Assigns default {@link ConnectionConfig}.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param config New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultConnectionConfig(ConnectionConfig)
	 */
	public RestClientBuilder defaultConnectionConfig(ConnectionConfig config) {
		httpClientBuilder.setDefaultConnectionConfig(config);
		return this;
	}

	/**
	 * Sets maximum time to live for persistent connections.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #connectionManager(HttpClientConnectionManager)} method.
	 * </ul>
	 *
	 * @param connTimeToLive New property value.
	 * @param connTimeToLiveTimeUnit New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionTimeToLive(long,TimeUnit)
	 */
	public RestClientBuilder connectionTimeToLive(long connTimeToLive, TimeUnit connTimeToLiveTimeUnit) {
		httpClientBuilder.setConnectionTimeToLive(connTimeToLive, connTimeToLiveTimeUnit);
		return this;
	}

	/**
	 * Assigns {@link HttpClientConnectionManager} instance.
	 *
	 * @param connManager New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManager(HttpClientConnectionManager)
	 */
	public RestClientBuilder connectionManager(HttpClientConnectionManager connManager) {
		this.httpClientConnectionManager = connManager;
		httpClientBuilder.setConnectionManager(connManager);
		return this;
	}

	/**
	 * Defines the connection manager is to be shared by multiple client instances.
	 *
	 * <ul class='notes'>
	 * 	<li>If the connection manager is shared its life-cycle is expected to be managed by the caller and it will not be shut down if the client is closed.
	 * </ul>
	 *
	 * @param shared New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionManagerShared(boolean)
	 */
	public RestClientBuilder connectionManagerShared(boolean shared) {
		httpClientBuilder.setConnectionManagerShared(shared);
		return this;
	}

	/**
	 * Assigns {@link ConnectionReuseStrategy} instance.
	 *
	 * @param reuseStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionReuseStrategy(ConnectionReuseStrategy)
	 */
	public RestClientBuilder connectionReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
		httpClientBuilder.setConnectionReuseStrategy(reuseStrategy);
		return this;
	}

	/**
	 * Assigns {@link ConnectionKeepAliveStrategy} instance.
	 *
	 * @param keepAliveStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setKeepAliveStrategy(ConnectionKeepAliveStrategy)
	 */
	public RestClientBuilder keepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		httpClientBuilder.setKeepAliveStrategy(keepAliveStrategy);
		return this;
	}

	/**
	 * Assigns {@link AuthenticationStrategy} instance for target host authentication.
	 *
	 * @param targetAuthStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setTargetAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClientBuilder targetAuthenticationStrategy(AuthenticationStrategy targetAuthStrategy) {
		httpClientBuilder.setTargetAuthenticationStrategy(targetAuthStrategy);
		return this;
	}

	/**
	 * Assigns {@link AuthenticationStrategy} instance for proxy authentication.
	 *
	 * @param proxyAuthStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxyAuthenticationStrategy(AuthenticationStrategy)
	 */
	public RestClientBuilder proxyAuthenticationStrategy(AuthenticationStrategy proxyAuthStrategy) {
		httpClientBuilder.setProxyAuthenticationStrategy(proxyAuthStrategy);
		return this;
	}

	/**
	 * Assigns {@link UserTokenHandler} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #disableConnectionState()} method.
	 * </ul>
	 *
	 * @param userTokenHandler New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserTokenHandler(UserTokenHandler)
	 */
	public RestClientBuilder userTokenHandler(UserTokenHandler userTokenHandler) {
		httpClientBuilder.setUserTokenHandler(userTokenHandler);
		return this;
	}

	/**
	 * Disables connection state tracking.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableConnectionState()
	 */
	public RestClientBuilder disableConnectionState() {
		httpClientBuilder.disableConnectionState();
		return this;
	}

	/**
	 * Assigns {@link SchemePortResolver} instance.
	 *
	 * @param schemePortResolver New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setSchemePortResolver(SchemePortResolver)
	 */
	public RestClientBuilder schemePortResolver(SchemePortResolver schemePortResolver) {
		httpClientBuilder.setSchemePortResolver(schemePortResolver);
		return this;
	}

	/**
	 * Assigns <c>User-Agent</c> value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * 	<li>{@link #userAgent(Object)} is an equivalent method.
	 * </ul>
	 *
	 * @param userAgent New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setUserAgent(String)
	 */
	public RestClientBuilder userAgent(String userAgent) {
		httpClientBuilder.setUserAgent(userAgent);
		return this;
	}

	/**
	 * Assigns default request header values.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * 	<li>{@link #headers(Header...)} is an equivalent method.
	 * </ul>
	 *
	 * @param defaultHeaders New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultHeaders(Collection)
	 */
	public RestClientBuilder defaultHeaders(Collection<? extends Header> defaultHeaders) {
		httpClientBuilder.setDefaultHeaders(defaultHeaders);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the head of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpResponseInterceptor)
	 */
	public RestClientBuilder addInterceptorFirst(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the tail of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpResponseInterceptor)
	 */
	public RestClientBuilder addInterceptorLast(HttpResponseInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the head of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorFirst(HttpRequestInterceptor)
	 */
	public RestClientBuilder addInterceptorFirst(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorFirst(itcp);
		return this;
	}

	/**
	 * Adds this protocol interceptor to the tail of the protocol processing list.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @param itcp New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#addInterceptorLast(HttpRequestInterceptor)
	 */
	public RestClientBuilder addInterceptorLast(HttpRequestInterceptor itcp) {
		httpClientBuilder.addInterceptorLast(itcp);
		return this;
	}

	/**
	 * Disables state (cookie) management.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableCookieManagement()
	 */
	public RestClientBuilder disableCookieManagement() {
		httpClientBuilder.disableCookieManagement();
		return this;
	}

	/**
	 * Disables automatic content decompression.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableContentCompression()
	 */
	public RestClientBuilder disableContentCompression() {
		httpClientBuilder.disableContentCompression();
		return this;
	}

	/**
	 * Disables authentication scheme caching.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #httpProcessor(HttpProcessor)} method.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAuthCaching()
	 */
	public RestClientBuilder disableAuthCaching() {
		httpClientBuilder.disableAuthCaching();
		return this;
	}

	/**
	 * Assigns {@link HttpProcessor} instance.
	 *
	 * @param httpprocessor New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setHttpProcessor(HttpProcessor)
	 */
	public RestClientBuilder httpProcessor(HttpProcessor httpprocessor) {
		httpClientBuilder.setHttpProcessor(httpprocessor);
		return this;
	}

	/**
	 * Assigns {@link HttpRequestRetryHandler} instance.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #disableAutomaticRetries()} method.
	 * </ul>
	 *
	 * @param retryHandler New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRetryHandler(HttpRequestRetryHandler)
	 */
	public RestClientBuilder retryHandler(HttpRequestRetryHandler retryHandler) {
		httpClientBuilder.setRetryHandler(retryHandler);
		return this;
	}

	/**
	 * Disables automatic request recovery and re-execution.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#disableAutomaticRetries()
	 */
	public RestClientBuilder disableAutomaticRetries() {
		httpClientBuilder.disableAutomaticRetries();
		return this;
	}

	/**
	 * Assigns default proxy value.
	 *
	 * <ul class='notes'>
	 * 	<li>This value can be overridden by the {@link #routePlanner(HttpRoutePlanner)} method.
	 * </ul>
	 *
	 * @param proxy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setProxy(HttpHost)
	 */
	public RestClientBuilder proxy(HttpHost proxy) {
		httpClientBuilder.setProxy(proxy);
		return this;
	}

	/**
	 * Assigns {@link HttpRoutePlanner} instance.
	 *
	 * @param routePlanner New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setRoutePlanner(HttpRoutePlanner)
	 */
	public RestClientBuilder routePlanner(HttpRoutePlanner routePlanner) {
		httpClientBuilder.setRoutePlanner(routePlanner);
		return this;
	}

	/**
	 * Assigns {@link ConnectionBackoffStrategy} instance.
	 *
	 * @param connectionBackoffStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setConnectionBackoffStrategy(ConnectionBackoffStrategy)
	 */
	public RestClientBuilder connectionBackoffStrategy(ConnectionBackoffStrategy connectionBackoffStrategy) {
		httpClientBuilder.setConnectionBackoffStrategy(connectionBackoffStrategy);
		return this;
	}

	/**
	 * Assigns {@link BackoffManager} instance.
	 *
	 * @param backoffManager New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setBackoffManager(BackoffManager)
	 */
	public RestClientBuilder backoffManager(BackoffManager backoffManager) {
		httpClientBuilder.setBackoffManager(backoffManager);
		return this;
	}

	/**
	 * Assigns {@link ServiceUnavailableRetryStrategy} instance.
	 *
	 * @param serviceUnavailStrategy New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setServiceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy)
	 */
	public RestClientBuilder serviceUnavailableRetryStrategy(ServiceUnavailableRetryStrategy serviceUnavailStrategy) {
		httpClientBuilder.setServiceUnavailableRetryStrategy(serviceUnavailStrategy);
		return this;
	}

	/**
	 * Assigns default {@link CookieStore} instance which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param cookieStore New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCookieStore(CookieStore)
	 */
	public RestClientBuilder defaultCookieStore(CookieStore cookieStore) {
		httpClientBuilder.setDefaultCookieStore(cookieStore);
		return this;
	}

	/**
	 * Assigns default {@link CredentialsProvider} instance which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param credentialsProvider New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultCredentialsProvider(CredentialsProvider)
	 */
	public RestClientBuilder defaultCredentialsProvider(CredentialsProvider credentialsProvider) {
		httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		return this;
	}

	/**
	 * Assigns default {@link org.apache.http.auth.AuthScheme} registry which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param authSchemeRegistry New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultAuthSchemeRegistry(Lookup)
	 */
	public RestClientBuilder defaultAuthSchemeRegistry(Lookup<AuthSchemeProvider> authSchemeRegistry) {
		httpClientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
		return this;
	}

	/**
	 * Assigns a map of {@link org.apache.http.client.entity.InputStreamFactory InputStreamFactories} to be used for automatic content decompression.
	 *
	 * @param contentDecoderMap New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setContentDecoderRegistry(Map)
	 */
	public RestClientBuilder contentDecoderRegistry(Map<String,InputStreamFactory> contentDecoderMap) {
		httpClientBuilder.setContentDecoderRegistry(contentDecoderMap);
		return this;
	}

	/**
	 * Assigns default {@link RequestConfig} instance which will be used for request execution if not explicitly set in the client execution context.
	 *
	 * @param config New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#setDefaultRequestConfig(RequestConfig)
	 */
	public RestClientBuilder defaultRequestConfig(RequestConfig config) {
		httpClientBuilder.setDefaultRequestConfig(config);
		return this;
	}

	/**
	 * Use system properties when creating and configuring default implementations.
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#useSystemProperties()
	 */
	public RestClientBuilder useSystemProperties() {
		httpClientBuilder.useSystemProperties();
		return this;
	}

	/**
	 * Makes this instance of {@link HttpClient} proactively evict expired connections from the connection pool using a background thread.
	 *
	 * <ul class='notes'>
	 * 	<li>One MUST explicitly close HttpClient with {@link CloseableHttpClient#close()} in order to stop and release the background thread.
	 * 	<li>This method has no effect if the instance of {@link HttpClient} is configured to use a shared connection manager.
	 * 	<li>This method may not be used when the instance of {@link HttpClient} is created inside an EJB container.
	 * </ul>
	 *
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictExpiredConnections()
	 */
	public RestClientBuilder evictExpiredConnections() {
		httpClientBuilder.evictExpiredConnections();
		return this;
	}

	/**
	 * Makes this instance of {@link HttpClient} proactively evict idle connections from the connection pool using a background thread.
	 *
	 * <ul class='notes'>
	 * 	<li>One MUST explicitly close HttpClient with {@link CloseableHttpClient#close()} in order to stop and release the background thread.
	 * 	<li>This method has no effect if the instance of {@link HttpClient} is configured to use a shared connection manager.
	 * 	<li>This method may not be used when the instance of {@link HttpClient} is created inside an EJB container.
	 * </ul>
	 *
	 * @param maxIdleTime New property value.
	 * @param maxIdleTimeUnit New property value.
	 * @return This object (for method chaining).
	 * @see HttpClientBuilder#evictIdleConnections(long,TimeUnit)
	 */
	public RestClientBuilder evictIdleConnections(long maxIdleTime, TimeUnit maxIdleTimeUnit) {
		httpClientBuilder.evictIdleConnections(maxIdleTime, maxIdleTimeUnit);
		return this;
	}
}
