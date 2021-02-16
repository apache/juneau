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

import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.assertion.*;

import static org.apache.juneau.httppart.HttpPartType.*;

import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.util.*;
import org.apache.juneau.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * Represents a response from a remote REST resource.
 *
 * <p>
 * Instances of this class are created by calling the {@link RestRequest#run()} method.
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link RestClient}
 * 	<li class='link'>{@doc juneau-rest-client}
 * </ul>
 */
public class RestResponse implements HttpResponse {

	private final RestClient client;
	private final RestRequest request;
	private final HttpResponse response;
	private final Parser parser;
	HttpPartParserSession partParser;
	private ResponseBody responseBody;
	private boolean isClosed;

	/**
	 * Constructor.
	 *
	 * @param client The RestClient that created this response.
	 * @param request The REST request.
	 * @param response The HTTP response.  Can be <jk>null</jk>.
	 * @param parser The overridden parser passed into {@link RestRequest#parser(Parser)}.
	 */
	protected RestResponse(RestClient client, RestRequest request, HttpResponse response, Parser parser) {
		this.client = client;
		this.request = request;
		this.parser = parser;
		this.response = response == null ? new BasicHttpResponse(null, 0, null) : response;
		this.responseBody = new ResponseBody(client, request, this, parser);
		this.partParser = client.getPartParserSession();
	}

