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
import static java.util.Collections.*;
import static java.util.Optional.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.Date;

/**
 * Represents the headers in an HTTP request.
 *
 * <p>
 * Entries are stored in a case-insensitive map.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestmRequestHeaders}
 * </ul>
 */
public class RequestHeaders extends TreeMap<String,String[]> {
	private static final long serialVersionUID = 1L;

	private final RestRequest req;
	private HttpPartParserSession parser;
	private RequestQuery queryParams;
	private Set<String> allowedQueryParams;

	RequestHeaders(RestRequest req) {
		super(String.CASE_INSENSITIVE_ORDER);
		this.req = req;
	}

	RequestHeaders parser(HttpPartParserSession parser) {
		this.parser = parser;
		return this;
	}

	RequestHeaders queryParams(RequestQuery queryParams, Set<String> allowedQueryParams) {
		this.queryParams = queryParams;
		this.allowedQueryParams = allowedQueryParams;
		return this;
	}

	/**
	 * Adds default entries to these headers.
	 *
	 * <p>
	 * Similar to {@link #put(String, Object)} but doesn't override existing values.
	 *
	 * @param pairs
	 * 	The default entries.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public RequestHeaders addDefault(List<Header> pairs) {
		for (Header p : pairs) {
			String key = p.getName();
			Object value = p.getValue();
			String[] v = get(key);
			if (v == null || v.length == 0 || StringUtils.isEmpty(v[0]))
				put(key, stringifyAll(value));
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
	 * Returns the first header with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestHeader#exists()} can be used
	 * to test for the existence of the header.
	 *
	 * @param name The header name.
	 * @return The header.  Never <jk>null</jk>.
	 */
	public RequestHeader getFirst(String name) {
		String[] x = getValues(name);
		return new RequestHeader(req, BasicHeader.of(name, x == null ? null : x[0]));
	}

	/**
	 * Returns the last header with the specified name.
	 *
	 * <p>
	 * Note that this method never returns <jk>null</jk> and that {@link RequestHeader#exists()} can be used
	 * to test for the existence of the header.
	 *
	 * @param name The header name.
	 * @return The header.  Never <jk>null</jk>.
	 */
	public RequestHeader getLast(String name) {
		String[] x = getValues(name);
		return new RequestHeader(req, BasicHeader.of(name, x == null ? null : x[x.length-1]));
	}

	/**
	 * Returns all the headers with the specified name.
	 *
	 * @param name The header name.
	 * @return The list of all headers with the specified name, or an empty list if none are found.
	 */
	public List<RequestHeader> getAll(String name) {
		String[] x = getValues(name);
		if (x == null)
			return emptyList();
		RequestHeader[] l = new RequestHeader[x.length];
		for (int i = 0; i < x.length; i++)
			l[i] = new RequestHeader(req, BasicHeader.of(name, x[i]));
		return Arrays.asList(l);
	}

	/**
	 * Returns all the headers in this request.
	 *
	 * @return All the headers in this request.
	 */
	public List<RequestHeader> getAll() {
		List<RequestHeader> l = new ArrayList<>();
 		for (Map.Entry<String,String[]> e : entrySet())
			for (String v : e.getValue())
				l.add(new RequestHeader(req, BasicHeader.of(e.getKey(), v)));
		return l;
	}

	/**
	 * Returns the last header with the specified name as a string.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The header name.
	 * @return The header value, or {@link Optional#empty()} if it doesn't exist.
	 */
	public Optional<String> getString(String name) {
		return getLast(name).asString();
	}

	/**
	 * Returns the last header with the specified name as an integer.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or {@link Optional#empty()} if the header isn't present.
	 */
	public Optional<Integer> getInteger(String name) {
		return getLast(name).asInteger();
	}

	/**
	 * Returns the last header with the specified name as a boolean.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or {@link Optional#empty()} if the header isn't present.
	 */
	public Optional<Boolean> getBoolean(String name) {
		return getLast(name).asBoolean();
	}

	/**
	 * Returns the last header with the specified name as a list from a comma-delimited string.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or {@link Optional#empty()} if the header isn't present.
	 */
	public Optional<List<String>> getCsvArray(String name) {
		return getLast(name).asCsvArray();
	}

	/**
	 * Returns the last header with the specified name as a long.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or {@link Optional#empty()} if the header isn't present.
	 */
	public Optional<Long> getLong(String name) {
		return getLast(name).asLong();
	}

	/**
	 * Returns the last header with the specified name as a boolean.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 * </ul>
	 *
	 * @param name The HTTP header name.
	 * @return The header value, or {@link Optional#empty()} if the header isn't present.
	 */
	public Optional<java.util.Date> getDate(String name) {
		return getLast(name).asDate();
	}

