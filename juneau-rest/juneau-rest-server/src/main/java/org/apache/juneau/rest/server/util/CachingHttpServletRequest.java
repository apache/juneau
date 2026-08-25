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
package org.apache.juneau.rest.server.util;

import java.io.*;
import java.nio.charset.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Wraps an {@link HttpServletRequest} and tees a <b>bounded</b> copy of the request body into memory for debug logging.
 *
 * <p>
 * Unlike a fully-buffering wrapper, this captures at most a configured cap (default 8&nbsp;KB) of the body while passing
 * the full stream through to the handler untouched. The total number of bytes read is tracked so a truncation marker can
 * be rendered. Memory stays bounded even for large uploads.
 *
 */
@SuppressWarnings({
	"resource" // The tee'd request stream is owned by the underlying request/servlet container, which closes it when the request completes; closing it here would break body-caching. Eclipse JDT @Owning warning is by design.
})
public class CachingHttpServletRequest extends HttpServletRequestWrapper {

	/** Default body capture cap, in bytes (8&nbsp;KB). */
	public static final int DEFAULT_CAP = 8 * 1024;

	/**
	 * Wraps the specified request inside a {@link CachingHttpServletRequest} if it isn't already, using the default cap.
	 *
	 * @param req The request to wrap. Must not be <jk>null</jk>.
	 * @return The wrapped request.
	 */
	public static CachingHttpServletRequest wrap(HttpServletRequest req) {
		return wrap(req, DEFAULT_CAP);
	}

	/**
	 * Wraps the specified request inside a {@link CachingHttpServletRequest} if it isn't already.
	 *
	 * @param req The request to wrap. Must not be <jk>null</jk>.
	 * @param cap The maximum number of body bytes to capture.
	 * @return The wrapped request.
	 */
	public static CachingHttpServletRequest wrap(HttpServletRequest req, int cap) {
		if (req instanceof CachingHttpServletRequest req2)
			return req2;
		return new CachingHttpServletRequest(req, cap);
	}

	private final int cap;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private long totalLength = 0;
	private TeeServletInputStream stream;
	private BufferedReader reader;

	/**
	 * Constructor.
	 *
	 * @param req The request being wrapped. Must not be <jk>null</jk>.
	 * @param cap The maximum number of body bytes to capture.
	 */
	protected CachingHttpServletRequest(HttpServletRequest req, int cap) {
		super(req);
		this.cap = cap;
	}

	/**
	 * Returns the captured (possibly truncated) request body bytes.
	 *
	 * @return The captured body bytes (at most the configured cap). Never <jk>null</jk>; empty if the body was not read.
	 */
	public byte[] getContent() { return buffer.toByteArray(); }

	/**
	 * Returns the total number of body bytes read so far, including any bytes beyond the capture cap.
	 *
	 * @return The total body length in bytes.
	 */
	public long getTotalLength() { return totalLength; }

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (stream == null)
			stream = new TeeServletInputStream(getRequest().getInputStream());
		return stream;
	}

	/**
	 * Returns a character-stream tee over the same underlying byte stream captured by {@link #getInputStream()}.
	 *
	 * <p>
	 * A handler that reads the request body through {@code getReader()} instead of {@code getInputStream()} would
	 * otherwise bypass capture entirely &mdash; the default {@link HttpServletRequestWrapper#getReader()} delegates
	 * straight to the wrapped request. This override routes the reader through the tee'd stream so both access
	 * styles are captured identically.
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		if (reader == null) {
			var enc = getCharacterEncoding();
			var cs = enc == null ? StandardCharsets.ISO_8859_1 : Charset.forName(enc);
			reader = new BufferedReader(new InputStreamReader(getInputStream(), cs));
		}
		return reader;
	}

	private final class TeeServletInputStream extends ServletInputStream {

		private final ServletInputStream delegate;

		TeeServletInputStream(ServletInputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public int read() throws IOException {
			var b = delegate.read();
			if (b != -1)
				capture(b);
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			var n = delegate.read(b, off, len);
			if (n > 0)
				capture(b, off, n);
			return n;
		}

		@Override
		public boolean isFinished() { return delegate.isFinished(); }

		@Override
		public boolean isReady() { return delegate.isReady(); }

		@Override
		public void setReadListener(ReadListener readListener) { delegate.setReadListener(readListener); }

		// Only this tee touches the outer request's capture buffer, so these helpers live here rather than on the
		// outer class.
		private void capture(int b) {
			totalLength++;
			if (buffer.size() < cap)
				buffer.write(b);
		}

		private void capture(byte[] b, int off, int len) {
			totalLength += len;
			var room = cap - buffer.size();
			if (room > 0)
				buffer.write(b, off, Math.min(room, len));
		}
	}
}
