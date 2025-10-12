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
package org.apache.juneau.rest.client;

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
import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.assertion.*;

/**
 * Represents a response from a remote REST resource.
 *
 * <p>
 * Instances of this class are created by calling the {@link RestRequest#run()} method.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a request and response, automatically closing both.</jc>
 * 	<jk>try</jk> (
 * 		<jv>RestRequest</jv> <jv>req</jv> = <jv>client</jv>.get(<js>"/myResource"</js>);
 * 		<jv>RestResponse</jv> <jv>res</jv> = <jv>req</jv>.run()
 * 	) {
 * 		String <jv>body</jv> = <jv>res</jv>.getContent().asString();
 * 	}
 * </p>
 *
 * <p>
 * Alternatively, you can rely on {@link RestRequest#close()} to automatically close the response:
 *
 * <p class='bjava'>
 * 	<jc>// Only specify RestRequest - it will close the response automatically.</jc>
 * 	<jk>try</jk> (<jv>RestRequest</jv> <jv>req</jv> = <jv>client</jv>.get(<js>"/myResource"</js>)) {
 * 		String <jv>body</jv> = <jv>req</jv>.run().getContent().asString();
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class implements {@link AutoCloseable} and can be used in try-with-resources blocks.
 * 		The {@link #close()} method allows unchecked exceptions to propagate for debuggability, 
 * 		while catching and logging checked exceptions to follow AutoCloseable best practices.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestClientBasics">juneau-rest-client Basics</a>
 * </ul>
 */
public class RestResponse implements HttpResponse, AutoCloseable {

	private final RestClient client;
	private final RestRequest request;
	private final HttpResponse response;
	private final Parser parser;
	private ResponseContent responseContent;
	private boolean isClosed;
	private HeaderList headers;

