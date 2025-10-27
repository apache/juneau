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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.common.utils.*;
import org.apache.juneau.rest.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * An implementation of {@link HttpServletResponse} for mocking purposes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestMockBasics">juneau-rest-mock Basics</a>
 * </ul>
*/
public class MockServletResponse implements HttpServletResponse {

	/**
	 * Creates a new servlet response.
	 *
	 * @return A new response.
	 */
	public static MockServletResponse create() {
		return new MockServletResponse();
	}

	private String characterEncoding = "UTF-8";
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private int bufferSize;
	private Locale locale;
	private int sc;
	private String msg;

	private Map<String,String[]> headerMap = CollectionUtils.map();

	@Override /* Overridden from HttpServletResponse */
	public void addCookie(Cookie cookie) {}

	@Override /* Overridden from HttpServletResponse */
	public void addDateHeader(String name, long date) {
		headerMap.put(name, new String[] { DateUtils.formatDate(new Date(date), DateUtils.PATTERN_RFC1123) });
	}

	@Override /* Overridden from HttpServletResponse */
	public void addHeader(String name, String value) {
		headerMap.put(name, new String[] { value });
	}

	@Override /* Overridden from HttpServletResponse */
	public void addIntHeader(String name, int value) {
		headerMap.put(name, new String[] { String.valueOf(value) });
	}

	@Override /* Overridden from HttpServletResponse */
	public boolean containsHeader(String name) {
		return nn(getHeader(name));
	}

	@Override /* Overridden from HttpServletResponse */
	public String encodeRedirectURL(String url) {
		return null;
	}

	@Override /* Overridden from HttpServletResponse */
	public String encodeURL(String url) {
		return null;
	}

	@Override /* Overridden from HttpServletResponse */
	public void flushBuffer() throws IOException {}

	@Override /* Overridden from HttpServletResponse */
	public int getBufferSize() { return bufferSize; }

	@Override /* Overridden from HttpServletResponse */
	public String getCharacterEncoding() { return characterEncoding; }

	@Override /* Overridden from HttpServletResponse */
	public String getContentType() { return getHeader("Content-Type"); }

	@Override /* Overridden from HttpServletResponse */
	public String getHeader(String name) {
		String[] s = headerMap.get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* Overridden from HttpServletResponse */
	public Collection<String> getHeaderNames() { return headerMap.keySet(); }

	@Override /* Overridden from HttpServletResponse */
	public Collection<String> getHeaders(String name) {
		String[] s = headerMap.get(name);
		return s == null ? Collections.emptyList() : u(alist(s));
	}

	@Override /* Overridden from HttpServletResponse */
	public Locale getLocale() { return locale; }

	/**
	 * Returns the response message.
	 *
	 * @return The response message.
	 */
	public String getMessage() { return msg; }

	@Override /* Overridden from HttpServletResponse */
	public ServletOutputStream getOutputStream() throws IOException { return new FinishableServletOutputStream(baos); }

	@Override /* Overridden from HttpServletResponse */
	public int getStatus() { return sc; }

	@Override /* Overridden from HttpServletResponse */
	public PrintWriter getWriter() throws IOException { return new PrintWriter(new OutputStreamWriter(getOutputStream(), characterEncoding)); }

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

	@Override /* Overridden from HttpServletResponse */
	public boolean isCommitted() { return false; }

	@Override /* Overridden from HttpServletResponse */
	public void reset() {}

	@Override /* Overridden from HttpServletResponse */
	public void resetBuffer() {}

	@Override /* Overridden from HttpServletResponse */
	public void sendError(int sc) throws IOException {
		this.sc = sc;
	}

	@Override /* Overridden from HttpServletResponse */
	public void sendError(int sc, String msg) throws IOException {
		this.sc = sc;
		this.msg = msg;
	}

	@Override /* Overridden from HttpServletResponse */
	public void sendRedirect(String location) throws IOException {
		this.sc = 302;
		headerMap.put("Location", new String[] { location });
	}

	@Override /* Overridden from HttpServletResponse */
	public void sendRedirect(String location, int sc, boolean clearBuffer) throws IOException {
		this.sc = sc;
		headerMap.put("Location", new String[] { location });
	}

	@Override /* Overridden from HttpServletResponse */
	public void setBufferSize(int size) { this.bufferSize = size; }

	@Override /* Overridden from HttpServletResponse */
	public void setCharacterEncoding(String charset) {
		this.characterEncoding = charset;
		updateContentTypeHeader();
	}

	@Override /* Overridden from HttpServletResponse */
	public void setContentLength(int len) {
		header("Content-Length", String.valueOf(len));
	}

	@Override /* Overridden from HttpServletResponse */
	public void setContentLengthLong(long len) {
		header("Content-Length", String.valueOf(len));
	}

	@Override /* Overridden from HttpServletResponse */
	public void setContentType(String type) {
		setHeader("Content-Type", type);
		updateContentTypeHeader();
	}

	@Override /* Overridden from HttpServletResponse */
	public void setDateHeader(String name, long date) {
		headerMap.put(name, new String[] { DateUtils.formatDate(new Date(date), DateUtils.PATTERN_RFC1123) });
	}

	@Override /* Overridden from HttpServletResponse */
	public void setHeader(String name, String value) {
		headerMap.put(name, new String[] { value });
	}

	@Override /* Overridden from HttpServletResponse */
	public void setIntHeader(String name, int value) {
		headerMap.put(name, new String[] { String.valueOf(value) });
	}

	@Override /* Overridden from HttpServletResponse */
	public void setLocale(Locale loc) { this.locale = loc; }

	@Override /* Overridden from HttpServletResponse */
	public void setStatus(int sc) { this.sc = sc; }

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

	private void updateContentTypeHeader() {
		String contentType = getContentType();
		String charset = characterEncoding;
		if (nn(contentType) && nn(charset)) {
			if (contentType.indexOf("charset=") != -1)
				contentType = contentType.replaceAll("\\;\\s*charset=.*", "");
			if (! "UTF-8".equalsIgnoreCase(charset))
				contentType = contentType + ";charset=" + charset;
			header("Content-Type", contentType);
		}
	}

	byte[] getContent() throws IOException {
		baos.flush();
		return baos.toByteArray();
	}

	Map<String,String[]> getHeaders() { return headerMap; }
}