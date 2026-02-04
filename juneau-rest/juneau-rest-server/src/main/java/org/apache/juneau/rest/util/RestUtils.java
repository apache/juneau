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
package org.apache.juneau.rest.util;

import static org.apache.juneau.commons.lang.StateEnum.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;

import jakarta.servlet.http.*;

/**
 * Various reusable utility methods.
 *
 */
@SuppressWarnings("resource")
public class RestUtils {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private RestUtils() {
		// Utility class - prevent instantiation
	}

	// @formatter:off
	private static Map<Integer,String> httpMsgs = mapb(Integer.class, String.class)
		.unmodifiable()
		.add(100, "Continue")
		.add(101, "Switching Protocols")
		.add(102, "Processing")
		.add(103, "Early Hints")
		.add(200, "OK")
		.add(201, "Created")
		.add(202, "Accepted")
		.add(203, "Non-Authoritative Information")
		.add(204, "No Content")
		.add(205, "Reset Content")
		.add(206, "Partial Content")
		.add(300, "Multiple Choices")
		.add(301, "Moved Permanently")
		.add(302, "Temporary Redirect")
		.add(303, "See Other")
		.add(304, "Not Modified")
		.add(305, "Use Proxy")
		.add(307, "Temporary Redirect")
		.add(400, "Bad Request")
		.add(401, "Unauthorized")
		.add(402, "Payment Required")
		.add(403, "Forbidden")
		.add(404, "Not Found")
		.add(405, "Method Not Allowed")
		.add(406, "Not Acceptable")
		.add(407, "Proxy Authentication Required")
		.add(408, "Request Time-Out")
		.add(409, "Conflict")
		.add(410, "Gone")
		.add(411, "Length Required")
		.add(412, "Precondition Failed")
		.add(413, "Request Entity Too Large")
		.add(414, "Request-URI Too Large")
		.add(415, "Unsupported Media Type")
		.add(500, "Internal Server Error")
		.add(501, "Not Implemented")
		.add(502, "Bad Gateway")
		.add(503, "Service Unavailable")
		.add(504, "Gateway Timeout")
		.add(505, "HTTP Version Not Supported")
		.build()
	;
	// @formatter:on

	/**
	 * Returns readable text for an HTTP response code.
	 *
	 * @param rc The HTTP response code.
	 * @return Readable text for an HTTP response code, or <jk>null</jk> if it's an invalid code.
	 */
	public static String getHttpResponseText(int rc) {
		return httpMsgs.get(rc);
	}

	/**
	 * Identical to {@link HttpServletRequest#getPathInfo()} but doesn't decode encoded characters.
	 *
	 * @param req The HTTP request
	 * @return The un-decoded path info.
	 */
	public static String getPathInfoUndecoded(HttpServletRequest req) {
		var requestURI = req.getRequestURI();
		var contextPath = req.getContextPath();
		var servletPath = req.getServletPath();
		var l = contextPath.length() + servletPath.length();
		return requestURI.length() == l ? null : requestURI.substring(l);
	}

	/**
	 * Parses a string as JSON if it appears to be JSON, otherwise returns the string as-is.
	 *
	 * <p>
	 * This method attempts to intelligently detect whether the input string is JSON or plain text.
	 * If the string appears to be JSON (starts with <c>{</c>, <c>[</c>, or other JSON indicators),
	 * it is parsed and returned as a Java object (<jk>Map</jk>, <jk>List</jk>, <jk>String</jk>, <jk>Number</jk>, etc.).
	 * Otherwise, the original string is returned unchanged.
	 *
	 * <p>
	 * This is useful when processing input that could be either a JSON value or a plain string,
	 * such as configuration values or user input that may or may not be JSON-encoded.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// JSON object is parsed</jc>
	 * 	Object <jv>result1</jv> = parseIfJson(<js>"{\"name\":\"John\"}"</js>);
	 * 	<jc>// Returns a Map with key "name" and value "John"</jc>
	 *
	 * 	<jc>// JSON array is parsed</jc>
	 * 	Object <jv>result2</jv> = parseIfJson(<js>"[1,2,3]"</js>);
	 * 	<jc>// Returns a List containing [1, 2, 3]</jc>
	 *
	 * 	<jc>// Plain string is returned as-is</jc>
	 * 	Object <jv>result3</jv> = parseIfJson(<js>"hello world"</js>);
	 * 	<jc>// Returns the string "hello world"</jc>
	 * </p>
	 *
	 * @param value The string to parse. Can be <jk>null</jk>.
	 * @return
	 * 	The parsed JSON object (if the string was JSON), or the original string (if it was not JSON).
	 * 	Returns <jk>null</jk> if the input is <jk>null</jk>.
	 * 	<br>Return type can be: <jk>Map</jk>, <jk>List</jk>, <jk>String</jk>, <jk>Number</jk>, <jk>Boolean</jk>, or <jk>null</jk>.
	 * @throws ParseException If the string appears to be JSON but contains invalid JSON syntax.
	 */
	public static Object parseIfJson(String value) throws ParseException {
		return isProbablyJson(value) ? JsonParser.DEFAULT.parse(value, Object.class) : value;
	}