	/**
	 * Returns the request object that created this response object.
	 *
	 * @return The request object that created this response object.
	 */
	public RestRequest getRequest() {
		return request;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Setters
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Consumes the response body.
	 *
	 * <p>
	 * This is equivalent to closing the input stream.
	 *
	 * @return This object (for method chaining).
	 * @throws RestCallException If one of the {@link RestCallInterceptor RestCallInterceptors} threw an exception.
	 */
	public RestResponse consume() throws RestCallException {
		close();
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Status line
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Same as {@link #getStatusLine()} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the status line in.
	 * @return This object (for method chaining).
	 */
	public RestResponse getStatusLine(Mutable<StatusLine> m) {
		m.set(getStatusLine());
		return this;
	}

	/**
	 * Returns the status code of the response.
	 *
	 * Shortcut for calling <code>getStatusLine().getStatusCode()</code>.
	 *
	 * @return The status code of the response.
	 */
	public int getStatusCode() {
		return getStatusLine().getStatusCode();
	}

	/**
	 * Same as {@link #getStatusCode()} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the status code in.
	 * @return This object (for method chaining).
	 */
	public RestResponse getStatusCode(Mutable<Integer> m) {
		m.set(getStatusCode());
		return this;
	}

	/**
	 * Returns the status line reason phrase of the response.
	 *
	 * Shortcut for calling <code>getStatusLine().getReasonPhrase()</code>.
	 *
	 * @return The status line reason phrase of the response.
	 */
	public String getReasonPhrase() {
		return getStatusLine().getReasonPhrase();
	}

	/**
	 * Same as {@link #getReasonPhrase()} but sets the value in a mutable for fluent calls.
	 *
	 * @param m The mutable to set the status line reason phrase in.
	 * @return This object (for method chaining).
	 */
	public RestResponse getReasonPhrase(Mutable<String> m) {
		m.set(getReasonPhrase());
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Status line assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on the response {@link StatusLine} object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertStatus().code().is(200)
	 * 		.getBody().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public ResponseStatusLineAssertion assertStatus() throws RestCallException {
		return new ResponseStatusLineAssertion(getStatusLine(), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the response status code.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertCode().is(200)
	 * 		.getBody().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public FluentIntegerAssertion<RestResponse> assertCode() throws RestCallException {
		return assertStatus().code();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Shortcut for calling <code>getHeader(name).asString()</code>.
	 *
	 * @param name The header name.
	 * @return The header value, never <jk>null</jk>
	 */
	public Optional<String> getStringHeader(String name) {
		return getResponseHeader(name).asString();
	}

	/**
	 * Returns the last header with the specified name.
	 *
	 * Unlike {@link #getFirstHeader(String)} and {@link #getLastHeader(String)}, this method returns an empty
	 * {@link ResponseHeader} object instead of returning <jk>null</jk>.  This allows it to be used more easily
	 * in fluent calls.
	 *
	 * @param name The header name.
	 * @return The header.  Never <jk>null</jk>.
	 */
	public ResponseHeader getResponseHeader(String name) {
		return new ResponseHeader(request, this, getLastHeader(name)).parser(partParser);
	}

	/**
	 * Shortcut for retrieving the response charset from the <l>Content-Type</l> header.
	 *
	 * @return The response charset.
	 * @throws RestCallException If REST call failed.
	 */
	public String getCharacterEncoding() throws RestCallException {
		Optional<ContentType> ct = getContentType();
		String s = null;
		if (ct.isPresent())
			s = getContentType().get().getParameter("charset");
		return StringUtils.isEmpty(s) ? "utf-8" : s;
	}

	/**
	 * Shortcut for retrieving the response content type from the <l>Content-Type</l> header.
	 *
	 * <p>
	 * This is equivalent to calling <c>getHeader(<js>"Content-Type"</js>).as(ContentType.<jk>class</jk>)</c>.
	 *
	 * @return The response charset.
	 * @throws RestCallException If REST call failed.
	 */
	public Optional<ContentType> getContentType() throws RestCallException {
		return getResponseHeader("Content-Type").as(ContentType.class);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the response character encoding.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates that the response content charset is UTF-8.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertCharset().is(<js>"utf-8"</js>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public FluentStringAssertion<RestResponse> assertCharset() throws RestCallException {
		return new FluentStringAssertion<>(getCharacterEncoding(), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on a response header.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the content type header is provided.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).exists();
	 *
	 * 	<jc>// Validates the content type is JSON.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).is(<js>"application/json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using test predicate.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).passes(<jv>x</jv> -&gt; <jv>x</jv>.equals(<js>"application/json"</js>));
	 *
	 * 	<jc>// Validates the content type is JSON by just checking for substring.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).contains(<js>"json"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the header and converts it to a bean.</jc>
	 * 	MediaType <jv>mediaType</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).exists()
	 * 		.assertHeader(<js>"Content-Type"</js>).matches(<js>".*json.*"</js>)
	 * 		.getHeader(<js>"Content-Type"</js>).as(MediaType.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The header name.
	 * @return A new fluent assertion object.
	 */
	public FluentResponseHeaderAssertion<RestResponse> assertHeader(String name) {
		return new FluentResponseHeaderAssertion<>(getLastHeader(name), this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Body
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the body of the response.
	 *
	 * This method can be called multiple times returning the same response body each time.
	 *
	 * @return The body of the response.
	 */
	public ResponseBody getBody() {
		return responseBody;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().equals(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().contains(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body passes a predicate test.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().passes(<jv>x</jv> -&gt; <jv>x</jv>.contains(<js>"OK"</js>));
	 *
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
	 * 	Pattern <jv>p</jv> = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<jv>p</jv>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBody().matches(<js>".*OK.*"</js>);
	 * 		.assertBody().doesNotMatch(<js>".*ERROR.*"</js>)
	 * 		.getBody().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public FluentStringAssertion<RestResponse> assertBody() throws RestCallException {
		return responseBody.cache().assertString();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the bytes of the response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body equals the text "foo".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertBodyBytes().hex().is(<js>"666F6F"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public FluentByteArrayAssertion<RestResponse> assertBodyBytes() throws RestCallException {
		return responseBody.cache().assertBytes();
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <p>
	 * <p>
	 * Combines the functionality of {@link ResponseBody#as(Class)} with {@link #assertBody()} by converting the body to the specified
	 * bean and then serializing it to simplified JSON for easy string comparison.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Validates the response body bean is the expected value.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<js>"/myBean"</js>)
	 * 		.run()
	 * 		.assertBody(MyBean.<jk>class</jk>).json().is(<js>"{foo:'bar'}"</js>);
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseBody#cache()}.
	 * 	<li>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @param type The object type to create.
	 * @return A new fluent assertion object.
	 * @throws RestCallException If REST call failed.
	 */
	public <V> FluentObjectAssertion<V,RestResponse> assertBody(Class<V> type) throws RestCallException {
		return responseBody.cache().assertObject(type);
	}

	/**
	 * Caches the response body so that it can be read as a stream multiple times.
	 *
	 * This is equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	getBody().cache();
	 * </p>
	 *
	 * @return The body of the response.
	 */
	public RestResponse cacheBody() {
		responseBody.cache();
		return this;
	}

	@SuppressWarnings("unchecked")
	<T> T as(ResponseBeanMeta rbm) {
		Class<T> c = (Class<T>)rbm.getClassMeta().getInnerClass();
		final RestClient rc = this.client;
		return (T)Proxy.newProxyInstance(
			c.getClassLoader(),
			new Class[] { c },
			new InvocationHandler() {
				@Override /* InvocationHandler */
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					ResponseBeanPropertyMeta pm = rbm.getProperty(method.getName());
					HttpPartParserSession pp = pm.getParser(partParser);
					HttpPartSchema schema = pm.getSchema();
					HttpPartType pt = pm.getPartType();
					String name = pm.getPartName();
					ClassMeta<?> type = rc.getClassMeta(method.getGenericReturnType());
					if (pt == RESPONSE_HEADER)
						return getResponseHeader(name).parser(pp).schema(schema).as(type).orElse(null);
					if (pt == RESPONSE_STATUS)
						return getStatusCode();
					return getBody().schema(schema).as(type);
				}
		});
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param t The throwable cause.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 * @return This object (for method chaining).
	 */
	public RestResponse log(Level level, Throwable t, String msg, Object...args) {
		client.log(level, t, msg, args);
		return this;
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 * @return This object (for method chaining).
	 */
	public RestResponse log(Level level, String msg, Object...args) {
		client.log(level, msg, args);
		return this;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// HttpResponse pass-through methods.
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Obtains the status line of this response.
	 *
	 * The status line can be set using one of the setStatusLine methods, or it can be initialized in a constructor.
	 *
	 * @return The status line, or <jk>null</jk> if not yet set.
	 */
	@Override /* HttpResponse */
	public StatusLine getStatusLine() {
		return response.getStatusLine();
	}

	/**
	 * Sets the status line of this response.
	 *
	 * @param statusline The status line of this response
	 */
	@Override /* HttpResponse */
	public void setStatusLine(StatusLine statusline) {
		response.setStatusLine(statusline);
	}

	/**
	 * Sets the status line of this response.
	 *
	 * <p>
	 * The reason phrase will be determined based on the current locale.
	 *
	 * @param ver The HTTP version.
	 * @param code The status code.
	 */
	@Override /* HttpResponse */
	public void setStatusLine(ProtocolVersion ver, int code) {
		response.setStatusLine(ver, code);
	}

	/**
	 * Sets the status line of this response with a reason phrase.
	 *
	 * @param ver The HTTP version.
	 * @param code The status code.
	 * @param reason The reason phrase, or <jk>null</jk> to omit.
	 */
	@Override /* HttpResponse */
	public void setStatusLine(ProtocolVersion ver, int code, String reason) {
		response.setStatusLine(ver, code, reason);
	}

	/**
	 * Updates the status line of this response with a new status code.
	 *
	 * @param code The HTTP status code.
	 * @throws IllegalStateException If the status line has not be set.
	 */
	@Override /* HttpResponse */
	public void setStatusCode(int code) {
		response.setStatusCode(code);
	}

	/**
	 * Updates the status line of this response with a new reason phrase.
	 *
	 * @param reason The new reason phrase as a single-line string, or <jk>null</jk> to unset the reason phrase.
	 * @throws IllegalStateException If the status line has not be set.
	 */
	@Override /* HttpResponse */
	public void setReasonPhrase(String reason) {
		response.setReasonPhrase(reason);
	}

	/**
	 * Obtains the message entity of this response.
	 *
	 * <p>
	 * The entity is provided by calling setEntity.
	 *
	 * <ul class='notes'>
	 * 	<li>Unlike the {@link HttpResponse#getEntity()} method, this method never returns a <jk>null</jk> response.
	 * 		Instead, <c>getBody().isPresent()</c> can be used to determine whether the response has a body.
	 * </ul>
	 *
	 * @return The response entity.  Never <jk>null</jk>.
	 */
	@Override /* HttpResponse */
	public ResponseBody getEntity() {
		return responseBody;
	}

	/**
	 * Associates a response entity with this response.
	 *
	 * <ul class='notes'>
	 * 	<li>If an entity has already been set for this response and it depends on an input stream
	 * 		({@link HttpEntity#isStreaming()} returns <jk>true</jk>), it must be fully consumed in order to ensure
	 * 		release of resources.
	 * </ul>
	 *
	 * @param entity The entity to associate with this response, or <jk>null</jk> to unset.
	 */
	@Override /* HttpResponse */
	public void setEntity(HttpEntity entity) {
		response.setEntity(entity);
		this.responseBody = new ResponseBody(client, request, this, parser);
	}

	/**
	 * Obtains the locale of this response.
	 *
	 * The locale is used to determine the reason phrase for the status code.
	 * It can be changed using {@link #setLocale(Locale)}.
	 *
	 * @return The locale of this response, never <jk>null</jk>.
	 */
	@Override /* HttpResponse */
	public Locale getLocale() {
		return response.getLocale();
	}

	/**
	 * Changes the locale of this response.
	 *
	 * @param loc The new locale.
	 */
	@Override /* HttpResponse */
	public void setLocale(Locale loc) {
		response.setLocale(loc);
	}

	/**
	 * Returns the protocol version this message is compatible with.
	 *
	 * @return The protocol version this message is compatible with.
	 */
	@Override /* HttpMessage */
	public ProtocolVersion getProtocolVersion() {
		return response.getProtocolVersion();
	}

	/**
	 * Checks if a certain header is present in this message.
	 *
	 * <p>
	 * Header values are ignored.
	 *
	 * @param name The header name to check for.
	 * @return <jk>true</jk> if at least one header with this name is present.
	 */
	@Override /* HttpMessage */
	public boolean containsHeader(String name) {
		return response.containsHeader(name);
	}

	/**
	 * Returns all the headers with a specified name of this message.
	 *
	 * Header values are ignored.
	 * <br>Headers are ordered in the sequence they were sent over a connection.
	 *
	 * @param name The name of the headers to return.
	 * @return All the headers with a specified name of this message.
	 */
	@Override /* HttpMessage */
	public ResponseHeader[] getHeaders(String name) {
		Header[] a = response.getHeaders(name);
		ResponseHeader[] b = new ResponseHeader[a.length];
		for (int i = 0; i < a.length; i++)
			b[i] = new ResponseHeader(request, this, a[i]).parser(partParser);
		return b;
	}

	/**
	 * Returns the first header with a specified name of this message.
	 *
	 * <p>
	 * If there is more than one matching header in the message the first element of {@link #getHeaders(String)} is returned.
	 * <p>
	 * This method always returns a value so that you can perform assertions on the result.
	 *
	 * @param name The name of the header to return.
	 * @return The header, never <jk>null</jk>.
	 */
	@Override /* HttpMessage */
	public ResponseHeader getFirstHeader(String name) {
		Header h = response.getFirstHeader(name);
		return new ResponseHeader(request, this, h).parser(partParser);
	}

	/**
	 * Returns the last header with a specified name of this message.
	 *
	 * <p>
	 * If there is more than one matching header in the message the last element of {@link #getHeaders(String)} is returned.
	 * <p>
	 * This method always returns a value so that you can perform assertions on the result.
	 *
	 * @param name The name of the header to return.
	 * @return The header, never <jk>null</jk>.
	 */
	@Override /* HttpMessage */
	public ResponseHeader getLastHeader(String name) {
		Header h = response.getLastHeader(name);
		return new ResponseHeader(request, this, h).parser(partParser);
	}

	/**
	 * Returns all the headers of this message.
	 *
	 * Headers are ordered in the sequence they were sent over a connection.
	 *
	 * @return All the headers of this message.
	 */
	@Override /* HttpMessage */
	public ResponseHeader[] getAllHeaders() {
		Header[] a = response.getAllHeaders();
		ResponseHeader[] b = new ResponseHeader[a.length];
		for (int i = 0; i < a.length; i++)
			b[i] = new ResponseHeader(request, this, a[i]).parser(partParser);
		return b;
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * @param header The header to append.
	 */
	@Override /* HttpMessage */
	public void addHeader(Header header) {
		response.addHeader(header);
	}

	/**
	 * Adds a header to this message.
	 *
	 * The header will be appended to the end of the list.
	 *
	 * @param name The name of the header.
	 * @param value The value of the header.
	 */
	@Override /* HttpMessage */
	public void addHeader(String name, String value) {
		response.addHeader(name, value);
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
		response.setHeader(header);
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
		response.setHeader(name, value);
	}

	/**
	 * Overwrites all the headers in the message.
	 *
	 * @param headers The array of headers to set.
	 */
	@Override /* HttpMessage */
	public void setHeaders(Header[] headers) {
		response.setHeaders(headers);
	}

	/**
	 * Removes a header from this message.
	 *
	 * @param header The header to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeader(Header header) {
		response.removeHeader(header);
	}

	/**
	 * Removes all headers with a certain name from this message.
	 *
	 * @param name The name of the headers to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		response.removeHeaders(name);
	}

	/**
	 * Returns an iterator of all the headers.
	 *
	 * @return {@link Iterator} that returns {@link Header} objects in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return response.headerIterator();
	}

	/**
	 * Returns an iterator of the headers with a given name.
	 *
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all headers.
	 * @return {@link Iterator} that returns {@link Header} objects with the argument name in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return response.headerIterator(name);
	}

	/**
	 * Returns the parameters effective for this message as set by {@link #setParams(HttpParams)}.
	 *
	 * @return The parameters effective for this message as set by {@link #setParams(HttpParams)}.
	 * @deprecated Use configuration classes provided <jk>org.apache.http.config</jk> and <jk>org.apache.http.client.config</jk>.
	 */
	@Override /* HttpMessage */
	@Deprecated
	public HttpParams getParams() {
		return response.getParams();
	}

	/**
	 * Provides parameters to be used for the processing of this message.
	 *
	 * @param params The parameters.
	 * @deprecated Use configuration classes provided <jk>org.apache.http.config</jk> and <jk>org.apache.http.client.config</jk>.
	 */
	@Override /* HttpMessage */
	@Deprecated
	public void setParams(HttpParams params) {
		response.setParams(params);
	}

	void close() throws RestCallException {
		if (isClosed)
			return;
		isClosed = true;
		EntityUtils.consumeQuietly(response.getEntity());

		if (request.isDebug() || client.logRequestsPredicate.test(request, this)) {
			if (client.logRequests == DetailLevel.SIMPLE) {
				client.log(client.logRequestsLevel, "HTTP {0} {1}, {2}", request.getMethod(), request.getURI(), this.getStatusLine());
			} else if (request.isDebug() || client.logRequests == DetailLevel.FULL) {
				String output = getBody().asString();
				StringBuilder sb = new StringBuilder();
				sb.append("\n=== HTTP Call (outgoing) ======================================================");
				sb.append("\n=== REQUEST ===\n");
				sb.append(request.getMethod()).append(" ").append(request.getURI());
				sb.append("\n---request headers---");
				for (Header h : request.getAllHeaders())
					sb.append("\n\t").append(h);
				if (request.hasHttpEntity()) {
					sb.append("\n---request entity---");
					HttpEntity e = request.getHttpEntity();
					if (e.getContentType() != null)
						sb.append("\n\t").append(e.getContentType());
					if (e.isRepeatable()) {
						try {
							sb.append("\n---request content---\n").append(EntityUtils.toString(e));
						} catch (Exception ex) {
							sb.append("\n---request content exception---\n").append(ex.getMessage());
						}
					}
				}
				sb.append("\n=== RESPONSE ===\n").append(getStatusLine());
				sb.append("\n---response headers---");
				for (Header h : getAllHeaders())
					sb.append("\n\t").append(h);
				sb.append("\n---response content---\n").append(output);
				sb.append("\n=== END =======================================================================");
				client.log(client.logRequestsLevel, sb.toString());
			}
		}

		for (RestCallInterceptor r : request.interceptors) {
			try {
				r.onClose(request, this);
			} catch (RuntimeException | RestCallException e) {
				throw e;
			} catch (Exception e) {
				throw new RestCallException(this, e, "Interceptor throw exception on close");
			}
		}
		client.onClose(request, this);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
	//------------------------------------------------------------------------------------------------------------------

	HttpResponse asHttpResponse() {
		return response;
	}
}
