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
import static java.util.logging.Level.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.serializer.Serializer.*;
import static java.lang.Integer.*;
import static java.util.Collections.*;
import static java.util.Optional.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
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
import org.apache.juneau.common.internal.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Content;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.assertions.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.BasicHeader;
import org.apache.juneau.http.header.Date;
import org.apache.juneau.http.response.*;
import org.apache.juneau.http.response.BasicHttpException;
import org.apache.juneau.rest.staticfile.*;
import org.apache.juneau.rest.swagger.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;

/**
 * Represents an HTTP request for a REST resource.
 *
 * <p>
 * 	The {@link RestRequest} object is an extension of the <l>HttpServletRequest</l> class
 * 	with various built-in convenience methods for use in building REST interfaces.
 * 	It can be accessed by passing it as a parameter on your REST Java method:
 * </p>
 *
 * <p class='bjava'>
 * 	<ja>@RestPost</ja>(...)
 * 	<jk>public</jk> Object myMethod(RestRequest <jv>req</jv>) {...}
 * </p>
 *
 * <p>
 * 	The primary methods on this class are:
 * </p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link RestRequest}
 * 	<ul class='spaced-list'>
 * 		<li>Methods for accessing the request content:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestRequest#getContent() getContent()}
 * 			<li class='jm'>{@link RestRequest#getInputStream() getInputStream()}
 * 			<li class='jm'>{@link RestRequest#getReader() getReader()}
 * 		</ul>
 * 		<li>Methods for accessing HTTP parts:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestRequest#containsFormParam(String) containsFormParam(String)}
 * 			<li class='jm'>{@link RestRequest#containsHeader(String) containsHeader(String)}
 * 			<li class='jm'>{@link RestRequest#containsQueryParam(String) containsQueryParam(String)}
 * 			<li class='jm'>{@link RestRequest#getHeader(Class) getHeader(Class)}
 * 			<li class='jm'>{@link RestRequest#getHeader(String) getHeader(String)}
 * 			<li class='jm'>{@link RestRequest#getHeaders() getHeaders()}
 * 			<li class='jm'>{@link RestRequest#getFormParam(Class) getFormParam(Class)}
 * 			<li class='jm'>{@link RestRequest#getFormParam(String) getFormParam(String)}
 * 			<li class='jm'>{@link RestRequest#getFormParams() getFormParams()}
 * 			<li class='jm'>{@link RestRequest#getPathParam(Class) getPathParam(Class)}
 * 			<li class='jm'>{@link RestRequest#getPathParam(String) getPathParam(String)}
 * 			<li class='jm'>{@link RestRequest#getPathParams() getPathParams()}
 * 			<li class='jm'>{@link RestRequest#getPathRemainder() getPathRemainder()}
 * 			<li class='jm'>{@link RestRequest#getQueryParam(Class) getQueryParam(Class)}
 * 			<li class='jm'>{@link RestRequest#getQueryParam(String) getQueryParam(String)}
 * 			<li class='jm'>{@link RestRequest#getQueryParams() getQueryParams()}
 * 			<li class='jm'>{@link RestRequest#getQueryString() getQueryString()}
 * 		</ul>
 * 		<li>Methods for localization:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestRequest#getLocale() getLocale()}
 * 			<li class='jm'>{@link RestRequest#getMessage(String,Object...) getMessage(String,Object...)}
 * 			<li class='jm'>{@link RestRequest#getMessages() getMessages()}
 * 			<li class='jm'>{@link RestRequest#getTimeZone() getTimeZone()}
 * 		</ul>
 * 		<li>Methods for accessing static files:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestRequest#getStaticFiles() getStaticFiles()}
 * 			<li class='jm'>{@link RestRequest#getVarResolverSession() getVarResolverSession()}
 * 		</ul>
 * 		<li>Methods for assertions:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestRequest#assertContent() assertContent()}
 * 			<li class='jm'>{@link RestRequest#assertCharset() assertCharset()}
 * 			<li class='jm'>{@link RestRequest#assertFormParam(String) assertFormParam(String)}
 * 			<li class='jm'>{@link RestRequest#assertHeader(String) assertHeader(String)}
 * 			<li class='jm'>{@link RestRequest#assertQueryParam(String) assertQueryParam(String)}
 * 			<li class='jm'>{@link RestRequest#assertRequestLine() assertRequestLine()}
 * 		</ul>
 * 		<li>Other:
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link RestRequest#getAttribute(String) getAttribute(String)}
 * 			<li class='jm'>{@link RestRequest#getAttributes() getAttributes()}
 * 			<li class='jm'>{@link RestRequest#getAuthorityPath() getAuthorityPath()}
 * 			<li class='jm'>{@link RestRequest#getBeanSession() getBeanSession()}
 * 			<li class='jm'>{@link RestRequest#getCharset() getCharset()}
 * 			<li class='jm'>{@link RestRequest#getConfig() getConfig()}
 * 			<li class='jm'>{@link RestRequest#getContext() getContext()}
 * 			<li class='jm'>{@link RestRequest#getContextPath() getContextPath()}
 * 			<li class='jm'>{@link RestRequest#getHttpServletRequest() getHttpServletRequest()}
 * 			<li class='jm'>{@link RestRequest#getMethod() getMethod()}
 * 			<li class='jm'>{@link RestRequest#getOpContext() getOpContext()}
 * 			<li class='jm'>{@link RestRequest#getOperationSwagger() getOperationSwagger()}
 * 			<li class='jm'>{@link RestRequest#getPartParserSession() getPartParserSession()}
 * 			<li class='jm'>{@link RestRequest#getPartSerializerSession() getPartSerializerSession()}
 * 			<li class='jm'>{@link RestRequest#getPathInfo() getPathInfo()}
 * 			<li class='jm'>{@link RestRequest#getProtocolVersion() getProtocolVersion()}
 * 			<li class='jm'>{@link RestRequest#getRequest(Class) getRequest(Class)}
 * 			<li class='jm'>{@link RestRequest#getRequestLine() getRequestLine()}
 * 			<li class='jm'>{@link RestRequest#getRequestURI() getRequestURI()}
 * 			<li class='jm'>{@link RestRequest#getRequestURL() getRequestURL()}
 * 			<li class='jm'>{@link RestRequest#getServletPath() getServletPath()}
 * 			<li class='jm'>{@link RestRequest#getSession() getSession()}
 * 			<li class='jm'>{@link RestRequest#getSwagger() getSwagger()}
 * 			<li class='jm'>{@link RestRequest#getUriContext() getUriContext()}
 * 			<li class='jm'>{@link RestRequest#getUriResolver() getUriResolver()}
 * 			<li class='jm'>{@link RestRequest#isDebug() isDebug()}
 * 			<li class='jm'>{@link RestRequest#isPlainText() isPlainText()}
 * 			<li class='jm'>{@link RestRequest#isUserInRole(String) isUserInRole(String)}
 * 			<li class='jm'>{@link RestRequest#setAttribute(String,Object) setAttribute(String,Object)}
 * 			<li class='jm'>{@link RestRequest#setCharset(Charset) setCharset(Charset)}
 * 			<li class='jm'>{@link RestRequest#setDebug() setDebug()}
 * 			<li class='jm'>{@link RestRequest#setException(Throwable) setException(Throwable)}
 * 			<li class='jm'>{@link RestRequest#setNoTrace() setNoTrace()}
 * 		</ul>
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
@SuppressWarnings({ "unchecked", "unused" })
public final class RestRequest extends HttpServletRequestWrapper {

	// Constructor initialized.
	private HttpServletRequest inner;
	private final RestContext context;
	private final RestOpContext opContext;
	private final RequestContent content;
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
		super(session.getRequest());
		this.session = session;
		this.opContext = opContext;

		inner = session.getRequest();
		context = session.getContext();

		attrs = new RequestAttributes(this);

		queryParams = new RequestQueryParams(this, session.getQueryParams(), true);

		headers = new RequestHeaders(this, queryParams, false);

		content = new RequestContent(this);

		if (context.isAllowContentParam()) {
			String b = queryParams.get("content").asString().orElse(null);
			if (b != null) {
				headers.set("Content-Type", UonSerializer.DEFAULT.getResponseContentType());
				content.mediaType(MediaType.UON).parser(UonParser.DEFAULT).content(b.getBytes(UTF8));
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

		content
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
	 * <p class='bjava'>
	 * 	<jc>// Validates the request content contains "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertRequestLine().protocol().minor().is(1);
	 * </p>
	 *
	 * @return A new assertion object.
	 */
	public FluentRequestLineAssertion<RestRequest> assertRequestLine() {
		return new FluentRequestLineAssertion<>(getRequestLine(), this);
	}

	/**
	 * Returns a fluent assertion for the request content.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the request content contains "foo".</jc>
	 * 	<jv>request</jv>
	 * 		.assertContent().asString().is(<js>"foo"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion on the content, never <jk>null</jk>.
	 */
	public FluentRequestContentAssertion<RestRequest> assertContent() {
		return new FluentRequestContentAssertion<>(getContent(), this);
	}

	/**
	 * Returns a fluent assertion for the specified header.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>request</jv>
	 * 		.assertHeader(<js>"Content-Type"</js>).asString().is(<js>"application/json"</js>);
	 * </p>
	 *
	 * @param name The header name.
	 * @return A new fluent assertion on the parameter, never <jk>null</jk>.
	 */
	public FluentRequestHeaderAssertion<RestRequest> assertHeader(String name) {
		return new FluentRequestHeaderAssertion<>(getHeaderParam(name), this);
	}

	/**
	 * Returns a fluent assertion for the specified query parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>request</jv>
	 * 		.assertQueryParam(<js>"foo"</js>).asString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * @param name The query parameter name.
	 * @return A new fluent assertion on the parameter, never <jk>null</jk>.
	 */
	public FluentRequestQueryParamAssertion<RestRequest> assertQueryParam(String name) {
		return new FluentRequestQueryParamAssertion<>(getQueryParam(name), this);
	}


	/**
	 * Returns a fluent assertion for the specified form parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>request</jv>
	 * 		.assertFormParam(<js>"foo"</js>).asString().contains(<js>"bar"</js>);
	 * </p>
	 *
	 * @param name The query parameter name.
	 * @return A new fluent assertion on the parameter, never <jk>null</jk>.
	 */
	public FluentRequestFormParamAssertion<RestRequest> assertFormParam(String name) {
		return new FluentRequestFormParamAssertion<>(getFormParam(name), this);
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
	 * <p class='bjava'>
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
	 * 		UUID etag = <jv>headers</jv>.get(<js>"ETag"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
	 *
	 * 		<jc>// Get a standard header.</jc>
	 * 		Optional&lt;CacheControl&gt; = <jv>headers</jv>.getCacheControl();
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This object is modifiable.
	 * 	<li class='note'>
	 * 		Values are converted from strings using the registered part parser on the resource class.
	 * 	<li class='note'>
	 * 		The {@link RequestHeaders} object can also be passed as a parameter on the method.
	 * 	<li class='note'>
	 * 		The {@link Header @Header} annotation can be used to access individual header values.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HttpParts">HTTP Parts</a>
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
	 * <p class='bjava'>
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
	public RequestHeader getHeaderParam(String name) {
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
	 * <p class='bjava'>
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
	 * Sets the charset to expect on the request content.
	 *
	 * @param value The new value to use for the request content.
	 */
	public void setCharset(Charset value) {
		this.charset = value;
	}

	/**
	 * Returns the charset specified on the <c>Content-Type</c> header, or <js>"UTF-8"</js> if not specified.
	 *
	 * @return The charset to use to decode the request content.
	 */
	public Charset getCharset() {
		if (charset == null) {
			// Determine charset
			// NOTE:  Don't use super.getCharacterEncoding() because the spec is implemented inconsistently.
			// Jetty returns the default charset instead of null if the character is not specified on the request.
			String h = getHeaderParam("Content-Type").orElse(null);
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
	@Override
	public Locale getLocale() {
		Locale best = inner.getLocale();
		String h = headers.get("Accept-Language").asString().orElse(null);
		if (h != null) {
			StringRanges sr = StringRanges.of(h);
			float qValue = 0;
			for (StringRange r : sr.toList()) {
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
	 * Returns the request header of the specified type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Header} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * <p>
	 * Typically any of the following:
	 * <ul class='javatreec'>
	 * 	<li class='jc'>{@link Accept}
	 * 	<li class='jc'>{@link AcceptCharset}
	 * 	<li class='jc'>{@link AcceptEncoding}
	 * 	<li class='jc'>{@link AcceptLanguage}
	 * 	<li class='jc'>{@link AcceptRanges}
	 * 	<li class='jc'>{@link Authorization}
	 * 	<li class='jc'>{@link CacheControl}
	 * 	<li class='jc'>{@link ClientVersion}
	 * 	<li class='jc'>{@link Connection}
	 * 	<li class='jc'>{@link ContentDisposition}
	 * 	<li class='jc'>{@link ContentEncoding}
	 * 	<li class='jc'>{@link ContentLength}
	 * 	<li class='jc'>{@link ContentType}
	 * 	<li class='jc'>{@link Date}
	 * 	<li class='jc'>{@link Debug}
	 * 	<li class='jc'>{@link Expect}
	 * 	<li class='jc'>{@link Forwarded}
	 * 	<li class='jc'>{@link From}
	 * 	<li class='jc'>{@link Host}
	 * 	<li class='jc'>{@link IfMatch}
	 * 	<li class='jc'>{@link IfModifiedSince}
	 * 	<li class='jc'>{@link IfNoneMatch}
	 * 	<li class='jc'>{@link IfRange}
	 * 	<li class='jc'>{@link IfUnmodifiedSince}
	 * 	<li class='jc'>{@link MaxForwards}
	 * 	<li class='jc'>{@link NoTrace}
	 * 	<li class='jc'>{@link Origin}
	 * 	<li class='jc'>{@link Pragma}
	 * 	<li class='jc'>{@link ProxyAuthorization}
	 * 	<li class='jc'>{@link Range}
	 * 	<li class='jc'>{@link Referer}
	 * 	<li class='jc'>{@link TE}
	 * 	<li class='jc'>{@link Thrown}
	 * 	<li class='jc'>{@link Upgrade}
	 * 	<li class='jc'>{@link UserAgent}
	 * 	<li class='jc'>{@link Warning}
	 * </ul>
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The parsed header on the request, never <jk>null</jk>.
	 */
	public <T> Optional<T> getHeader(Class<T> type) {
		return headers.get(type);
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
		String tz = headers.get("Time-Zone").asString().orElse(null);
		if (tz != null)
			return optional(TimeZone.getTimeZone(tz));
		return Optional.empty();
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
	 * <p class='bjava'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This object is modifiable.
	 * 	<li class='note'>
	 * 		Values are converted from strings using the registered part parser on the resource class.
	 * 	<li class='note'>
	 * 		The {@link RequestAttributes} object can also be passed as a parameter on the method.
	 * 	<li class='note'>
	 * 		The {@link Attr @Attr} annotation can be used to access individual attribute values.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HttpParts">HTTP Parts</a>
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
	@Override
	public RequestAttribute getAttribute(String name) {
		return attrs.get(name);
	}

	/**
	 * Sets a request attribute.
	 *
	 * @param name The attribute name.
	 * @param value The attribute value.
	 */
	@Override
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
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(...)
	 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to query parameters on the URL.</jc>
	 * 		RequestQueryParams <jv>query</jv> = <jv>req</jv>.getQuery();
	 *
	 * 		<jc>// Get query parameters converted to various types.</jc>
	 * 		<jk>int</jk> <jv>p1/</jv> = <jv>query</jv>.getInteger(<js>"p1"</js>).orElse(<jk>null</jk>);
	 * 		String <jv>p2</jv> = <jv>query</jv>.getString(<js>"p2"</js>).orElse(<jk>null</jk>);
	 * 		UUID <jv>p3</jv> = <jv>query</jv>.get(<js>"p3"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This object is modifiable.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HttpParts">HTTP Parts</a>
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
	 * Returns the request query parameter of the specified type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Query} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The parsed query parameter on the request, never <jk>null</jk>.
	 */
	public <T> Optional<T> getQueryParam(Class<T> type) {
		return queryParams.get(type);
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
	 * Similar to {@link HttpServletRequest#getParameterMap()}, but only looks for form data in the HTTP content.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPost</ja>(...)
	 * 	<jk>public void</jk> doPost(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to parsed form data parameters.</jc>
	 * 		RequestFormParams <jv>formParams</jv> = <jv>req</jv>.getFormParams();
	 *
	 * 		<jc>// Get form data parameters converted to various types.</jc>
	 * 		<jk>int</jk> <jv>p1</jv> = <jv>formParams</jv>.get(<js>"p1"</js>).asInteger().orElse(0);
	 * 		String <jv>p2</jv> = <jv>formParams</jv>.get(<js>"p2"</js>).asString().orElse(<jk>null</jk>);
	 * 		UUID <jv>p3</jv> = <jv>formParams</jv>.get(<js>"p3"</js>).as(UUID.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		This object is modifiable.
	 * 	<li class='note'>
	 * 		Values are converted from strings using the registered part parser on the resource class.
	 * 	<li class='note'>
	 * 		The {@link RequestFormParams} object can also be passed as a parameter on the method.
	 * 	<li class='note'>
	 * 		The {@link FormData @FormDAta} annotation can be used to access individual form data parameter values.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HttpParts">HTTP Parts</a>
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
	 * Returns the request form-data parameter of the specified type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.FormData} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The parsed form-data parameter on the request, never <jk>null</jk>.
	 */
	public <T> Optional<T> getFormParam(Class<T> type) {
		return getFormParams().get(type);
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
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(<js>"/{foo}/{bar}/{baz}/*"</js>)
	 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get access to path data.</jc>
	 * 		RequestPathParams <jv>pathParams</jv> = <jv>req</jv>.getPathParams();
	 *
	 * 		<jc>// Example URL:  /123/qux/true/quux</jc>
	 *
	 * 		<jk>int</jk> <jv>foo</jv> = <jv>pathParams</jv>.get(<js>"foo"</js>).asInteger().orElse(-1);  <jc>// =123</jc>
	 * 		String <jv>bar</jv> = <jv>pathParams</jv>.get(<js>"bar"</js>).orElse(<jk>null</jk>);  <jc>// =qux</jc>
	 * 		<jk>boolean</jk> <jv>baz</jv> = <jv>pathParams</jv>.get(<js>"baz"</js>).asBoolean().orElse(<jk>false</jk>);  <jc>// =true</jc>
	 * 		String <jv>remainder</jv> = <jv>pathParams</jv>.getRemainder().orElse(<jk>null</jk>);  <jc>// =quux</jc>
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
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
	 * Returns the request path parameter of the specified type.
	 *
	 * <p>
	 * Type must have a name specified via the {@link org.apache.juneau.http.annotation.Path} annotation
	 * and a public constructor that takes in either <c>value</c> or <c>name,value</c> as strings.
	 *
	 * @param <T> The bean type to create.
	 * @param type The bean type to create.
	 * @return The parsed form-data parameter on the request, never <jk>null</jk>.
	 */
	public <T> Optional<T> getPathParam(Class<T> type) {
		return pathParams.get(type);
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
	// Content methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Request content.
	 *
	 * <p>
	 * Returns a {@link RequestContent} object that encapsulates access to the HTTP request content.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPost</ja>(...)
	 * 	<jk>public void</jk> doPost(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Convert content to a linked list of Person objects.</jc>
	 * 		List&lt;Person&gt; <jv>list</jv> = <jv>req</jv>.getContent().as(LinkedList.<jk>class</jk>, Person.<jk>class</jk>);
	 * 		..
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The {@link RequestContent} object can also be passed as a parameter on the method.
	 * 	<li class='note'>
	 * 		The {@link Content @Content} annotation can be used to access the content as well.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.HttpParts">HTTP Parts</a>
	 * </ul>
	 *
	 * @return
	 * 	The content of this HTTP request.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RequestContent getContent() {
		return content;
	}

	/**
	 * Returns the HTTP content content as a {@link Reader}.
	 *
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &content=xxx} in the URL query
	 * string.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * <p>
	 * This method is equivalent to calling <c>getContent().getReader()</c>.
	 *
	 * @return The HTTP content content as a {@link Reader}.
	 * @throws IOException If content could not be read.
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		return getContent().getReader();
	}

	/**
	 * Returns the HTTP content content as an {@link InputStream}.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * <p>
	 * This method is equivalent to calling <c>getContent().getInputStream()</c>.
	 *
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it in the GZIP wrapper.
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return getContent().getInputStream();
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
	@Override
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
	@Override
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
	public URI getUri(boolean includeQuery, Map<String,Object> addQueryParams) {
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
			throw asRuntimeException(e);
		}
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
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>
	 * 	<jk>public</jk> List&lt;Tag&gt; swaggerTags(RestRequest <jv>req</jv>) {
	 * 		<jk>return</jk> <jv>req</jv>.getSwagger().getTags();
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The {@link Swagger} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(Class)}
	 * 	<li class='jm'>{@link RestContext.Builder#swaggerProvider(SwaggerProvider)}
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.Swagger">Swagger</a>
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

		return ofNullable(swagger.get().getOperation(opContext.getPathPattern(), getMethod().toLowerCase()));
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
	@Override
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
		return "true".equals(queryParams.get("plainText").asString().orElse("false"));
	}

	/**
	 * Returns the resource bundle for the request locale.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>
	 * 	<jk>public</jk> String hello(RestRequest <jv>req</jv>, <ja>@Query</ja>(<js>"user"</js>) String <jv>user</jv>) {
	 *
	 * 		<jc>// Return a localized message.</jc>
	 * 		<jk>return</jk> <jv>req</jv>.getMessages().getString(<js>"hello.message"</js>, <jv>user</jv>);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The {@link Messages} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestRequest#getMessage(String,Object...)}
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.LocalizedMessages">Localized Messages</a>
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
		return getAttribute("Debug").as(Boolean.class).orElse(false);
	}

	/**
	 * Sets the <js>"Exception"</js> attribute to the specified throwable.
	 *
	 * <p>
	 * This exception is used by {@link CallLogger} for logging purposes.
	 *
	 * @param t The attribute value.
	 * @return This object.
	 */
	public RestRequest setException(Throwable t) {
		setAttribute("Exception", t);
		return this;
	}

	/**
	 * Sets the <js>"NoTrace"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link CallLogger} and tells it not to log the current request.
	 *
	 * @param b The attribute value.
	 * @return This object.
	 */
	public RestRequest setNoTrace(Boolean b) {
		setAttribute("NoTrace", b);
		return this;
	}

	/**
	 * Shortcut for calling <c>setNoTrace(<jk>true</jk>)</c>.
	 *
	 * @return This object.
	 */
	public RestRequest setNoTrace() {
		return setNoTrace(true);
	}

	/**
	 * Sets the <js>"Debug"</js> attribute to the specified boolean.
	 *
	 * <p>
	 * This flag is used by {@link CallLogger} to help determine how a request should be logged.
	 *
	 * @param b The attribute value.
	 * @return This object.
	 * @throws IOException If content could not be cached.
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
	 * @return This object.
	 * @throws IOException If content could not be cached.
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
	 * <p class='bjava'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The {@link VarResolverSession} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext#getVarResolver()}
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.SvlVariables">SVL Variables</a>
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
	 * Returns the static files registered on the REST resource context object.
	 *
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API.
	 *
	 * @return This object.
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
	 * <p class='bjava'>
	 * 	<ja>@RestGet</ja>(...)
	 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>) {
	 *
	 * 		<jc>// Get config file.</jc>
	 * 		Config <jv>config</jv> = <jv>req</jv>.getConfig();
	 *
	 * 		<jc>// Get simple values from config file.</jc>
	 * 		<jk>int</jk> <jv>timeout</jv> = <jv>config</jv>.get(<js>"MyResource/timeout"</js>).asInteger().orElse(=10000);
	 *
	 * 		<jc>// Get complex values from config file.</jc>
	 * 		MyBean <jv>bean</jv> = <jv>config</jv>.get(<js>"MyResource/myBean"</js>).as(MyBean.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The {@link Config} object can also be passed as a parameter on the method.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jrs.ConfigurationFiles">Configuration Files</a>
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
	 * <p class='bjava'>
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
	 * @param <T> The request bean interface to instantiate.
	 * @param c The request bean interface to instantiate.
	 * @return A new request bean proxy for this REST request.
	 */
	public <T> T getRequest(Class<T> c) {
		return getRequest(RequestBeanMeta.create(c, getContext().getAnnotations()));
	}

	/**
	 * Same as {@link #getRequest(Class)} but used on pre-instantiated {@link RequestBeanMeta} objects.
	 *
	 * @param <T> The request bean interface to instantiate.
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
								return getContent().setSchema(schema).as(type);
							if (pt == QUERY)
								return getQueryParam(name).parser(pp).schema(schema).as(type).orElse(null);
							if (pt == FORMDATA)
								return getFormParam(name).parser(pp).schema(schema).as(type).orElse(null);
							if (pt == HEADER)
								return getHeaderParam(name).parser(pp).schema(schema).as(type).orElse(null);
							if (pt == PATH)
								return getPathParam(name).parser(pp).schema(schema).as(type).orElse(null);
						}
						return null;
					}

			});
		} catch (Exception e) {
			throw asRuntimeException(e);
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
		getHeaders().forEach(x -> sb.append("\t").append(x).append("\n"));
		String m = getMethod();
		if (m.equals("PUT") || m.equals("POST")) {
			try {
				sb.append("---Content UTF-8---\n");
				sb.append(content.asString()).append("\n");
				sb.append("---Content Hex---\n");
				sb.append(content.asSpacedHex()).append("\n");
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