	/**
	 * Parses a URL query string or form-data content from a string.
	 *
	 * <p>
	 * Parses key-value pairs from a query string format (e.g., <c>key1=value1&key2=value2</c>).
	 * Supports multiple values for the same key, which are collected into a <jk>List</jk>.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * 	<li>Empty or <jk>null</jk> strings return an empty map</li>
	 * 	<li>Keys without values (e.g., <c>key1&key2</c>) are stored with <jk>null</jk> values</li>
	 * 	<li>Keys with empty values (e.g., <c>key=</c>) are stored with empty strings</li>
	 * 	<li>Multiple occurrences of the same key append values to the list</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	Map&lt;String,List&lt;String&gt;&gt; <jv>params</jv> = parseQuery(<js>"f1=v1&f2=v2&f1=v3"</js>);
	 * 	<jv>params</jv>.get(<js>"f1"</js>);  <jc>// Returns [v1, v3]</jc>
	 * 	<jv>params</jv>.get(<js>"f2"</js>);  <jc>// Returns [v2]</jc>
	 * </p>
	 *
	 * @param qs The query string to parse. Can be <jk>null</jk> or empty.
	 * @return A map of parameter names to lists of values. Returns an empty map if the input is <jk>null</jk> or empty.
	 */
	public static Map<String,List<String>> parseQuery(String qs) {
		if (isEmpty(qs)) return Collections.emptyMap();
		return safe(()->parseQuery(new ParserPipe(qs)));
	}

	/**
	 * Parses a URL query string or form-data content from a reader.
	 *
	 * <p>
	 * Parses key-value pairs from a query string format (e.g., <c>key1=value1&key2=value2</c>).
	 * Supports multiple values for the same key, which are collected into a <jk>List</jk>.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * 	<li><jk>null</jk> readers return an empty map</li>
	 * 	<li>Keys without values (e.g., <c>key1&key2</c>) are stored with <jk>null</jk> values</li>
	 * 	<li>Keys with empty values (e.g., <c>key=</c>) are stored with empty strings</li>
	 * 	<li>Multiple occurrences of the same key append values to the list</li>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Parse from a reader</jc>
	 * 	Reader <jv>reader</jv> = <jk>new</jk> StringReader(<js>"f1=v1&f2=v2"</js>);
	 * 	Map&lt;String,List&lt;String&gt;&gt; <jv>params</jv> = parseQuery(<jv>reader</jv>);
	 * 	<jv>params</jv>.get(<js>"f1"</js>);  <jc>// Returns [v1]</jc>
	 * </p>
	 *
	 * @param qs The reader containing the query string to parse. Can be <jk>null</jk>.
	 * @return A map of parameter names to lists of values. Returns an empty map if the input is <jk>null</jk>.
	 */
	public static Map<String,List<String>> parseQuery(Reader qs) {
		if (n(qs)) return Collections.emptyMap();
		return safe(()-> parseQuery(new ParserPipe(qs)));
	}

