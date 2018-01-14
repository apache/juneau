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

import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.Date;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;

/**
 * Represents the headers in an HTTP request.
 * 
 * <p>
 * Entries are stored in a case-insensitive map.
 */
public class RequestHeaders extends TreeMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private HttpPartParser parser;
	private BeanSession beanSession;
	private RequestQuery queryParams;

	RequestHeaders() {
		super(String.CASE_INSENSITIVE_ORDER);
	}

	RequestHeaders setParser(HttpPartParser parser) {
		this.parser = parser;
		return this;
	}

	RequestHeaders setBeanSession(BeanSession beanSession) {
		this.beanSession = beanSession;
		return this;
	}

	RequestHeaders setQueryParams(RequestQuery queryParams) {
		this.queryParams = queryParams;
		return this;
	}

	/**
	 * Adds default entries to these headers.
	 * 
	 * <p>
	 * This includes the default headers defined on the servlet and method levels.
	 * 
	 * @param defaultEntries The default entries.  Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders addDefault(Map<String,Object> defaultEntries) {
		if (defaultEntries != null) {
			for (Map.Entry<String,Object> e : defaultEntries.entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				String[] v = get(key);
				if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
					put(key, new String[]{StringUtils.toString(value)});
			}
		}
		return this;
	}

	/**
	 * Adds a set of header values to this object.
	 * 
	 * @param name The header name.
	 * @param values The header values.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders put(String name, Enumeration<String> values) {
		// Optimized for enumerations of one entry, the most-common case.
		if (values.hasMoreElements()) {
			String v = values.nextElement();
			String[] s = new String[]{v};
			while (values.hasMoreElements())
				s = append(s, values.nextElement());
			put(name, s);
		}
		return this;
	}

	/**
	 * Returns the specified header value, or <jk>null</jk> if the header doesn't exist.
	 * 
	 * <p>
	 * If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the
	 * URL query string.
	 * 
	 * @param name The header name.
	 * @return The header value, or <jk>null</jk> if it doesn't exist.
	 */
	public String getString(String name) {
		String[] v = null;
		if (queryParams != null)
			v = queryParams.get(name);
		if (v == null || v.length == 0)
			v = get(name);
		if (v == null || v.length == 0)
			return null;
		return v[0];
	}

	/**
	 * Returns the specified header value, or a default value if the header doesn't exist.
	 * 
	 * <p>
	 * If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the
	 * URL query string.
	 * 
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public String getString(String name, String def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : s;
	}

	/**
	 * Same as {@link #getString(String)} but converts the value to an integer.
	 * 
	 * @param name The HTTP header name.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Same as {@link #getString(String,String)} but converts the value to an integer.
	 * 
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public int getInt(String name, int def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Integer.parseInt(s);
	}

	/**
	 * Same as {@link #getString(String)} but converts the value to a boolean.
	 * 
	 * @param name The HTTP header name.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public boolean getBoolean(String name) {
		return getBoolean(name, false);
	}

	/**
	 * Same as {@link #getString(String,String)} but converts the value to a boolean.
	 * 
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public boolean getBoolean(String name, boolean def) {
		String s = getString(name);
		return StringUtils.isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Sets a request header value.
	 * 
	 * @param name The header name.
	 * @param value The header value.
	 */
	public void put(String name, Object value) {
		super.put(name, new String[]{StringUtils.toString(value)});
	}

	/**
	 * Returns the specified header value converted to a POJO.
	 * 
	 * <p>
	 * The type can be any POJO type convertible from a <code>String</code>
	 * (See <a class="doclink" href="package-summary.html#PojosConvertableFromString">POJOs Convertible From Strings</a>).
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myheader = req.getHeader(<js>"My-Header"</js>, <jk>int</jk>.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse a UUID.</jc>
	 * 	UUID myheader = req.getHeader(<js>"My-Header"</js>, UUID.<jk>class</jk>);
	 * </p>
	 * 
	 * @param name The HTTP header name.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException 
	 */
	public <T> T get(String name, Class<T> type) throws ParseException {
		return get(parser, name, null, type);
	}

	/**
	 * Same as {@link #get(String, Class)} but allows you to override the part parser used.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The HTTP header name.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException 
	 */
	public <T> T get(HttpPartParser parser, String name, Class<T> type) throws ParseException {
		return get(parser, name, null, type);
	}

	/**
	 * Same as {@link #get(String, Class)} but returns a default value if not found.
	 * 
	 * @param name The HTTP header name.
	 * @param def The default value if the header was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException 
	 */
	public <T> T get(String name, T def, Class<T> type) throws ParseException {
		return get(parser, name, def, type);
	}

	/**
	 * Same as {@link #get(String, Object, Class)} but allows you to override the part parser used.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name The HTTP header name.
	 * @param def The default value if the header was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException 
	 */
	public <T> T get(HttpPartParser parser, String name, T def, Class<T> type) throws ParseException {
		String s = getString(name);
		if (s == null)
			return def;
		if (parser == null)
			parser = this.parser;
		return parser.parse(HttpPartType.HEADER, s, getClassMeta(type));
	}

	/**
	 * Returns the specified header value converted to a POJO.
	 * 
	 * <p>
	 * The type can be any POJO type convertible from a <code>String</code>
	 * (See <a class="doclink" href="package-summary.html#PojosConvertableFromString">POJOs Convertible From Strings</a>).
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myheader = req.getHeader(<js>"My-Header"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * </p>
	 * 
	 * @param name The HTTP header name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException If the header could not be converted to the specified type.
	 */
	public <T> T get(String name, Type type, Type...args) throws ParseException {
		return get(null, name, type, args);
	}

	/**
	 * Same as {@link #get(String, Type, Type...)} but allows you to override the part parser used.
	 * 
	 * @param parser
	 * 	The parser to use for parsing the string header.
	 * 	<br>If <jk>null</jk>, uses the part parser defined on the servlet/method. 
	 * @param name 
	 * 	The HTTP header name.
	 * @param type
	 * 	The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException If the header could not be converted to the specified type.
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(HttpPartParser parser, String name, Type type, Type...args) throws ParseException {
		String s = getString(name);
		if (s == null)
			return null;
		if (parser == null)
			parser = this.parser;
		return (T)parser.parse(HttpPartType.HEADER, s, getClassMeta(type, args));
	}

	/**
	 * Returns a copy of this object, but only with the specified header names copied.
	 * 
	 * @param headers The headers to include in the copy.
	 * @return A new headers object.
	 */
	public RequestHeaders subset(String...headers) {
		RequestHeaders rh2 = new RequestHeaders().setParser(parser).setBeanSession(beanSession).setQueryParams(queryParams);
		for (String h : headers)
			if (containsKey(h))
				rh2.put(h, get(h));
		return rh2;
	}

	/**
	 * Same as {@link #subset(String...)}, but allows you to specify header names as a comma-delimited list.
	 * 
	 * @param headers The headers to include in the copy.
	 * @return A new headers object.
	 */
	public RequestHeaders subset(String headers) {
		return subset(split(headers));
	}

	/**
	 * Returns the <code>Accept</code> header on the request.
	 * 
	 * <p>
	 * Content-Types that are acceptable for the response.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Accept: text/plain
	 * </p>
	 * 
	 * @return The parsed <code>Accept</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Accept getAccept() {
		return Accept.forString(getString("Accept"));
	}

	/**
	 * Returns the <code>Accept-Charset</code> header on the request.
	 * 
	 * <p>
	 * Character sets that are acceptable.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Accept-Charset: utf-8
	 * </p>
	 * 
	 * @return The parsed <code>Accept-Charset</code> header on the request, or <jk>null</jk> if not found.
	 */
	public AcceptCharset getAcceptCharset() {
		return AcceptCharset.forString(getString("Accept-Charset"));
	}

	/**
	 * Returns the <code>Accept-Encoding</code> header on the request.
	 * 
	 * <p>
	 * List of acceptable encodings.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Accept-Encoding: gzip, deflate
	 * </p>
	 * 
	 * @return The parsed <code>Accept-Encoding</code> header on the request, or <jk>null</jk> if not found.
	 */
	public AcceptEncoding getAcceptEncoding() {
		return AcceptEncoding.forString(getString("Accept-Encoding"));
	}

	/**
	 * Returns the <code>Accept-Language</code> header on the request.
	 * 
	 * <p>
	 * List of acceptable human languages for response.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Accept-Language: en-US
	 * </p>
	 * 
	 * @return The parsed <code>Accept-Language</code> header on the request, or <jk>null</jk> if not found.
	 */
	public AcceptLanguage getAcceptLanguage() {
		return AcceptLanguage.forString(getString("Accept-Language"));
	}

	/**
	 * Returns the <code>Authorization</code> header on the request.
	 * 
	 * <p>
	 * Authentication credentials for HTTP authentication.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
	 * </p>
	 * 
	 * @return The parsed <code>Authorization</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Authorization getAuthorization() {
		return Authorization.forString(getString("Authorization"));
	}

	/**
	 * Returns the <code>Cache-Control</code> header on the request.
	 * 
	 * <p>
	 * Used to specify directives that must be obeyed by all caching mechanisms along the request-response chain.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Cache-Control: no-cache
	 * </p>
	 * 
	 * @return The parsed <code>Cache-Control</code> header on the request, or <jk>null</jk> if not found.
	 */
	public CacheControl getCacheControl() {
		return CacheControl.forString(getString("Cache-Control"));
	}

	/**
	 * Returns the <code>Connection</code> header on the request.
	 * 
	 * <p>
	 * Control options for the current connection and list of hop-by-hop request fields.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Connection: keep-alive
	 * 	Connection: Upgrade
	 * </p>
	 * 
	 * @return The parsed <code></code> header on the request, or <jk>null</jk> if not found.
	 */
	public Connection getConnection() {
		return Connection.forString(getString("Connection"));
	}

	/**
	 * Returns the <code>Content-Length</code> header on the request.
	 * 
	 * <p>
	 * The length of the request body in octets (8-bit bytes).
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Content-Length: 348
	 * </p>
	 * 
	 * @return The parsed <code>Content-Length</code> header on the request, or <jk>null</jk> if not found.
	 */
	public ContentLength getContentLength() {
		return ContentLength.forString(getString("Content-Length"));
	}

	/**
	 * Returns the <code>Content-Type</code> header on the request.
	 * 
	 * <p>
	 * The MIME type of the body of the request (used with POST and PUT requests).
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Content-Type: application/x-www-form-urlencoded
	 * </p>
	 * 
	 * @return The parsed <code>Content-Type</code> header on the request, or <jk>null</jk> if not found.
	 */
	public ContentType getContentType() {
		return ContentType.forString(getString("Content-Type"));
	}

	/**
	 * Returns the <code>Date</code> header on the request.
	 * 
	 * <p>
	 * The date and time that the message was originated (in "HTTP-date" format as defined by RFC 7231 Date/Time Formats).
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Date: Tue, 15 Nov 1994 08:12:31 GMT
	 * </p>
	 * 
	 * @return The parsed <code>Date</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Date getDate() {
		return Date.forString(getString("Date"));
	}

	/**
	 * Returns the <code>Expect</code> header on the request.
	 * 
	 * <p>
	 * Indicates that particular server behaviors are required by the client.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Expect: 100-continue
	 * </p>
	 * 
	 * @return The parsed <code>Expect</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Expect getExpect() {
		return Expect.forString(getString("Expect"));
	}

	/**
	 * Returns the <code>From</code> header on the request.
	 * 
	 * <p>
	 * The email address of the user making the request.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	From: user@example.com
	 * </p>
	 * 
	 * @return The parsed <code>From</code> header on the request, or <jk>null</jk> if not found.
	 */
	public From getFrom() {
		return From.forString(getString("From"));
	}

	/**
	 * Returns the <code>Host</code> header on the request.
	 * 
	 * <p>
	 * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening.
	 * The port number may be omitted if the port is the standard port for the service requested.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Host: en.wikipedia.org:8080
	 * 	Host: en.wikipedia.org
	 * </p>
	 * 
	 * @return The parsed <code>Host</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Host getHost() {
		return Host.forString(getString("Host"));
	}

	/**
	 * Returns the <code>If-Match</code> header on the request.
	 * 
	 * <p>
	 * Only perform the action if the client supplied entity matches the same entity on the server.
	 * This is mainly for methods like PUT to only update a resource if it has not been modified since the user last
	 * updated it.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	If-Match: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 * 
	 * @return The parsed <code>If-Match</code> header on the request, or <jk>null</jk> if not found.
	 */
	public IfMatch getIfMatch() {
		return IfMatch.forString(getString("If-Match"));
	}

	/**
	 * Returns the <code>If-Modified-Since</code> header on the request.
	 * 
	 * <p>
	 * Allows a 304 Not Modified to be returned if content is unchanged.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
	 * </p>
	 * 
	 * @return The parsed <code>If-Modified-Since</code> header on the request, or <jk>null</jk> if not found.
	 */
	public IfModifiedSince getIfModifiedSince() {
		return IfModifiedSince.forString(getString("If-Modified-Since"));
	}

	/**
	 * Returns the <code>If-None-Match</code> header on the request.
	 * 
	 * <p>
	 * Allows a 304 Not Modified to be returned if content is unchanged, see HTTP ETag.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	If-None-Match: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 * 
	 * @return The parsed <code>If-None-Match</code> header on the request, or <jk>null</jk> if not found.
	 */
	public IfNoneMatch getIfNoneMatch() {
		return IfNoneMatch.forString(getString("If-None-Match"));
	}

	/**
	 * Returns the <code>If-Range</code> header on the request.
	 * 
	 * <p>
	 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire new entity.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	If-Range: "737060cd8c284d8af7ad3082f209582d"
	 * </p>
	 * 
	 * @return The parsed <code>If-Range</code> header on the request, or <jk>null</jk> if not found.
	 */
	public IfRange getIfRange() {
		return IfRange.forString(getString("If-Range"));
	}

	/**
	 * Returns the <code>If-Unmodified-Since</code> header on the request.
	 * 
	 * <p>
	 * Only send the response if the entity has not been modified since a specific time.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
	 * </p>
	 * 
	 * @return The parsed <code>If-Unmodified-Since</code> header on the request, or <jk>null</jk> if not found.
	 */
	public IfUnmodifiedSince getIfUnmodifiedSince() {
		return IfUnmodifiedSince.forString(getString("If-Unmodified-Since"));
	}

	/**
	 * Returns the <code>Max-Forwards</code> header on the request.
	 * 
	 * <p>
	 * Limit the number of times the message can be forwarded through proxies or gateways.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Max-Forwards: 10
	 * </p>
	 * 
	 * @return The parsed <code>Max-Forwards</code> header on the request, or <jk>null</jk> if not found.
	 */
	public MaxForwards getMaxForwards() {
		return MaxForwards.forString(getString("Max-Forwards"));
	}

	/**
	 * Returns the <code>Pragma</code> header on the request.
	 * 
	 * <p>
	 * Implementation-specific fields that may have various effects anywhere along the request-response chain.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Pragma: no-cache
	 * </p>
	 * 
	 * @return The parsed <code>Pragma</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Pragma getPragma() {
		return Pragma.forString(getString("Pragma"));
	}

	/**
	 * Returns the <code>Proxy-Authorization</code> header on the request.
	 * 
	 * <p>
	 * Authorization credentials for connecting to a proxy.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Proxy-Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
	 * </p>
	 * 
	 * @return The parsed <code>Proxy-Authorization</code> header on the request, or <jk>null</jk> if not found.
	 */
	public ProxyAuthorization getProxyAuthorization() {
		return ProxyAuthorization.forString(getString("Proxy-Authorization"));
	}

	/**
	 * Returns the <code>Range</code> header on the request.
	 * 
	 * <p>
	 * Request only part of an entity. Bytes are numbered from 0.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Range: bytes=500-999
	 * </p>
	 * 
	 * @return The parsed <code>Range</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Range getRange() {
		return Range.forString(getString("Range"));
	}

	/**
	 * Returns the <code>Referer</code> header on the request.
	 * 
	 * <p>
	 * This is the address of the previous web page from which a link to the currently requested page was followed.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Referer: http://en.wikipedia.org/wiki/Main_Page
	 * </p>
	 * 
	 * @return The parsed <code>Referer</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Referer getReferer() {
		return Referer.forString(getString("Referer"));
	}

	/**
	 * Returns the <code>TE</code> header on the request.
	 * 
	 * <p>
	 * The transfer encodings the user agent is willing to accept: the same values as for the response header field
	 * Transfer-Encoding can be used, plus the "trailers" value (related to the "chunked" transfer method) to notify the
	 * server it expects to receive additional fields in the trailer after the last, zero-sized, chunk.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	TE: trailers, deflate
	 * </p>
	 * 
	 * @return The parsed <code>TE</code> header on the request, or <jk>null</jk> if not found.
	 */
	public TE getTE() {
		return TE.forString(getString("TE"));
	}

	/**
	 * Returns the <code>Time-Zone</code> header value on the request if there is one.
	 * 
	 * <p>
	 * Example: <js>"GMT"</js>.
	 * 
	 * @return The <code>Time-Zone</code> header value on the request, or <jk>null</jk> if not present.
	 */
	public TimeZone getTimeZone() {
		String tz = getString("Time-Zone");
		if (tz != null)
			return TimeZone.getTimeZone(tz);
		return null;
	}

	/**
	 * Returns the <code>User-Agent</code> header on the request.
	 * 
	 * <p>
	 * The user agent string of the user agent.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
	 * </p>
	 * 
	 * @return The parsed <code>User-Agent</code> header on the request, or <jk>null</jk> if not found.
	 */
	public UserAgent getUserAgent() {
		return UserAgent.forString(getString("User-Agent"));
	}

	/**
	 * Returns the <code>Upgrade</code> header on the request.
	 * 
	 * <p>
	 * Ask the server to upgrade to another protocol.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Upgrade: HTTP/2.0, HTTPS/1.3, IRC/6.9, RTA/x11, websocket
	 * </p>
	 * 
	 * @return The parsed <code>Upgrade</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Upgrade getUpgrade() {
		return Upgrade.forString(getString("Upgrade"));
	}

	/**
	 * Returns the <code>Via</code> header on the request.
	 * 
	 * <p>
	 * Informs the server of proxies through which the request was sent.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Via: 1.0 fred, 1.1 example.com (Apache/1.1)
	 * </p>
	 * 
	 * @return The parsed <code>Via</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Via getVia() {
		return Via.forString(getString("Via"));
	}

	/**
	 * Returns the <code>Warning</code> header on the request.
	 * 
	 * <p>
	 * A general warning about possible problems with the entity body.
	 * 
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
	 * 	Warning: 199 Miscellaneous warning
	 * </p>
	 * 
	 * @return The parsed <code>Warning</code> header on the request, or <jk>null</jk> if not found.
	 */
	public Warning getWarning() {
		return Warning.forString(getString("Warning"));
	}

	/**
	 * Converts the headers to a readable string.
	 * 
	 * @param sorted Sort the headers by name.
	 * @return A JSON string containing the contents of the headers.
	 */
	public String toString(boolean sorted) {
		Map<String,Object> m = (sorted ? new TreeMap<String,Object>() : new LinkedHashMap<String,Object>());
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			String[] v = e.getValue();
			m.put(e.getKey(), v.length == 1 ? v[0] : v);
		}
		return JsonSerializer.DEFAULT_LAX.toString(m);
	}
	
	private ClassMeta<?> getClassMeta(Type type, Type...args) {
		return beanSession.getClassMeta(type, args);
	}

	private <T> ClassMeta<T> getClassMeta(Class<T> type) {
		return beanSession.getClassMeta(type);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}
}
