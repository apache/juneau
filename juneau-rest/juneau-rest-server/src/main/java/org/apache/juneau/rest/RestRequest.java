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
package org.apache.juneau.rest;

import static java.util.Collections.*;
import static java.util.Optional.*;
import static java.util.logging.Level.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static java.lang.Integer.*;
import static java.util.Collections.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.BasicHeader;
import org.apache.juneau.http.header.Date;
import org.apache.juneau.http.response.*;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;

/**
 * Represents an HTTP request for a REST resource.
 *
 * <p>
 * Equivalent to {@link HttpServletRequest} except with some additional convenience methods.
 *
 * <p>
 * For reference, given the URL <js>"http://localhost:9080/contextRoot/servletPath/foo?bar=baz#qux"</js>, the
 * following methods return the following values....
 * <table class='styled'>
 * 	<tr><th>Method</th><th>Value</th></tr>
 * 	<tr><td>{@code getContextPath()}</td><td>{@code /contextRoot}</td></tr>
 * 	<tr><td>{@code getPathInfo()}</td><td>{@code /foo}</td></tr>
 * 	<tr><td>{@code getPathTranslated()}</td><td>{@code path-to-deployed-war-on-filesystem/foo}</td></tr>
 * 	<tr><td>{@code getQueryString()}</td><td>{@code bar=baz}</td></tr>
 * 	<tr><td>{@code getRequestURI()}</td><td>{@code /contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getRequestURL()}</td><td>{@code http://localhost:9080/contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getServletPath()}</td><td>{@code /servletPath}</td></tr>
 * </table>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRestRequest}
 * </ul>
 */
@SuppressWarnings({ "unchecked", "unused" })
public final class RestRequest {

	// Constructor initialized.
	private HttpServletRequest inner;
	private final RestContext context;
	private final RestOpContext opContext;
	private final RequestBody body;
	private final BeanSession beanSession;
	private final RequestQueryParams queryParams;
	private final RequestPathParams pathParams;
	private final RequestHeaders headers;
	private final RequestAttributes attrs;
	private final HttpPartParserSession partParserSession;
	private final RestSession session;

	// Lazy initialized.
	private VarResolverSession varSession;
	private RequestFormParams formParams;
	private UriContext uriContext;
	private String authorityPath;
	private Config config;
	private Swagger swagger;
	private Charset charset;