	@SuppressWarnings("java:S2677")
	private static Map<String,List<String>> parseQuery(ParserPipe p) throws IOException {

		var m = CollectionUtils.<String,List<String>>map();

		// S1: Looking for attrName start.
		// S2: Found attrName start, looking for = or & or end.
		// S3: Found =, looking for valStart or &.
		// S4: Found valStart, looking for & or end.

		try (var r = new UonReader(p, true)) {
			var c = r.peekSkipWs();
			if (c == '?')
				r.read();

			var state = S1;
			String currAttr = null;
			while (c != -1) {
				c = r.read();
				if (state == S1) {
					if (c != -1) {
						r.unread();
						r.mark();
						state = S2;
					}
				} else if (state == S2) {
					if (c == -1) {
						add(m, r.getMarked(), null);
					} else if (c == '\u0001') {
						add(m, r.getMarked(0, -1), null);
						state = S1;
					} else if (c == '\u0002') {
						currAttr = r.getMarked(0, -1);
						state = S3;
					}
				} else if (state == S3) {
					if (c == -1 || c == '\u0001') {
						add(m, currAttr, "");
						state = S1;
					} else {
						if (c == '\u0002')
							r.replace('=');
						r.unread();
						r.mark();
						state = S4;
					}
				} else if (state == S4) {
					if (c == -1) {
						add(m, currAttr, r.getMarked());
					} else if (c == '\u0001') {
						add(m, currAttr, r.getMarked(0, -1));
						state = S1;
					} else if (c == '\u0002') {
						r.replace('=');
					}
				}
			}
		}

		return m;
	}

	/**
	 * Converts the specified path segment to a valid context path.
	 *
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param s The value to convert.
	 * @return The converted path.
	 */
	public static String toValidContextPath(String s) {
		if (s == null || s.isEmpty())
			return "";
		s = trimTrailingSlashes(s);
		if (s.isEmpty())
			return s;
		if (s.charAt(0) != '/')
			s = '/' + s;
		return s;
	}

	/**
	 * Validates that the specified value is a valid path-info path and returns it.
	 *
	 * <p>
	 * A valid path-info path must be:
	 * <ul>
	 * 	<li><jk>null</jk> (valid, indicates no extra path information)</li>
	 * 	<li>Non-empty and starting with <c>/</c> (e.g., <c>/users/123</c>)</li>
	 * </ul>
	 *
	 * <p>
	 * The path-info follows the servlet path but precedes the query string.
	 *
	 * @param value The value to validate.
	 * @return The validated value (may be <jk>null</jk>).
	 * @throws RuntimeException If the value is not a valid path-info path.
	 */
	public static String validatePathInfo(String value) {
		if (value != null && (value.isEmpty() || value.charAt(0) != '/'))
			throw rex("Value is not a valid path-info path: [{0}]", value);
		return value;
	}

	/**
	 * Validates that the specified value is a valid servlet path and returns it.
	 *
	 * <p>
	 * A valid servlet path must be:
	 * <ul>
	 * 	<li>An empty string <c>""</c> (valid, indicates servlet matched using <c>/*</c> pattern)</li>
	 * 	<li>Non-empty, starting with <c>/</c>, not ending with <c>/</c>, and not exactly <c>/</c> (e.g., <c>/api</c>, <c>/api/users</c>)</li>
	 * </ul>
	 *
	 * <p>
	 * The servlet path includes either the servlet name or a path to the servlet, but does not include any extra path information or a query string.
	 *
	 * @param value The value to validate.
	 * @return The validated value (never <jk>null</jk>).
	 * @throws RuntimeException If the value is <jk>null</jk> or not a valid servlet path.
	 */
	public static String validateServletPath(String value) {
		if (value == null)
			throw rex("Value is not a valid servlet path: [{0}]", value);
		if (! value.isEmpty() && (value.equals("/") || value.charAt(value.length() - 1) == '/' || value.charAt(0) != '/'))
			throw rex("Value is not a valid servlet path: [{0}]", value);
		return value;
	}

	private static void add(Map<String,List<String>> m, String key, String val) {
		if (val == null) {
			if (! m.containsKey(key))
				m.put(key, null);
		} else {
			m.compute(key, (k, existing) -> {
				if (existing != null)
					return addAll(existing, val);
				return list(val);
			});
		}
	}
}