	/**
	 * Sets a request header value.
	 *
	 * <p>
	 * This overwrites any previous value.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	public void put(String name, Object value) {
		super.put(name, stringifyAll(value));
	}

	/**
	 * Returns a copy of this object but only with the specified header names copied.
	 *
	 * @param headers The headers to include in the copy.
	 * @return A new headers object.
	 */
	public RequestHeaders subset(String...headers) {
		RequestHeaders rh2 = new RequestHeaders(req).parser(parser).queryParams(queryParams, allowedQueryParams);
		for (String h : headers)
			if (containsKey(h))
				rh2.put(h, get(h));
		return rh2;
	}

	/**
	 * Same as {@link #subset(String...)} but allows you to specify header names as a comma-delimited list.
	 *
	 * @param headers The headers to include in the copy.
	 * @return A new headers object.
	 */
	public RequestHeaders subset(String headers) {
		return subset(split(headers));
	}

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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Accept> getAccept() {
		return ofNullable(Accept.of(getString("Accept").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<AcceptCharset> getAcceptCharset() {
		return ofNullable(AcceptCharset.of(getString("Accept-Charset").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<AcceptEncoding> getAcceptEncoding() {
		return ofNullable(AcceptEncoding.of(getString("Accept-Encoding").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<AcceptLanguage> getAcceptLanguage() {
		return ofNullable(AcceptLanguage.of(getString("Accept-Language").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Authorization> getAuthorization() {
		return ofNullable(Authorization.of(getString("Authorization").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<CacheControl> getCacheControl() {
		return ofNullable(CacheControl.of(getString("Cache-Control").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Connection> getConnection() {
		return ofNullable(Connection.of(getString("Connection").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<ContentLength> getContentLength() {
		return ofNullable(ContentLength.of(getString("Content-Length").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<ContentType> getContentType() {
		return ofNullable(ContentType.of(getString("Content-Type").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Date> getDate() {
		return ofNullable(Date.of(getString("Date").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Expect> getExpect() {
		return ofNullable(Expect.of(getString("Expect").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<From> getFrom() {
		return ofNullable(From.of(getString("From").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Host> getHost() {
		return ofNullable(Host.of(getString("Host").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<IfMatch> getIfMatch() {
		return ofNullable(IfMatch.of(getString("If-Match").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<IfModifiedSince> getIfModifiedSince() {
		return ofNullable(IfModifiedSince.of(getString("If-Modified-Since").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<IfNoneMatch> getIfNoneMatch() {
		return ofNullable(IfNoneMatch.of(getString("If-None-Match").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<IfRange> getIfRange() {
		return ofNullable(IfRange.of(getString("If-Range").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<IfUnmodifiedSince> getIfUnmodifiedSince() {
		return ofNullable(IfUnmodifiedSince.of(getString("If-Unmodified-Since").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<MaxForwards> getMaxForwards() {
		return ofNullable(MaxForwards.of(getString("Max-Forwards").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Pragma> getPragma() {
		return ofNullable(Pragma.of(getString("Pragma").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<ProxyAuthorization> getProxyAuthorization() {
		return ofNullable(ProxyAuthorization.of(getString("Proxy-Authorization").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Range> getRange() {
		return ofNullable(Range.of(getString("Range").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Referer> getReferer() {
		return ofNullable(Referer.of(getString("Referer").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<TE> getTE() {
		return ofNullable(TE.of(getString("TE").orElse(null)));
	}

	/**
	 * Returns the <c>Time-Zone</c> header value on the request if there is one.
	 *
	 * <p>
	 * Example: <js>"GMT"</js>.
	 *
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<TimeZone> getTimeZone() {
		String tz = getString("Time-Zone").orElse(null);
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<UserAgent> getUserAgent() {
		return ofNullable(UserAgent.of(getString("User-Agent").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Upgrade> getUpgrade() {
		return ofNullable(Upgrade.of(getString("Upgrade").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Via> getVia() {
		return ofNullable(Via.of(getString("Via").orElse(null)));
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
	 * @return The parsed header on the request, or {@link Optional#empty()} if not present.
	 */
	public Optional<Warning> getWarning() {
		return ofNullable(Warning.of(getString("Warning").orElse(null)));
	}

	/**
	 * Converts the headers to a readable string.
	 *
	 * @param sorted Sort the headers by name.
	 * @return A JSON string containing the contents of the headers.
	 */
	public String toString(boolean sorted) {
		Map<String,Object> m = null;
		if (sorted)
			m = new TreeMap<>();
		else
			m = new LinkedHashMap<>();
		for (Map.Entry<String,String[]> e : this.entrySet()) {
			String[] v = e.getValue();
			m.put(e.getKey(), v.length == 1 ? v[0] : v);
		}
		return SimpleJsonSerializer.DEFAULT.toString(m);
	}

	@Override /* Object */
	public String toString() {
		return toString(false);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private String[] getValues(String name) {
		String[] v = null;
		if (queryParams != null)
			if (allowedQueryParams.contains("*") || allowedQueryParams.contains(name))
				v = queryParams.get(name, true);
		if (v == null || v.length == 0)
			v = get(name);
		if (v == null || v.length == 0)
			return null;
		return v;
	}
}
