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
package org.apache.juneau.rest.mock2;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;

/**
 * An implementation of {@link HttpServletResponse} for mocking purposes.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRest}
 * </ul>
*/
public class MockServletResponse implements HttpServletResponse, MockHttpResponse {

	private String characterEncoding = "UTF-8";
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private long contentLength = 0;
	private int bufferSize = 0;
	private Locale locale;
	private int sc;
	private String msg;
	private Map<String,String[]> headerMap = new LinkedHashMap<>();

	/**
	 * Creates a new servlet response.
	 *
	 * @return A new response.
	 */
	public static MockServletResponse create() {
		return new MockServletResponse();
	}

	/**
	 * Returns the content length.
	 *
	 * @return The content length.
	 */
	public long getContentLength() {
		return contentLength;
	}

	/**
	 * Returns the response message.
	 *
	 * @return The response message.
	 */
	@Override /* MockHttpResponse */
	public String getMessage() {
		return msg;
	}

	@Override /* HttpServletResponse */
	public String getCharacterEncoding() {
		return characterEncoding ;
	}

	@Override /* HttpServletResponse */
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override /* HttpServletResponse */
	public ServletOutputStream getOutputStream() throws IOException {
		return new FinishableServletOutputStream(baos);
	}

	@Override /* HttpServletResponse */
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(new OutputStreamWriter(getOutputStream(), characterEncoding));
	}

	@Override /* HttpServletResponse */
	public void setCharacterEncoding(String charset) {
		this.characterEncoding = charset;
	}

	/**
	 * Fluent setter for {@link #setCharacterEncoding(String)}.
	 *
	 * @param value The new property value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse characterEncoding(String value) {
		setCharacterEncoding(value);
		return this;
	}

	@Override /* HttpServletResponse */
	public void setContentLength(int len) {
		this.contentLength = len;
	}

	/**
	 * Fluent setter for {@link #setContentLength(int)}.
	 *
	 * @param value The new property value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse contentLength(int value) {
		setContentLength(value);
		return this;
	}

	@Override /* HttpServletResponse */
	public void setContentLengthLong(long len) {
		this.contentLength = len;
	}

	@Override /* HttpServletResponse */
	public void setContentType(String type) {
		setHeader("Content-Type", type);
	}

	/**
	 * Fluent setter for {@link #setContentType(String)}.
	 *
	 * @param value The new property value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse contentType(String value) {
		setContentType(value);
		return this;
	}

	@Override /* HttpServletResponse */
	public void setBufferSize(int size) {
		this.bufferSize = size;
	}

	/**
	 * Fluent setter for {@link #bufferSize(int)}.
	 *
	 * @param value The new property value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse bufferSize(int value) {
		setBufferSize(value);
		return this;
	}

	@Override /* HttpServletResponse */
	public int getBufferSize() {
		return bufferSize;
	}

	@Override /* HttpServletResponse */
	public void flushBuffer() throws IOException {
	}

	@Override /* HttpServletResponse */
	public void resetBuffer() {
	}

	@Override /* HttpServletResponse */
	public boolean isCommitted() {
		return false;
	}

	@Override /* HttpServletResponse */
	public void reset() {
	}

	@Override /* HttpServletResponse */
	public void setLocale(Locale loc) {
		this.locale = loc;
	}

	/**
	 * Fluent setter for {@link #setLocale(Locale)}.
	 *
	 * @param value The new property value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse locale(Locale value) {
		setLocale(value);
		return this;
	}

	@Override /* HttpServletResponse */
	public Locale getLocale() {
		return locale;
	}

	@Override /* HttpServletResponse */
	public void addCookie(Cookie cookie) {
	}

	@Override /* HttpServletResponse */
	public boolean containsHeader(String name) {
		return getHeader(name) != null;
	}

	@Override /* HttpServletResponse */
	public String encodeURL(String url) {
		return null;
	}

	@Override /* HttpServletResponse */
	public String encodeRedirectURL(String url) {
		return null;
	}

	@Override /* HttpServletResponse */
	public String encodeUrl(String url) {
		return null;
	}

	@Override /* HttpServletResponse */
	public String encodeRedirectUrl(String url) {
		return null;
	}

	@Override /* HttpServletResponse */
	public void sendError(int sc, String msg) throws IOException {
		this.sc = sc;
		this.msg = msg;
	}

	@Override /* HttpServletResponse */
	public void sendError(int sc) throws IOException {
		this.sc = sc;
	}

	@Override /* HttpServletResponse */
	public void sendRedirect(String location) throws IOException {
		this.sc = 302;
		headerMap.put("Location", new String[] {location});
	}

	@Override /* HttpServletResponse */
	public void setDateHeader(String name, long date) {
		headerMap.put(name, new String[] {DateUtils.formatDate(new Date(date), DateUtils.PATTERN_RFC1123)});
	}

	@Override /* HttpServletResponse */
	public void addDateHeader(String name, long date) {
		headerMap.put(name, new String[] {DateUtils.formatDate(new Date(date), DateUtils.PATTERN_RFC1123)});
	}

	@Override /* HttpServletResponse */
	public void setHeader(String name, String value) {
		headerMap.put(name, new String[] {value});
	}

	@Override /* HttpServletResponse */
	public void addHeader(String name, String value) {
		headerMap.put(name, new String[] {value});
	}

	/**
	 * Fluent setter for {@link #setHeader(String,String)}.
	 *
	 * @param name The header name.
	 * @param value The new header value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse header(String name, String value) {
		setHeader(name, value);
		return this;
	}

	@Override /* HttpServletResponse */
	public void setIntHeader(String name, int value) {
		headerMap.put(name, new String[] {String.valueOf(value)});
	}

	@Override /* HttpServletResponse */
	public void addIntHeader(String name, int value) {
		headerMap.put(name, new String[] {String.valueOf(value)});
	}

	@Override /* HttpServletResponse */
	public void setStatus(int sc) {
		this.sc = sc;
	}

	/**
	 * Fluent setter for {@link #setStatus(int)}.
	 *
	 * @param value The new property value.
	 * @return This object (for method chaining).
	 */
	public MockServletResponse status(int value) {
		setStatus(value);
		return this;
	}

	@Override /* HttpServletResponse */
	public void setStatus(int sc, String sm) {
		this.sc = sc;
		this.msg = sm;
	}

	@Override /* HttpServletResponse */
	public int getStatus() {
		return sc;
	}

	@Override /* HttpServletResponse */
	public String getHeader(String name) {
		String[] s = headerMap.get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* HttpServletResponse */
	public Collection<String> getHeaders(String name) {
		String[] s = headerMap.get(name);
		return s == null ? Collections.emptyList() : Arrays.asList(s);
	}

	@Override /* HttpServletResponse */
	public Collection<String> getHeaderNames() {
		return headerMap.keySet();
	}

	/**
	 * Returns the body of the request as a string.
	 *
	 * @return The body of the request as a string.
	 */
	public String getBodyAsString() {
		try {
			return baos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Throws an {@link AssertionError} if the response status does not match the expected status.
	 *
	 * @param status The expected status.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if status does not match.
	 */
	public MockServletResponse assertStatus(int status) throws AssertionError {
		if (getStatus() != status)
			throw new MockAssertionError("Response did not have the expected status.\n\tExpected=[{0}]\n\tActual=[{1}]", status, getStatus());
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response body does not contain the expected text.
	 *
	 * @param text The expected text of the body.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the body does not contain the expected text.
	 */
	public MockServletResponse assertBody(String text) throws AssertionError {
		if (! StringUtils.isEquals(text, getBodyAsString()))
			throw new MockAssertionError("Response did not have the expected text.\n\tExpected=[{0}]\n\tActual=[{1}]", text, getBodyAsString());
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response body does not contain all of the expected substrings.
	 *
	 * @param substrings The expected substrings.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the body does not contain one or more of the expected substrings.
	 */
	public MockServletResponse assertBodyContains(String...substrings) throws AssertionError {
		String text = getBodyAsString();
		for (String substring : substrings)
			if (! contains(text, substring))
				throw new MockAssertionError("Response did not have the expected substring.\n\tExpected=[{0}]\n\tBody=[{1}]", substring, text);
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response body does not match the specified pattern.
	 *
	 * <p>
	 * A pattern is a simple string containing <js>"*"</js> to represent zero or more arbitrary characters.
	 *
	 * @param pattern The pattern to match against.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the body does not match the specified pattern.
	 */
	public MockServletResponse assertBodyMatches(String pattern) throws AssertionError {
		String text = getBodyAsString();
		if (! getMatchPattern(pattern).matcher(text).matches())
			throw new MockAssertionError("Response did not match expected pattern.\n\tPattern=[{0}]\n\tBody=[{1}]", pattern, text);
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response body does not match the specified regular expression.
	 *
	 * <p>
	 * A pattern is a simple string containing <js>"*"</js> to represent zero or more arbitrary characters.
	 *
	 * @param regExp The regular expression to match against.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the body does not match the specified regular expression.
	 */
	public MockServletResponse assertBodyMatchesRE(String regExp) throws AssertionError {
		String text = getBodyAsString();
		if (! Pattern.compile(regExp).matcher(text).matches())
			throw new MockAssertionError("Response did not match expected regular expression.\n\tRegExp=[{0}]\n\tBody=[{1}]", regExp, text);
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response does not contain the expected character encoding.
	 *
	 * @param value The expected character encoding.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the response does not contain the expected character encoding.
	 */
	public MockServletResponse assertCharset(String value) {
		if (! StringUtils.isEquals(value, getCharacterEncoding()))
			throw new MockAssertionError("Response did not have the expected character encoding.\n\tExpected=[{0}]\n\tActual=[{1}]", value, getBodyAsString());
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response does not contain the expected header value.
	 *
	 * @param name The header name.
	 * @param value The expected header value.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the response does not contain the expected header value.
	 */
	public MockServletResponse assertHeader(String name, String value) {
		if (! StringUtils.isEquals(value, getHeader(name)))
			throw new MockAssertionError("Response did not have the expected value for header {0}.\n\tExpected=[{1}]\n\tActual=[{2}]", name, value, getHeader(name));
		return this;
	}

	/**
	 * Throws an {@link AssertionError} if the response header does not contain all of the expected substrings.
	 *
	 * @param name The header name.
	 * @param substrings The expected substrings.
	 * @return This object (for method chaining).
	 * @throws AssertionError Thrown if the header does not contain one or more of the expected substrings.
	 */
	public MockServletResponse assertHeaderContains(String name, String...substrings) {
		String text = getHeader(name);
		for (String substring : substrings)
			if (! contains(text, substring))
				throw new MockAssertionError("Response did not have the expected substring in header {0}.\n\tExpected=[{1}]\n\tHeader=[{2}]", name, substring, text);
		return this;
	}

	/**
	 * Returns the body of the request.
	 *
	 * @return The body of the request.
	 */
	@Override /* MockHttpResponse */
	public byte[] getBody() {
		return baos.toByteArray();
	}

	@Override /* MockHttpResponse */
	public Map<String,String[]> getHeaders() {
		return headerMap;
	}

	private static class MockAssertionError extends AssertionError {
		private static final long serialVersionUID = 1L;

		MockAssertionError(String msg, Object...args) {
			super(MessageFormat.format(msg, args));
			System.err.println(getMessage());  // NOT DEBUG
		}
	}
}