	private Map<HttpPartParser,HttpPartParserSession> partParserSessions = new IdentityHashMap<>();
	private HttpPartParserSession partParserSession;

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
		this.responseContent = new ResponseContent(client, request, this, parser);
		this.headers = HeaderList.of(this.response.getAllHeaders());
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
	 * <p>
	 * Any exceptions thrown during close are logged but not propagated.
	 *
	 * @return This object.
	 */
	public RestResponse consume() {
		close();
		return this;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Status line
	//------------------------------------------------------------------------------------------------------------------

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
	 * Returns the status line reason phrase of the response.
	 *
	 * Shortcut for calling <code>getStatusLine().getReasonPhrase()</code>.
	 *
	 * @return The status line reason phrase of the response.
	 */
	public String getReasonPhrase() {
		return getStatusLine().getReasonPhrase();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Status line assertions
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Provides the ability to perform fluent-style assertions on the response {@link StatusLine} object.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertStatus().asCode().is(200)
	 * 		.getContent().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentResponseStatusLineAssertion<RestResponse> assertStatus() {
		return new FluentResponseStatusLineAssertion<>(getStatusLine(), this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the response status code.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertStatus(200)
	 * 		.getContent().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param value The value to assert.
	 * @return A new fluent assertion object.
	 */
	public RestResponse assertStatus(int value) {
		assertStatus().asCode().is(value);
		return this;
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
		return getHeader(name).asString();
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
		return Utils.isEmpty(s) ? "utf-8" : s;
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
		return getHeader("Content-Type").as(ContentType.class);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on the response character encoding.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
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
	 * <p class='bjava'>
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
	 * 		.assertHeader(<js>"Content-Type"</js>).is(<jv>x</jv> -&gt; <jv>x</jv>.equals(<js>"application/json"</js>));
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
	 * 		.assertHeader(<js>"Content-Type"</js>).isPattern(<js>".*json.*"</js>);
	 *
	 * 	<jc>// Validates the content type is JSON using case-insensitive regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).isPattern(<js>".*json.*"</js>, <jsf>CASE_INSENSITIVE</jsf>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bjava'>
	 * 	<jc>// Validates the header and converts it to a bean.</jc>
	 * 	MediaType <jv>mediaType</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertHeader(<js>"Content-Type"</js>).isNotEmpty()
	 * 		.assertHeader(<js>"Content-Type"</js>).isPattern(<js>".*json.*"</js>)
	 * 		.getHeader(<js>"Content-Type"</js>).as(MediaType.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The header name.
	 * @return A new fluent assertion object.
	 */
	public FluentResponseHeaderAssertion<RestResponse> assertHeader(String name) {
		return new FluentResponseHeaderAssertion<>(getHeader(name), this);
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
	public ResponseContent getContent() {
		return responseContent;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().is(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body contains the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isContains(<js>"OK"</js>);
	 *
	 * 	<jc>// Validates the response body passes a predicate test.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().is(<jv>x</jv> -&gt; <jv>x</jv>.contains(<js>"OK"</js>));
	 *
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<js>".*OK.*"</js>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression using regex flags.</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<js>".*OK.*"</js>, <jsf>MULTILINE</jsf> &amp; <jsf>CASE_INSENSITIVE</jsf>);
	 *
	 * 	<jc>// Validates the response body matches a regular expression in the form of an existing Pattern.</jc>
	 * 	Pattern <jv>pattern</jv> = Pattern.<jsm>compile</jsm>(<js>".*OK.*"</js>);
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<jv>pattern</jv>);
	 * </p>
	 *
	 * <p>
	 * The assertion test returns the original response object allowing you to chain multiple requests like so:
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body matches a regular expression.</jc>
	 * 	MyBean <jv>bean</jv> = <jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent().isPattern(<js>".*OK.*"</js>);
	 * 		.assertContent().isNotPattern(<js>".*ERROR.*"</js>)
	 * 		.getContent().as(MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		If no charset was found on the <code>Content-Type</code> response header, <js>"UTF-8"</js> is assumed.
	 *  <li class='note'>
	 *		When using this method, the body is automatically cached by calling the {@link ResponseContent#cache()}.
	 * 	<li class='note'>
	 * 		The input stream is automatically closed after this call.
	 * </ul>
	 *
	 * @return A new fluent assertion object.
	 */
	public FluentResponseBodyAssertion<RestResponse> assertContent() {
		return new FluentResponseBodyAssertion<>(responseContent, this);
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <p>
	 * A shortcut for calling <c>assertContent().is(<jv>value</jv>)</c>.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Validates the response body equals the text "OK".</jc>
	 * 	<jv>client</jv>
	 * 		.get(<jsf>URI</jsf>)
	 * 		.run()
	 * 		.assertContent(<js>"OK"</js>);
	 * </p>
	 *
	 * @param value The value to assert.
	 * @return This object.
	 */
	public RestResponse assertContent(String value) {
		assertContent().is(value);
		return this;
	}

	/**
	 * Provides the ability to perform fluent-style assertions on this response body.
	 *
	 * <p>
	 * A shortcut for calling <c>assertContent().asString().isMatches(<jv>value</jv>)</c>.
	 *
	 * @see FluentStringAssertion#isMatches(String)
	 * @param value The value to assert.
	 * @return This object.
	 */
	public RestResponse assertContentMatches(String value) {
		assertContent().asString().isMatches(value);
		return this;
	}

	/**
	 * Caches the response body so that it can be read as a stream multiple times.
	 *
	 * This is equivalent to calling the following:
	 * <p class='bjava'>
	 * 	getContent().cache();
	 * </p>
	 *
	 * @return The body of the response.
	 */
	public RestResponse cacheContent() {
		responseContent.cache();
		return this;
	}

	@SuppressWarnings("unchecked")
	<T> T as(ResponseBeanMeta rbm) {
		Class<T> c = (Class<T>)rbm.getClassMeta().getInnerClass();
		final RestClient rc = this.client;
		return (T)Proxy.newProxyInstance(
			c.getClassLoader(),
			new Class[] { c },
			(InvocationHandler) (proxy, method, args) -> {
            	ResponseBeanPropertyMeta pm = rbm.getProperty(method.getName());
            	HttpPartParserSession pp = getPartParserSession(pm.getParser().orElse(rc.getPartParser()));
            	HttpPartSchema schema = pm.getSchema();
            	HttpPartType pt = pm.getPartType();
            	String name = pm.getPartName().orElse(null);
            	ClassMeta<?> type = rc.getBeanContext().getClassMeta(method.getGenericReturnType());
            	if (pt == RESPONSE_HEADER)
            		return getHeader(name).parser(pp).schema(schema).as(type).orElse(null);
            	if (pt == RESPONSE_STATUS)
            		return getStatusCode();
            	return getContent().schema(schema).as(type);
            });
	}

	/**
	 * Logs a message.
	 *
	 * @param level The log level.
	 * @param t The throwable cause.
	 * @param msg The message with {@link MessageFormat}-style arguments.
	 * @param args The arguments.
	 * @return This object.
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
	 * @return This object.
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
	public ResponseStatusLine getStatusLine() {
		return new ResponseStatusLine(this, response.getStatusLine());
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Unlike the {@link HttpResponse#getEntity()} method, this method never returns a <jk>null</jk> response.
	 * 		Instead, <c>getContent().isPresent()</c> can be used to determine whether the response has a body.
	 * </ul>
	 *
	 * @return The response entity.  Never <jk>null</jk>.
	 */
	@Override /* HttpResponse */
	public ResponseContent getEntity() {
		return responseContent;
	}

	/**
	 * Associates a response entity with this response.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>If an entity has already been set for this response and it depends on an input stream
	 * 		({@link HttpEntity#isStreaming()} returns <jk>true</jk>), it must be fully consumed in order to ensure
	 * 		release of resources.
	 * </ul>
	 *
	 * @param entity The entity to associate with this response, or <jk>null</jk> to unset.
	 */
	@Override /* HttpResponse */
	public void setEntity(HttpEntity entity) {
		response.setEntity(entity);
		this.responseContent = new ResponseContent(client, request, this, parser);
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
		return headers.stream(name).map(x -> new ResponseHeader(name, request, this, x).parser(getPartParserSession())).toArray(ResponseHeader[]::new);
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
		return new ResponseHeader(name, request, this, headers.getFirst(name).orElse(null)).parser(getPartParserSession());
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
		return new ResponseHeader(name, request, this, headers.getLast(name).orElse(null)).parser(getPartParserSession());
	}

	/**
	 * Returns the response header with the specified name.
	 *
	 * <p>
	 * If more that one header with the given name exists the values will be combined with <js>", "</js> as per <a href='https://tools.ietf.org/html/rfc2616#section-4.2'>RFC 2616 Section 4.2</a>.
	 *
	 * @param name The name of the header to return.
	 * @return The header, never <jk>null</jk>.
	 */
	public ResponseHeader getHeader(String name) {
		return new ResponseHeader(name, request, this, headers.get(name).orElse(null)).parser(getPartParserSession());
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
		return headers.stream().map(x -> new ResponseHeader(x.getName(), request, this, x).parser(getPartParserSession())).toArray(ResponseHeader[]::new);
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
		headers.append(header);
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
		headers.append(name, value);
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
		headers.set(header);
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
		headers.set(name, value);
	}

	/**
	 * Overwrites all the headers in the message.
	 *
	 * @param headers The array of headers to set.
	 */
	@Override /* HttpMessage */
	public void setHeaders(Header[] headers) {
		this.headers = HeaderList.of(headers);
	}

	/**
	 * Removes a header from this message.
	 *
	 * @param header The header to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeader(Header header) {
		headers.remove(header);
	}

	/**
	 * Removes all headers with a certain name from this message.
	 *
	 * @param name The name of the headers to remove.
	 */
	@Override /* HttpMessage */
	public void removeHeaders(String name) {
		headers.remove(name);
	}

	/**
	 * Returns an iterator of all the headers.
	 *
	 * @return {@link Iterator} that returns {@link Header} objects in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator() {
		return headers.headerIterator();
	}

	/**
	 * Returns an iterator of the headers with a given name.
	 *
	 * @param name The name of the headers over which to iterate, or <jk>null</jk> for all headers.
	 * @return {@link Iterator} that returns {@link Header} objects with the argument name in the sequence they are sent over a connection.
	 */
	@Override /* HttpMessage */
	public HeaderIterator headerIterator(String name) {
		return headers.headerIterator(name);
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

	/**
	 * Closes this response.
	 *
	 * <p>
	 * This method is idempotent and can be called multiple times without side effects.
	 *
	 * <h5 class='section'>Implementation Notes:</h5>
	 * <p>
	 * This implementation represents a compromise between strict AutoCloseable compliance and debuggability:
	 * <ul>
	 * 	<li>Unchecked exceptions ({@link RuntimeException} and {@link Error}) from interceptors are allowed to propagate.
	 * 		This ensures programming errors and serious issues are visible during development and testing.
	 * 	<li>Checked exceptions (including {@link RestCallException}) are caught and logged but not thrown. 
	 * 		This follows AutoCloseable best practices and prevents close exceptions from interfering with 
	 * 		try-with-resources cleanup or masking the original exception.
	 * </ul>
	 */
	@Override /* AutoCloseable */
	public void close() {
		if (isClosed)
			return;
		isClosed = true;

		try {
			EntityUtils.consumeQuietly(response.getEntity());

			if (!request.isLoggingSuppressed() && (request.isDebug() || client.logRequestsPredicate.test(request, this))) {
				if (client.logRequests == DetailLevel.SIMPLE) {
					client.log(client.logRequestsLevel, "HTTP {0} {1}, {2}", request.getMethod(), request.getURI(), this.getStatusLine());
				} else if (request.isDebug() || client.logRequests == DetailLevel.FULL) {
					String output = getContent().asString();
					StringBuilder sb = new StringBuilder();
					sb.append("\n=== HTTP Call (outgoing) ======================================================");
					sb.append("\n=== REQUEST ===\n");
					sb.append(request.getMethod()).append(" ").append(request.getURI());
					sb.append("\n---request headers---");
					request.getHeaders().forEach(x -> sb.append("\n\t").append(x));
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
				} catch (RuntimeException | Error e) {
					// Let unchecked exceptions propagate - these indicate programming errors that should be visible
					throw e;
				} catch (Exception e) {
					// Wrap checked exceptions from interceptors (including RestCallException)
					throw new RestCallException(this, e, "Interceptor throw exception on close");
				}
			}
			client.onCallClose(request, this);
		} catch (RuntimeException | Error e) {
			// Let unchecked exceptions propagate for debuggability
			throw e;
		} catch (Exception e) {
			// Log checked exceptions but don't throw - follows AutoCloseable best practices
			client.log(Level.WARNING, e, "Error during RestResponse close");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Creates a session of the specified part parser.
	 *
	 * @param parser The parser to create a session for.
	 * @return A session of the specified parser.
	 */
	protected HttpPartParserSession getPartParserSession(HttpPartParser parser) {
		HttpPartParserSession s = partParserSessions.get(parser);
		if (s == null) {
			s = parser.getPartSession();
			partParserSessions.put(parser, s);
		}
		return s;
	}

	/**
	 * Creates a session of the client-default parat parser.
	 *
	 * @return A session of the specified parser.
	 */
	protected HttpPartParserSession getPartParserSession() {
		if (partParserSession == null)
			partParserSession = client.getPartParser().getPartSession();
		return partParserSession;
	}

	HttpResponse asHttpResponse() {
		return response;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------
}