	/**
	 * Constructor.
	 */
	RestRequest(RestOpContext opContext, RestSession session) throws Exception {
		this.session = session;
		this.opContext = opContext;

		inner = session.getRequest();
		context = session.getContext();

		attrs = new RequestAttributes(this);

		queryParams = new RequestQueryParams(this, session.getQueryParams(), true);

		headers = new RequestHeaders(this, queryParams, false);

		body = new RequestBody(this);

		if (context.isAllowBodyParam()) {
			String b = queryParams.getString("body").orElse(null);
			if (b != null) {
				headers.set("Content-Type", UonSerializer.DEFAULT.getResponseContentType());
				body.load(MediaType.UON, UonParser.DEFAULT, b.getBytes(UTF8));
			}
		}

		pathParams = new RequestPathParams(session, this, true);

		beanSession = opContext.getBeanContext().getSession();

		partParserSession = opContext.getPartParser().getPartSession();

		pathParams.parser(partParserSession);

		queryParams
			.addDefault(opContext.getDefaultRequestQueryData().getAll())
			.parser(partParserSession);

		headers
			.addDefault(opContext.getDefaultRequestHeaders().getAll())
			.addDefault(context.getDefaultRequestHeaders().getAll())
			.parser(partParserSession);

		body
			.encoders(opContext.getEncoders())
			.parsers(opContext.getParsers())
			.maxInput(opContext.getMaxInput());

		attrs
			.addDefault(opContext.getDefaultRequestAttributes())
			.addDefault(context.getDefaultRequestAttributes());

		if (isDebug())
			inner = CachingHttpServletRequest.wrap(inner);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Request line.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the request line of this request.
	 *
	 * @return The request line of this request.
	 */
	public RequestLine getRequestLine() {
		String x = inner.getProtocol();
		int i = x.indexOf('/');
		int j = x.indexOf('.', i);
		ProtocolVersion pv = new ProtocolVersion(x.substring(0,i), parseInt(x.substring(i+1,j)), parseInt(x.substring(j+1)));
		return new BasicRequestLine(inner.getMethod(), inner.getRequestURI(), pv);
	}

	/**
	 * Returns the protocol version from the request line of this request.
	 *
	 * @return The protocol version from the request line of this request.
	 */
	public ProtocolVersion getProtocolVersion() {
		return getRequestLine().getProtocolVersion();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Assertions
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns an assertion on the request line returned by {@link #getRequestLine()}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the request body contains "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertRequestLine().protocol().minor().is(1);
	 * </p>
	 *
	 * @return A new assertion object.
	 */
	public FluentRequestLineAssertion<RestRequest> assertRequestLine() {
		return new FluentRequestLineAssertion<RestRequest>(getRequestLine(), this);
	}

	/**
	 * Returns a fluent assertion for the request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the request body contains "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertBody().is(<js>"foo"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion on the body, never <jk>null</jk>.
	 */
	public FluentRequestBodyAssertion<RestRequest> assertBody() {
		return new FluentRequestBodyAssertion<RestRequest>(getBody(), this);
	}

	/**
	 * Returns a fluent assertion for the specified header.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>request</jv>
	 * 		.assertHeader(<js>"Content-Type"</js>).is(<js>"application/json"</js>);
	 * </p>
	 *
	 * @param name The header name.
	 * @return A new fluent assertion on the parameter, never <jk>null</jk>.
	 */
	public FluentRequestHeaderAssertion<RestRequest> assertHeader(String name) {
		return new FluentRequestHeaderAssertion<RestRequest>(getHeader(name), this);
	}

	/**
	 * Returns a fluent assertion for the specified query parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>request</jv>
	 * 		.assertQueryParam(<js>"foo"</js>).asString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * @param name The query parameter name.
	 * @return A new fluent assertion on the parameter, never <jk>null</jk>.
	 */
	public FluentRequestQueryParamAssertion<RestRequest> assertQueryParam(String name) {
		return new FluentRequestQueryParamAssertion<RestRequest>(getQueryParam(name), this);
	}


	/**
	 * Returns a fluent assertion for the specified form parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>request</jv>
	 * 		.assertFormParam(<js>"foo"</js>).asString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * @param name The query parameter name.
	 * @return A new fluent assertion on the parameter, never <jk>null</jk>.
	 */
	public FluentRequestFormParamAssertion<RestRequest> assertFormParam(String name) {
		return new FluentRequestFormParamAssertion<RestRequest>(getFormParam(name), this);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Headers
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request headers.
	 *
	 * <p>
	 * Returns a {@link RequestHeaders} object that encapsulates access to HTTP headers on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(...)
	 * 	<jk>public</jk> Object myMethod(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to headers.</jc>
	 * 		RequestHeaders <jv>headers</jv> = <jv>req</jv>.getRequestHeaders();
	 *
	 * 		<jc>// Add a default value.</jc>
	 * 		<jv>headers</jv>.addDefault(<js>"ETag"</js>, <jsf>DEFAULT_UUID</jsf>);
	 *
	 *  	<jc>// Get a header value as a POJO.</jc>
	 * 		UUID etag = <jv>headers</jv>.get(<js>"ETag"</js>).asType(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
	 *
	 * 		<jc>// Get a standard header.</jc>
	 * 		Optional&lt;CacheControl&gt; = <jv>headers</jv>.getCacheControl();
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered part parser on the resource class.
	 * 	<li>
	 * 		The {@link RequestHeaders} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Header @Header} annotation can be used to access individual header values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The headers on this request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestHeaders getHeaders() {
		return headers;
	}

	/**
	 * Returns the last header with a specified name of this message.
	 *
	 * <p>
	 * If there is more than one matching header in the message the last element of <c>getHeaders(String)</c> is returned.
	 * <br>If there is no matching header in the message, an empty request header object is returned.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Gets a header and throws a BadRequest if it doesn't exist.</jc>
	 * 	<jv>request</jv>
	 * 		.getHeader(<js>"Foo"</js>)
	 * 		.assertValue().exists()
	 * 		.get();
	 * </p>
	 *
	 * @param name The header name.
	 * @return The request header object, never <jk>null</jk>.
	 */
	public RequestHeader getHeader(String name) {
		return headers.getLast(name);
	}

	/**
	 * Returns <jk>true</jk> if this request contains the specified header.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if this request contains the specified header.
	 */
	public boolean containsHeader(String name) {
		return headers.contains(name);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the response character encoding.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response content charset is UTF-8.</jc>
	 * 	<jv>request</jv>
	 * 		.assertCharset().is(<js>"utf-8"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws BasicHttpException If REST call failed.
	 */
	public FluentStringAssertion<RestRequest> assertCharset() {
		return new FluentStringAssertion<>(getCharset().name(), this);
	}

	/**
	 * Sets the charset to expect on the request body.
	 *
	 * @param value The new value to use for the request body.
	 */
	public void setCharset(Charset value) {
		this.charset = value;
	}

	/**
	 * Returns the charset specified on the <c>Content-Type</c> header, or <js>"UTF-8"</js> if not specified.
	 *
	 * @return The charset to use to decode the request body.
	 */
	public Charset getCharset() {
		if (charset == null) {
			// Determine charset
			// NOTE:  Don't use super.getCharacterEncoding() because the spec is implemented inconsistently.
			// Jetty returns the default charset instead of null if the character is not specified on the request.
			String h = getHeader("Content-Type").orElse(null);
			if (h != null) {
				int i = h.indexOf(";charset=");
				if (i > 0)
					charset = Charset.forName(h.substring(i+9).trim());
			}
			if (charset == null)
				charset = opContext.getDefaultCharset();
			if (charset == null)
				charset = Charset.forName("UTF-8");
		}
		return charset;
	}

	/**
	 * Returns the preferred Locale that the client will accept content in, based on the Accept-Language header.
	 *
	 * <p>
	 * If the client request doesn't provide an <c>Accept-Language</c> header, this method returns the default locale for the server.
	 *
	 * @return The preferred Locale that the client will accept content in.  Never <jk>null</jk>.
	 */
	public Locale getLocale() {
		Locale best = inner.getLocale();
		String h = headers.getString("Accept-Language").orElse(null);
		if (h != null) {
			StringRanges sr = StringRanges.of(h);
			float qValue = 0;
			for (StringRange r : sr.getRanges()) {
				if (r.getQValue() > qValue) {
					best = toLocale(r.getName());
					qValue = r.getQValue();
				}
			}
		}
		return best;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Standard headers.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the <c>Accept</c> header on the request.
	 *
	 * <p>
	 * Content-Types that are acceptable for the response.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept: text/plain
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Accept getAccept() {
		return headers.get("Accept").asHeader(Accept.class);
	}

	/**
	 * Returns the <c>Accept-Charset</c> header on the request.
	 *
	 * <p>
	 * Character sets that are acceptable.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept-Charset: utf-8
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public AcceptCharset getAcceptCharset() {
		return headers.get("Accept-Charset").asHeader(AcceptCharset.class);
	}

	/**
	 * Returns the <c>Accept-Encoding</c> header on the request.
	 *
	 * <p>
	 * List of acceptable encodings.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept-Encoding: gzip, deflate
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public AcceptEncoding getAcceptEncoding() {
		return headers.get("Accept-Encoding").asHeader(AcceptEncoding.class);
	}

	/**
	 * Returns the <c>Accept-Language</c> header on the request.
	 *
	 * <p>
	 * List of acceptable human languages for response.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Accept-Language: en-US
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public AcceptLanguage getAcceptLanguage() {
		return headers.get("Accept-Language").asHeader(AcceptLanguage.class);
	}

	/**
	 * Returns the <c>Authorization</c> header on the request.
	 *
	 * <p>
	 * Authentication credentials for HTTP authentication.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Authorization getAuthorization() {
		return headers.get("Authorization").asHeader(Authorization.class);
	}

	/**
	 * Returns the <c>Cache-Control</c> header on the request.
	 *
	 * <p>
	 * Used to specify directives that must be obeyed by all caching mechanisms along the request-response chain.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Cache-Control: no-cache
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public CacheControl getCacheControl() {
		return headers.get("Cache-Control").asHeader(CacheControl.class);
	}

	/**
	 * Returns the <c>Connection</c> header on the request.
	 *
	 * <p>
	 * Control options for the current connection and list of hop-by-hop request fields.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Connection: keep-alive
	 * 	Connection: Upgrade
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Connection getConnection() {
		return headers.get("Connection").asHeader(Connection.class);
	}

	/**
	 * Returns the <c>Content-Length</c> header on the request.
	 *
	 * <p>
	 * The length of the request body in octets (8-bit bytes).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Content-Length: 348
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public ContentLength getContentLength() {
		return headers.get("Content-Length").asHeader(ContentLength.class);
	}

	/**
	 * Returns the <c>Content-Type</c> header on the request.
	 *
	 * <p>
	 * The MIME type of the body of the request (used with POST and PUT requests).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Content-Type: application/x-www-form-urlencoded
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public ContentType getContentType() {
		return headers.get("Content-Type").asHeader(ContentType.class);
	}

	/**
	 * Returns the <c>Date</c> header on the request.
	 *
	 * <p>
	 * The date and time that the message was originated (in "HTTP-date" format as defined by RFC 7231 Date/Time Formats).
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Date: Tue, 15 Nov 1994 08:12:31 GMT
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Date getDate() {
		return headers.get("Date").asHeader(Date.class);
	}

	/**
	 * Returns the <c>Expect</c> header on the request.
	 *
	 * <p>
	 * Indicates that particular server behaviors are required by the client.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Expect: 100-continue
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Expect getExpect() {
		return headers.get("Expect").asHeader(Expect.class);
	}

	/**
	 * Returns the <c>From</c> header on the request.
	 *
	 * <p>
	 * The email address of the user making the request.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	From: user@example.com
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public From getFrom() {
		return headers.get("From").asHeader(From.class);
	}

	/**
	 * Returns the <c>Host</c> header on the request.
	 *
	 * <p>
	 * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening.
	 * The port number may be omitted if the port is the standard port for the service requested.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Host: en.wikipedia.org:8080
	 * 	Host: en.wikipedia.org
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Host getHost() {
		return headers.get("Host").asHeader(Host.class);
	}

	/**
	 * Returns the <c>If-Match</c> header on the request.
	 *
	 * <p>
	 * Only perform the action if the client supplied entity matches the same entity on the server.
	 * This is mainly for methods like PUT to only update a resource if it has not been modified since the user last
	 * updated it.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Match: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public IfMatch getIfMatch() {
		return headers.get("If-Match").asHeader(IfMatch.class);
	}

	/**
	 * Returns the <c>If-Modified-Since</c> header on the request.
	 *
	 * <p>
	 * Allows a 304 Not Modified to be returned if content is unchanged.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public IfModifiedSince getIfModifiedSince() {
		return headers.get("If-Modified-Since").asHeader(IfModifiedSince.class);
	}

	/**
	 * Returns the <c>If-None-Match</c> header on the request.
	 *
	 * <p>
	 * Allows a 304 Not Modified to be returned if content is unchanged, see HTTP ETag.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-None-Match: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public IfNoneMatch getIfNoneMatch() {
		return headers.get("If-None-Match").asHeader(IfNoneMatch.class);
	}

	/**
	 * Returns the <c>If-Range</c> header on the request.
	 *
	 * <p>
	 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire ofNullable(entity.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Range: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public IfRange getIfRange() {
		return headers.get("If-Range").asHeader(IfRange.class);
	}

	/**
	 * Returns the <c>If-Unmodified-Since</c> header on the request.
	 *
	 * <p>
	 * Only send the response if the entity has not been modified since a specific time.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public IfUnmodifiedSince getIfUnmodifiedSince() {
		return headers.get("If-Unmodified-Since").asHeader(IfUnmodifiedSince.class);
	}

	/**
	 * Returns the <c>Max-Forwards</c> header on the request.
	 *
	 * <p>
	 * Limit the number of times the message can be forwarded through proxies or gateways.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Max-Forwards: 10
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public MaxForwards getMaxForwards() {
		return headers.get("Max-Forwards").asHeader(MaxForwards.class);
	}

	/**
	 * Returns the <c>Pragma</c> header on the request.
	 *
	 * <p>
	 * Implementation-specific fields that may have various effects anywhere along the request-response chain.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Pragma: no-cache
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Pragma getPragma() {
		return headers.get("Pragma").asHeader(Pragma.class);
	}

	/**
	 * Returns the <c>Proxy-Authorization</c> header on the request.
	 *
	 * <p>
	 * Authorization credentials for connecting to a proxy.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Proxy-Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public ProxyAuthorization getProxyAuthorization() {
		return headers.get("Proxy-Authorization").asHeader(ProxyAuthorization.class);
	}

	/**
	 * Returns the <c>Range</c> header on the request.
	 *
	 * <p>
	 * Request only part of an entity. Bytes are numbered from 0.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Range: bytes=500-999
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Range getRange() {
		return headers.get("Range").asHeader(Range.class);
	}

	/**
	 * Returns the <c>Referer</c> header on the request.
	 *
	 * <p>
	 * This is the address of the previous web page from which a link to the currently requested page was followed.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Referer: http://en.wikipedia.org/wiki/Main_Page
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Referer getReferer() {
		return headers.get("Referer").asHeader(Referer.class);
	}

	/**
	 * Returns the <c>TE</c> header on the request.
	 *
	 * <p>
	 * The transfer encodings the user agent is willing to accept: the same values as for the response header field
	 * Transfer-Encoding can be used, plus the "trailers" value (related to the "chunked" transfer method) to notify the
	 * server it expects to receive additional fields in the trailer after the last, zero-sized, chunk.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	TE: trailers, deflate
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public TE getTE() {
		return headers.get("TE").asHeader(TE.class);
	}

	/**
	 * Returns the <c>Time-Zone</c> header value on the request if there is one.
	 *
	 * <p>
	 * Example: <js>"GMT"</js>.
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Optional<TimeZone> getTimeZone() {
		String tz = headers.getString("Time-Zone").orElse(null);
		if (tz != null)
			return of(TimeZone.getTimeZone(tz));
		return empty();
	}

	/**
	 * Returns the <c>User-Agent</c> header on the request.
	 *
	 * <p>
	 * The user agent string of the user agent.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public UserAgent getUserAgent() {
		return headers.get("Upgrade").asHeader(UserAgent.class);
	}

	/**
	 * Returns the <c>Upgrade</c> header on the request.
	 *
	 * <p>
	 * Ask the server to upgrade to another protocol.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Upgrade: HTTP/2.0, HTTPS/1.3, IRC/6.9, RTA/x11, websocket
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Upgrade getUpgrade() {
		return headers.get("Upgrade").asHeader(Upgrade.class);
	}

	/**
	 * Returns the <c>Via</c> header on the request.
	 *
	 * <p>
	 * Informs the server of proxies through which the request was sent.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Via: 1.0 fred, 1.1 example.com (Apache/1.1)
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Via getVia() {
		return headers.get("Via").asHeader(Via.class);
	}

	/**
	 * Returns the <c>Warning</c> header on the request.
	 *
	 * <p>
	 * A general warning about possible problems with the entity body.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	Warning: 199 Miscellaneous warning
	 * </p>
	 *
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public Warning getWarning() {
		return headers.get("Warning").asHeader(Warning.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Attributes
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request attributes.
	 *
	 * <p>
	 * Returns a {@link RequestAttributes} object that encapsulates access to attributes on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(...)
	 * 	<jk>public</jk> Object myMethod(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to attributes.</jc>
	 * 		RequestAttributes <jv>attributes</jv> = <jv>req</jv>.getAttributes();
	 *
	 *  	<jc>// Get a header value as a POJO.</jc>
	 * 		UUID <jv>etag</jv> = <jv>attributes</jv>.get(<js>"ETag"</js>, UUID.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered part parser on the resource class.
	 * 	<li>
	 * 		The {@link RequestAttributes} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Attr @Attr} annotation can be used to access individual attribute values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestAttributes}
	 * </ul>
	 *
	 * @return
	 * 	The headers on this request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestAttributes getAttributes() {
		return attrs;
	}

	/**
	 * Returns the request attribute with the specified name.
	 *
	 * @param name The attribute name.
	 * @return The attribute value, never <jk>null</jk>.
	 */
	public RequestAttribute getAttribute(String name) {
		return attrs.get(name);
	}

	/**
	 * Sets a request attribute.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 */
	public void setAttribute(String name, Object value) {
		attrs.set(name, value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Query parameters.
	 *
	 * <p>
	 * Returns a {@link RequestQueryParams} object that encapsulates access to URL GET parameters.
	 *
	 * <p>
	 * Similar to {@link HttpServletRequest#getParameterMap()} but only looks for query parameters in the URL and not form posts.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(...)
	 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to query parameters on the URL.</jc>
	 * 		RequestQueryParams <jv>query</jv> = <jv>req</jv>.getQuery();
	 *
	 * 		<jc>// Get query parameters converted to various types.</jc>
	 * 		<jk>int</jk> <jv>p1/<jv> = <jv>query</jv>.getInteger(<js>"p1"</js>).orElse(<jk>null</jk>);
	 * 		String <jv>p2</jv> = <jv>query</jv>.getString(<js>"p2"</js>).orElse(<jk>null</jk>);
	 * 		UUID <jv>p3</jv> = <jv>query</jv>.get(<js>"p3"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestQuery}
	 * </ul>
	 *
	 * @return
	 * 	The query parameters as a modifiable map.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestQueryParams getQueryParams() {
		return queryParams;
	}

	/**
	 * Shortcut for calling <c>getRequestQuery().getLast(<jv>name</jv>)</c>.
	 *
	 * @param name The query parameter name.
	 * @return The query parameter, never <jk>null</jk>.
	 */
	public RequestQueryParam getQueryParam(String name) {
		return queryParams.get(name);
	}

	/**
	 * Returns <jk>true</jk> if this request contains the specified header.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if this request contains the specified header.
	 */
	public boolean containsQueryParam(String name) {
		return queryParams.contains(name);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Form data parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Form-data.
	 *
	 * <p>
	 * Returns a {@link RequestFormParams} object that encapsulates access to form post parameters.
	 *
	 * <p>
	 * Similar to {@link HttpServletRequest#getParameterMap()}, but only looks for form data in the HTTP body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(...)
	 * 	<jk>public void</jk> doPost(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to parsed form data parameters.</jc>
	 * 		RequestFormData <jv>formData</jv> = <jv>req</jv>.getFormData();
	 *
	 * 		<jc>// Get form data parameters converted to various types.</jc>
	 * 		<jk>int</jk> <jv>p1</jv> = <jv>formData</jv>.get(<js>"p1"</js>, 0, <jk>int</jk>.<jk>class</jk>);
	 * 		String <jv>p2</jv> = <jv>formData</jv>.get(<js>"p2"</js>, String.<jk>class</jk>);
	 * 		UUID <jv>p3</jv> = <jv>formData</jv>.get(<js>"p3"</js>, UUID.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * 	<li>
	 * 		Values are converted from strings using the registered part parser on the resource class.
	 * 	<li>
	 * 		The {@link RequestFormParams} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link FormData @FormDAta} annotation can be used to access individual form data parameter values.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestFormData}
	 * </ul>
	 *
	 * @return
	 * 	The URL-encoded form data from the request.
	 * 	<br>Never <jk>null</jk>.
	 * @throws InternalServerError If query parameters could not be parsed.
	 * @see org.apache.juneau.http.annotation.FormData
	 */
	public RequestFormParams getFormParams() throws InternalServerError {
		try {
			if (formParams == null)
				formParams = new RequestFormParams(this, true).parser(partParserSession);
			formParams.addDefault(opContext.getDefaultRequestFormData().getAll());
			return formParams;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	/**
	 * Shortcut for calling <c>getFormData().getString(name)</c>.
	 *
	 * @param name The form data parameter name.
	 * @return The form data parameter value, or <jk>null</jk> if not found.
	 */
	public RequestFormParam getFormParam(String name) {
		return getFormParams().get(name);
	}

	/**
	 * Returns <jk>true</jk> if this request contains the specified header.
	 *
	 * @param name The header name.
	 * @return <jk>true</jk> if this request contains the specified header.
	 */
	public boolean containsFormParam(String name) {
		return getFormParams().contains(name);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Path parameters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Path parameters.
	 *
	 * <p>
	 * Returns a {@link RequestPathParams} object that encapsulates access to URL path parameters.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(<js>"/{foo}/{bar}/{baz}/*"</js>)
	 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to path data.</jc>
	 * 		RequestPathParams <jv>pathParams</jv> = <jv>req</jv>.getPathParams();
	 *
	 * 		<jc>// Example URL:  /123/qux/true/quux</jc>
	 *
	 * 		<jk>int</jk> <jv>foo</jv> = <jv>pathParams</jv>.get(<js>"foo"</js>).asInteger().orElse(-1);  <jc>// =123</jc>
	 * 		String <jv>bar</jv> = <jv>pathParams</jv>.get(<js>"bar"</js>).orElse(null);  <jc>// =qux</jc>
	 * 		<jk>boolean</jk> <jv>baz</jv> = <jv>pathParams</jv>.get(<js>"baz"</js>).asBoolean().orElse(<jk>false</jk>);  <jc>// =true</jc>
	 * 		String <jv>remainder</jv> = <jv>pathParams</jv>.getRemainder().orElse(<jk>null</jk>);  <jc>// =quux</jc>
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		This object is modifiable.
	 * </ul>
	 *
	 * @return
	 * 	The path parameters.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestPathParams getPathParams() {
		return pathParams;
	}

	/**
	 * Shortcut for calling <c>getPathParams().get(<jv>name</jv>)</c>.
	 *
	 * @param name The path parameter name.
	 * @return The path parameter, never <jk>null</jk>.
	 */
	public RequestPathParam getPathParam(String name) {
		return pathParams.get(name);
	}

	/**
	 * Shortcut for calling <c>getPathParams().getRemainder()</c>.
	 *
	 * @return The path remainder value, never <jk>null</jk>.
	 */
	public RequestPathParam getPathRemainder() {
		return pathParams.getRemainder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Body methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request body.
	 *
	 * <p>
	 * Returns a {@link RequestBody} object that encapsulates access to the HTTP request body.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(...)
	 * 	<jk>public void</jk> doPost(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Convert body to a linked list of Person objects.</jc>
	 * 		List&lt;Person&gt; <jv>list</jv> = <jv>req</jv>.getBody().asType(LinkedList.<jk>class</jk>, Person.<jk>class</jk>);
	 * 		..
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link RequestBody} object can also be passed as a parameter on the method.
	 * 	<li>
	 * 		The {@link Body @Body} annotation can be used to access the body as well.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestmRequestBody}
	 * </ul>
	 *
	 * @return
	 * 	The body of this HTTP request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestBody getBody() {
		return body;
	}

	/**
	 * Returns the HTTP body content as a {@link Reader}.
	 *
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query
	 * string.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * <p>
	 * This method is equivalent to calling <c>getBody().getReader()</c>.
	 *
	 * @return The HTTP body content as a {@link Reader}.
	 * @throws IOException If body could not be read.
	 */
	public BufferedReader getReader() throws IOException {
		return getBody().getReader();
	}

	/**
	 * Returns the HTTP body content as an {@link InputStream}.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * <p>
	 * This method is equivalent to calling <c>getBody().getInputStream()</c>.
	 *
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it in the GZIP wrapper.
	 */
	public ServletInputStream getInputStream() throws IOException {
		return getBody().getInputStream();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// URI-related methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the portion of the request URI that indicates the context of the request.
	 *
	 * <p>The context path always comes first in a request URI.
	 * The path starts with a <js>"/"</js> character but does not end with a <js>"/"</js> character.
	 * For servlets in the default (root) context, this method returns <js>""</js>.
	 * The container does not decode this string.
	 *
	 * @return The context path, never <jk>null</jk>.
	 * @see HttpServletRequest#getContextPath()
	 */
	public String getContextPath() {
		String cp = context.getUriContext();
		return cp == null ? inner.getContextPath() : cp;
	}

	/**
	 * Returns the URI authority portion of the request.
	 *
	 * @return The URI authority portion of the request.
	 */
	public String getAuthorityPath() {
		if (authorityPath == null)
			authorityPath = context.getUriAuthority();
		if (authorityPath == null) {
			String scheme = inner.getScheme();
			int port = inner.getServerPort();
			StringBuilder sb = new StringBuilder(inner.getScheme()).append("://").append(inner.getServerName());
			if (! (port == 80 && "http".equals(scheme) || port == 443 && "https".equals(scheme)))
				sb.append(':').append(port);
			authorityPath = sb.toString();
		}
		return authorityPath;
	}

	/**
	 * Returns the part of this request's URL that calls the servlet.
	 *
	 * <p>
	 * This path starts with a <js>"/"</js> character and includes either the servlet name or a path to the servlet,
	 * but does not include any extra path information or a query string.
	 *
	 * @return The servlet path, never <jk>null</jk>.
	 * @see HttpServletRequest#getServletPath()
	 */
	public String getServletPath() {
		String cp = context.getUriContext();
		String sp = inner.getServletPath();
		return cp == null || ! sp.startsWith(cp) ? sp : sp.substring(cp.length());
	}

	/**
	 * Returns the URI context of the request.
	 *
	 * <p>
	 * The URI context contains all the information about the URI of the request, such as the servlet URI, context
	 * path, etc...
	 *
	 * @return The URI context of the request.
	 */
	public UriContext getUriContext() {
		if (uriContext == null)
			uriContext = UriContext.of(getAuthorityPath(), getContextPath(), getServletPath(), StringUtils.urlEncodePath(inner.getPathInfo()));
		return uriContext;
	}

	/**
	 * Returns a URI resolver that can be used to convert URIs to absolute or root-relative form.
	 *
	 * @param resolution The URI resolution rule.
	 * @param relativity The relative URI relativity rule.
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver(UriResolution resolution, UriRelativity relativity) {
		return UriResolver.of(resolution, relativity, getUriContext());
	}

	/**
	 * Shortcut for calling {@link #getUriResolver()} using {@link UriResolution#ROOT_RELATIVE} and
	 * {@link UriRelativity#RESOURCE}
	 *
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver() {
		return UriResolver.of(context.getUriResolution(), context.getUriRelativity(), getUriContext());
	}

	/**
	 * Returns the URI for this request.
	 *
	 * <p>
	 * Similar to {@link #getRequestURI()} but returns the value as a {@link URI}.
	 * It also gives you the capability to override the query parameters (e.g. add new query parameters to the existing
	 * URI).
	 *
	 * @param includeQuery If <jk>true</jk> include the query parameters on the request.
	 * @param addQueryParams Augment the request URI with the specified query parameters.
	 * @return A new URI.
	 */
	public URI getUri(boolean includeQuery, Map<String,?> addQueryParams) {
		String uri = inner.getRequestURI();
		if (includeQuery || addQueryParams != null) {
			StringBuilder sb = new StringBuilder(uri);
			RequestQueryParams rq = this.queryParams.copy();
			if (addQueryParams != null)
				for (Map.Entry<String,?> e : addQueryParams.entrySet())
					rq.set(e.getKey(), e.getValue());
			if (! rq.isEmpty())
				sb.append('?').append(rq.asQueryString());
			uri = sb.toString();
		}
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			// Shouldn't happen.
			throw runtimeException(e);
		}
	}

	/**
	 * Returns any extra path information associated with the URL the client sent when it made this request.
	 *
	 * <p>
	 * The extra path information follows the servlet path but precedes the query string and will start with a <js>"/"</js> character.
	 * This method returns <jk>null</jk> if there was no extra path information.
	 *
	 * @return The extra path information.
	 * @see HttpServletRequest#getPathInfo()
	 */
	public String getPathInfo() {
		return inner.getPathInfo();
	}

	/**
	 * Returns the part of this request's URL from the protocol name up to the query string in the first line of the HTTP request.
	 *
	 * The web container does not decode this String
	 *
	 * @return The request URI.
	 * @see HttpServletRequest#getRequestURI()
	 */
	public String getRequestURI() {
		return inner.getRequestURI();
	}


	/**
	 * Returns the query string that is contained in the request URL after the path.
	 *
	 * <p>
	 * This method returns <jk>null</jk> if the URL does not have a query string.
	 *
	 * @return The query string.
	 * @see HttpServletRequest#getQueryString()
	 */
	public String getQueryString() {
		return inner.getQueryString();
	}

	/**
	 * Reconstructs the URL the client used to make the request.
	 *
	 * <p>
	 * The returned URL contains a protocol, server name, port number, and server path, but it does not include query string parameters.
	 *
	 * @return The request URL.
	 * @see HttpServletRequest#getRequestURL()
	 */
	public StringBuffer getRequestURL() {
		return inner.getRequestURL();
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Labels
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the localized swagger associated with the resource.
	 *
	 * <p>
	 * A shortcut for calling <c>getInfoProvider().getSwagger(request);</c>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>
	 * 	<jk>public</jk> List&lt;Tag&gt; swaggerTags(RestRequest <jv>req</jv>) {
	 * 		<jk>return</jk> <jv>req</jv>.getSwagger().getTags();
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link Swagger} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(SwaggerProvider)}
	 * 	<li class='link'>{@doc RestSwagger}
	 * </ul>
	 *
	 * @return
	 * 	The swagger associated with the resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Optional<Swagger> getSwagger() {
		return context.getSwagger(getLocale());
	}

	/**
	 * Returns the swagger for the Java method invoked.
	 *
	 * @return The swagger for the Java method as an {@link Optional}.  Never <jk>null</jk>.
	 */
	public Optional<Operation> getOperationSwagger() {

		Optional<Swagger> swagger = getSwagger();
		if (! swagger.isPresent())
			return Optional.empty();

		return swagger.get().operation(opContext.getPathPattern(), getMethod().toLowerCase());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the part serializer associated with this request.
	 *
	 * @return The part serializer associated with this request.
	 */
	public HttpPartParserSession getPartParserSession() {
		return partParserSession;
	}

	/**
	 * Returns the HTTP method of this request.
	 *
	 * <p>
	 * If <c>allowHeaderParams</c> init parameter is <jk>true</jk>, then first looks for
	 * <c>&amp;method=xxx</c> in the URL query string.
	 *
	 * @return The HTTP method of this request.
	 */
	public String getMethod() {
		return session.getMethod();
	}

	/**
	 * Returns <jk>true</jk> if <c>&amp;plainText=true</c> was specified as a URL parameter.
	 *
	 * <p>
	 * This indicates that the <c>Content-Type</c> of the output should always be set to <js>"text/plain"</js>
	 * to make it easy to render in a browser.
	 *
	 * <p>
	 * This feature is useful for debugging.
	 *
	 * @return <jk>true</jk> if {@code &amp;plainText=true} was specified as a URL parameter
	 */
	public boolean isPlainText() {
		return "true".equals(queryParams.getString("plainText").orElse("false"));
	}

	/**
	 * Returns the resource bundle for the request locale.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>
	 * 	<jk>public</jk> String hello(RestRequest <jv>req</jv>, <ja>@Query</ja>(<js>"user"</js>) String <jv>user</jv>) {
	 *
	 * 		<jc>// Return a localized message.</jc>
	 * 		<jk>return</jk> <jv>req</jv>.getMessages().getString(<js>"hello.message"</js>, <jv>user</jv>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link Messages} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestRequest#getMessage(String,Object...)}
	 * 	<li class='link'>{@doc RestMessages}
	 * </ul>
	 *
	 * @return
	 * 	The resource bundle.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() {
		return context.getMessages().forLocale(getLocale());
	}

	/**
	 * Shortcut method for calling {@link RestRequest#getMessages()} and {@link Messages#getString(String,Object...)}.
	 *
	 * @param key The message key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return The localized message.
	 */
	public String getMessage(String key, Object...args) {
		return getMessages().getString(key, args);
	}

	/**
	 * Returns the resource context handling the request.
	 *
	 * <p>
	 * Can be used to access servlet-init parameters or annotations during requests, such as in calls to
	 * {@link RestGuard#guard(RestRequest, RestResponse)}..
	 *
	 * @return The resource context handling the request.
	 */
	public RestContext getContext() {
		return context;
	}

	/**
	 * Returns access to the inner {@link RestOpContext} of this method.
	 *
	 * @return The {@link RestOpContext} of this method.  May be <jk>null</jk> if method has not yet been found.
	 */
	public RestOpContext getOpContext() {
		return opContext;
	}

	/**
	 * Returns the {@link BeanSession} associated with this request.
	 *
	 * @return The request bean session.
	 */
	public BeanSession getBeanSession() {
		return beanSession;
	}

	/**
	 * Returns <jk>true</jk> if debug mode is enabled.
	 *
	 * Debug mode is enabled by simply adding <js>"?debug=true"</js> to the query string or adding a <c>Debug: true</c> header on the request.
	 *
	 * @return <jk>true</jk> if debug mode is enabled.
	 */
	public boolean isDebug() {
		return getAttribute("Debug").asType(Boolean.class).orElse(false);
	}

	/**
	 * Sets the <js>"Exception"</js> attribute to the specified throwable.
	 *
	 * <p>
	 * This exception is used by {@link BasicRestLogger} for logging purposes.
	 *
	 * @param t The attribute value.
	 * @return This object (for method chaining).
	 */
	public RestRequest setException(Throwable t) {
		setAttribute("Exception", t);
		return this;
	}

	/**
	 * Sets the <js>"NoTrace"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestLogger} and tells it not to log the current request.
	 *
	 * @param b The attribute value.
	 * @return This object (for method chaining).
	 */
	public RestRequest setNoTrace(Boolean b) {
		setAttribute("NoTrace", b);
		return this;
	}

	/**
	 * Shortcut for calling <c>setNoTrace(<jk>true</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestRequest setNoTrace() {
		return setNoTrace(true);
	}

	/**
	 * Sets the <js>"Debug"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link BasicRestLogger} to help determine how a request should be logged.
	 *
	 * @param b The attribute value.
	 * @return This object (for method chaining).
	 * @throws IOException If body could not be cached.
	 */
	public RestRequest setDebug(Boolean b) throws IOException {
		setAttribute("Debug", b);
		if (b)
			inner = CachingHttpServletRequest.wrap(inner);
		return this;
	}

	/**
	 * Shortcut for calling <c>setDebug(<jk>true</jk>)</c>.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If body could not be cached.
	 */
	public RestRequest setDebug() throws IOException {
		return setDebug(true);
	}

	/**
	 * Request-level variable resolver session.
	 *
	 * <p>
	 * Used to resolve SVL variables in text.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>
	 * 	<jk>public</jk> String hello(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get var resolver session.</jc>
	 * 		VarResolverSession <jv>session</jv> = getVarResolverSession();
	 *
	 * 		<jc>// Use it to construct a customized message from a query parameter.</jc>
	 * 		<jk>return</jk> <jv>session</jv>.resolve(<js>"Hello $RQ{user}!"</js>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link VarResolverSession} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'>{@doc RestSvlVariables}
	 * </ul>
	 *
	 * @return The variable resolver for this request.
	 */
	public VarResolverSession getVarResolverSession() {
		if (varSession == null)
			varSession = context
				.getVarResolver()
				.createSession(session.getBeanStore())
				.bean(RestRequest.class, this)
				.bean(RestSession.class, session);
		return varSession;
	}

	/**
	 * Returns the file finder registered on the REST resource context object.
	 *
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes.
	 *
	 * @return The file finder associated with the REST resource object.
	 */
	public FileFinder getFileFinder() {
		return context.getFileFinder();
	}

	/**
	 * Returns the static files registered on the REST resource context object.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API.
	 *
	 * @return This object (for method chaining).
	 */
	public StaticFiles getStaticFiles() {
		return context.getStaticFiles();
	}

	/**
	 * Config file associated with the resource.
	 *
	 * <p>
	 * Returns a config file with session-level variable resolution.
	 *
	 * The config file is identified via one of the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Rest#config()}
	 * 	<li class='jm'>{@link RestContext.Builder#config(Config)}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestGet</ja>(...)
	 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get config file.</jc>
	 * 		Config <jv>config</jv> = <jv>req</jv>.getConfig();
	 *
	 * 		<jc>// Get simple values from config file.</jc>
	 * 		<jk>int</jk> <jv>timeout</jv> = <jv>config</jv>.getInt(<js>"MyResource/timeout"</js>, 10000);
	 *
	 * 		<jc>// Get complex values from config file.</jc>
	 * 		MyBean <jv>bean</jv> = <jv>config</jv>.getObject(<js>"MyResource/myBean"</js>, MyBean.<jk>class</jk>);
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The {@link Config} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestConfigurationFiles}
	 * </ul>
	 *
	 * @return
	 * 	The config file associated with the resource, or <jk>null</jk> if resource does not have a config file
	 * 	associated with it.
	 */
	public Config getConfig() {
		if (config == null)
			config = context.getConfig().resolving(getVarResolverSession());
		return config;
	}

	/**
	 * Creates a proxy interface to retrieve HTTP parts of this request as a proxy bean.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestPost</ja>(<js>"/mypath/{p1}/{p2}/*"</js>)
	 * 	<jk>public void</jk> myMethod(<ja>@Request</ja> MyRequest <jv>requestBean</jv>) {...}
	 *
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<ja>@Path</ja> <jc>// Path variable name inferred from getter.</jc>
	 * 		String getP1();
	 *
	 * 		<ja>@Path</ja>(<js>"p2"</js>)
	 * 		String getX();
	 *
	 * 		<ja>@Path</ja>(<js>"/*"</js>)
	 * 		String getRemainder();
	 *
	 * 		<ja>@Query</ja>
	 * 		String getQ1();
	 *
	 *		<jc>// Schema-based query parameter:  Pipe-delimited lists of comma-delimited lists of integers.</jc>
	 * 		<ja>@Query</ja>(
	 * 			collectionFormat=<js>"pipes"</js>
	 * 			items=<ja>@Items</ja>(
	 * 				items=<ja>@SubItems</ja>(
	 * 					collectionFormat=<js>"csv"</js>
	 * 					type=<js>"integer"</js>
	 * 				)
	 * 			)
	 * 		)
	 * 		<jk>int</jk>[][] getQ3();
	 *
	 * 		<ja>@Header</ja>(<js>"*"</js>)
	 * 		Map&lt;String,Object&gt; getHeaders();
	 * </p>
	 *
	 * @param c The request bean interface to instantiate.
	 * @return A new request bean proxy for this REST request.
	 */
	public <T> T getRequest(Class<T> c) {
		return getRequest(RequestBeanMeta.create(c, getContext().getAnnotations()));
	}

	/**
	 * Same as {@link #getRequest(Class)} but used on pre-instantiated {@link RequestBeanMeta} objects.
	 *
	 * @param rbm The metadata about the request bean interface to create.
	 * @return A new request bean proxy for this REST request.
	 */
	public <T> T getRequest(final RequestBeanMeta rbm) {
		try {
			Class<T> c = (Class<T>)rbm.getClassMeta().getInnerClass();
			final BeanSession bs = getBeanSession();
			final BeanMeta<T> bm = bs.getBeanMeta(c);
			return (T)Proxy.newProxyInstance(
				c.getClassLoader(),
				new Class[] { c },
				new InvocationHandler() {
					@Override /* InvocationHandler */
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RequestBeanPropertyMeta pm = rbm.getProperty(method.getName());
						if (pm != null) {
							HttpPartParserSession pp = pm.getParser(getPartParserSession());
							HttpPartSchema schema = pm.getSchema();
							String name = pm.getPartName();
							ClassMeta<?> type = bs.getClassMeta(method.getGenericReturnType());
							HttpPartType pt = pm.getPartType();
							if (pt == HttpPartType.BODY)
								return getBody().schema(schema).asType(type);
							if (pt == QUERY)
								return getQueryParam(name).parser(pp).schema(schema).asType(type).orElse(null);
							if (pt == FORMDATA)
								return getFormParam(name).parser(pp).schema(schema).asType(type).orElse(null);
							if (pt == HEADER)
								return getHeader(name).parser(pp).schema(schema).asType(type).orElse(null);
							if (pt == PATH)
								return getPathParam(name).parser(pp).schema(schema).asType(type).orElse(null);
						}
						return null;
					}

			});
		} catch (Exception e) {
			throw runtimeException(e);
		}
	}

	/* Called by RestSession.finish() */
	void close() {
		if (config != null) {
			try {
				config.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the current session associated with this request, or if the request does not have a session, creates one.
	 *
	 * @return The current request session.
	 * @see HttpServletRequest#getSession()
	 */
	public HttpSession getSession() {
		return inner.getSession();
	}

	/**
	 * Returns a boolean indicating whether the authenticated user is included in the specified logical "role".
	 *
	 * <p>
	 * Roles and role membership can be defined using deployment descriptors.
	 * If the user has not been authenticated, the method returns false.
	 *
	 * @param role The role name.
	 * @return <jk>true</jk> if the user holds the specified role.
	 * @see HttpServletRequest#isUserInRole(String)
	 */
	public boolean isUserInRole(String role) {
		return inner.isUserInRole(role);
	}

	/**
	 * Returns the wrapped servlet request.
	 *
	 * @return The wrapped servlet request.
	 */
	public HttpServletRequest getHttpServletRequest() {
		return inner;
	}

	/**
	 * Returns the part serializer session for this request.
	 *
	 * @return The part serializer session for this request.
	 */
	public HttpPartSerializerSession getPartSerializerSession() {
		return opContext.getPartSerializer().getPartSession();
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder("\n").append(getRequestLine()).append("\n");
		sb.append("---Headers---\n");
		for (RequestHeader h : getHeaders().getAll()) {
			sb.append("\t").append(h).append("\n");
		}
		String m = getMethod();
		if (m.equals("PUT") || m.equals("POST")) {
			try {
				sb.append("---Body UTF-8---\n");
				sb.append(body.asString()).append("\n");
				sb.append("---Body Hex---\n");
				sb.append(body.asSpacedHex()).append("\n");
			} catch (Exception e1) {
				sb.append(e1.getLocalizedMessage());
			}
		}
		return sb.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/*
	 * Converts an Accept-Language value entry to a Locale.
	 */
	private static Locale toLocale(String lang) {
		String country = "";
		int i = lang.indexOf('-');
		if (i > -1) {
			country = lang.substring(i+1).trim();
			lang = lang.substring(0,i).trim();
		}
		return new Locale(lang, country);
	}
}