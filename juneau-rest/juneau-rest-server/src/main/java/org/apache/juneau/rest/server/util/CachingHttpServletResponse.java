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
 * Wraps an {@link HttpServletResponse} and tees a <b>bounded</b> copy of the output stream into memory for debug logging.
 *
 * <p>
 * At most a configured cap (default 8&nbsp;KB) of the response body is captured while all bytes are still written through
 * to the real client. The total number of bytes written is tracked so a truncation marker can be rendered.
 *
 * <p>
 * The underlying {@link HttpServletResponse#getOutputStream()} is not acquired until the first
 * {@link #getOutputStream()} or {@link #getWriter()} call &mdash; acquiring it eagerly in the constructor would
 * permanently lock the response into stream mode, making a subsequent direct {@code getWriter()} call on this
 * wrapper (or on the underlying response) throw {@link IllegalStateException} per the servlet spec, even for
 * calls that never write anything.
 *
 */
@SuppressWarnings({
	"resource" // The tee'd response stream is owned by the underlying response/servlet container, which closes it when the response completes; closing it here would break response body caching. Eclipse JDT @Owning warning is by design.
})
public class CachingHttpServletResponse extends HttpServletResponseWrapper {

	/** Default body capture cap, in bytes (8&nbsp;KB). */
	public static final int DEFAULT_CAP = 8 * 1024;

	/**
	 * Wraps the specified response inside a {@link CachingHttpServletResponse} if it isn't already, using the default cap.
	 *
	 * @param res The response to wrap. Must not be <jk>null</jk>.
	 * @return The wrapped response.
	 */
	public static CachingHttpServletResponse wrap(HttpServletResponse res) {
		return wrap(res, DEFAULT_CAP);
	}

	/**
	 * Wraps the specified response inside a {@link CachingHttpServletResponse} if it isn't already.
	 *
	 * @param res The response to wrap. Must not be <jk>null</jk>.
	 * @param cap The maximum number of body bytes to capture.
	 * @return The wrapped response.
	 */
	public static CachingHttpServletResponse wrap(HttpServletResponse res, int cap) {
		if (res instanceof CachingHttpServletResponse res2)
			return res2;
		return new CachingHttpServletResponse(res, cap);
	}

	private final int cap;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private long totalLength = 0;
	private TeeServletOutputStream stream;
	private PrintWriter writer;

	/**
	 * Constructor.
	 *
	 * @param res The wrapped servlet response. Must not be <jk>null</jk>.
	 * @param cap The maximum number of body bytes to capture.
	 */
	protected CachingHttpServletResponse(HttpServletResponse res, int cap) {
		super(res);
		this.cap = cap;
	}

	/**
	 * Returns the captured (possibly truncated) response body bytes.
	 *
	 * @return The captured body bytes (at most the configured cap). Never <jk>null</jk>.
	 */
	public byte[] getContent() { return buffer.toByteArray(); }

	/**
	 * Returns the total number of body bytes written so far, including any bytes beyond the capture cap.
	 *
	 * @return The total body length in bytes.
	 */
	public long getTotalLength() { return totalLength; }

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

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (stream == null)
			stream = new TeeServletOutputStream(getResponse().getOutputStream());
		return stream;
	}

	/**
	 * Returns a character-stream tee that writes through {@link #getOutputStream()}.
	 *
	 * <p>
	 * Juneau's own {@link org.apache.juneau.rest.server.RestResponse#getWriter()} always negotiates its writer over
	 * {@link #getOutputStream()}, so this override chiefly protects direct/raw consumers (filters, non-Juneau
	 * servlets in the same chain) that call {@code HttpServletResponse.getWriter()} on the wrapped response after
	 * capture has been installed &mdash; without it, that call would either bypass the tee entirely or throw
	 * {@link IllegalStateException} because {@link #getOutputStream()} may have already been called.
	 */
	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer == null) {
			var enc = getCharacterEncoding();
			var cs = enc == null ? StandardCharsets.ISO_8859_1 : Charset.forName(enc);
			writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), cs));
		}
		return writer;
	}

	private final class TeeServletOutputStream extends ServletOutputStream {

		private final ServletOutputStream delegate;

		TeeServletOutputStream(ServletOutputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		@Override
		public void flush() throws IOException {
			delegate.flush();
		}

		@Override
		public boolean isReady() { return delegate.isReady(); }

		@Override
		public void setWriteListener(WriteListener writeListener) {
			delegate.setWriteListener(writeListener);
		}

		@Override
		public void write(int b) throws IOException {
			capture(b);
			delegate.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			capture(b, off, len);
			delegate.write(b, off, len);
		}
	}
}
