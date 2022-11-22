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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;

/**
 * An implementation of {@link HttpServletResponse} for mocking purposes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
*/
public class MockServletResponse implements HttpServletResponse {

	private String characterEncoding = "UTF-8";
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private int bufferSize = 0;
	private Locale locale;
	private int sc;
	private String msg;
	private Map<String,String[]> headerMap = map();

	/**
	 * Creates a new servlet response.
	 *
	 * @return A new response.
	 */
	public static MockServletResponse create() {
		return new MockServletResponse();
	}

	/**
	 * Returns the response message.
	 *
	 * @return The response message.
	 */
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
		updateContentTypeHeader();
	}

	private void updateContentTypeHeader() {
		String contentType = getContentType();
		String charset = characterEncoding;
		if (contentType != null && charset != null) {
			if (contentType.indexOf("charset=") != -1)
				contentType = contentType.replaceAll("\\;\\s*charset=.*", "");
			if (! "UTF-8".equalsIgnoreCase(charset))
				contentType = contentType + ";charset=" + charset;
			header("Content-Type", contentType);
		}
	}

	@Override /* HttpServletResponse */
	public void setContentLength(int len) {
		header("Content-Length", String.valueOf(len));
	}

	@Override /* HttpServletResponse */
	public void setContentLengthLong(long len) {
		header("Content-Length", String.valueOf(len));
	}

	@Override /* HttpServletResponse */
	public void setContentType(String type) {
		setHeader("Content-Type", type);
		updateContentTypeHeader();
	}

	@Override /* HttpServletResponse */
	public void setBufferSize(int size) {
		this.bufferSize = size;
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
	 * @return This object.
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
	 * @return This object.
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
		return s == null ? emptyList() : ulist(s);
	}

	@Override /* HttpServletResponse */
	public Collection<String> getHeaderNames() {
		return headerMap.keySet();
	}

	byte[] getContent() throws IOException {
		baos.flush();
		return baos.toByteArray();
	}

	Map<String,String[]> getHeaders() {
		return headerMap;
	}
}
