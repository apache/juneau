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

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * Wraps an {@link HttpServletResponse} and tees a <b>bounded</b> copy of the output stream into memory for debug logging.
 *
 * <p>
 * At most a configured cap (default 8&nbsp;KB) of the response body is captured while all bytes are still written through
 * to the real client. The total number of bytes written is tracked so a truncation marker can be rendered.
 *
 */
@SuppressWarnings({
	"resource" // os is a servlet-container-managed stream obtained from the wrapped response; closed by the container
})
public class CachingHttpServletResponse extends HttpServletResponseWrapper {

	/** Default body capture cap, in bytes (8&nbsp;KB). */
	public static final int DEFAULT_CAP = 8 * 1024;

	/**
	 * Wraps the specified response inside a {@link CachingHttpServletResponse} if it isn't already, using the default cap.
	 *
	 * @param res The response to wrap. Must not be <jk>null</jk>.
	 * @return The wrapped response.
	 * @throws IOException Thrown by underlying content stream.
	 */
	public static CachingHttpServletResponse wrap(HttpServletResponse res) throws IOException {
		return wrap(res, DEFAULT_CAP);
	}

	/**
	 * Wraps the specified response inside a {@link CachingHttpServletResponse} if it isn't already.
	 *
	 * @param res The response to wrap. Must not be <jk>null</jk>.
	 * @param cap The maximum number of body bytes to capture.
	 * @return The wrapped response.
	 * @throws IOException Thrown by underlying content stream.
	 */
	public static CachingHttpServletResponse wrap(HttpServletResponse res, int cap) throws IOException {
		if (res instanceof CachingHttpServletResponse res2)
			return res2;
		return new CachingHttpServletResponse(res, cap);
	}

	private final int cap;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private long totalLength = 0;
	private final ServletOutputStream os;

	/**
	 * Constructor.
	 *
	 * @param res The wrapped servlet response. Must not be <jk>null</jk>.
	 * @param cap The maximum number of body bytes to capture.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected CachingHttpServletResponse(HttpServletResponse res, int cap) throws IOException {
		super(res);
		this.cap = cap;
		os = res.getOutputStream();
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

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream() {

			@Override
			public void close() throws IOException {
				os.close();
			}

			@Override
			public void flush() throws IOException {
				os.flush();
			}

			@Override
			public boolean isReady() { return os.isReady(); }

			@Override
			public void setWriteListener(WriteListener writeListener) {
				os.setWriteListener(writeListener);
			}

			@Override
			public void write(int b) throws IOException {
				capture(b);
				os.write(b);
			}
		};
	}
}
