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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;

import jakarta.servlet.*;

/**
 * ServletInputStream wrapper around a normal input stream with support for limiting input.
 *
 */
public class BoundedServletInputStream extends ServletInputStream {

	private final InputStream is;
	private final ServletInputStream sis;
	private long remain;

	/**
	 * Wraps the specified byte array.
	 *
	 * @param b The byte contents of the stream.
	 */
	public BoundedServletInputStream(byte[] b) {
		this(new ByteArrayInputStream(b), Long.MAX_VALUE);
	}

	/**
	 * Wraps the specified input stream.
	 *
	 * @param is The input stream to wrap.
	 * @param max The maximum number of bytes to read from the stream.
	 */
	public BoundedServletInputStream(InputStream is, long max) {
		this.is = is;
		this.sis = null;
		this.remain = max;
	}

	/**
	 * Wraps the specified input stream.
	 *
	 * @param sis The input stream to wrap.
	 * @param max The maximum number of bytes to read from the stream.
	 */
	public BoundedServletInputStream(ServletInputStream sis, long max) {
		this.sis = sis;
		this.is = sis;
		this.remain = max;
	}

	@Override /* Overridden from InputStream */
	public int available() throws IOException {
		if (remain <= 0)
			return 0;
		return is.available();
	}

	@Override /* Overridden from InputStream */
	public void close() throws IOException {
		is.close();
	}

	@Override /* Overridden from ServletInputStream */
	public boolean isFinished() { return sis == null ? false : sis.isFinished(); }

	@Override /* Overridden from ServletInputStream */
	public boolean isReady() { return sis == null ? true : sis.isReady(); }

	@Override /* Overridden from InputStream */
	public synchronized void mark(int limit) {
		is.mark(limit);
	}

	@Override /* Overridden from InputStream */
	public boolean markSupported() {
		return is.markSupported();
	}

	@Override /* Overridden from InputStream */
	public int read() throws IOException {
		decrement();
		return is.read();
	}

	@Override /* Overridden from InputStream */
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override /* Overridden from InputStream */
	public int read(byte[] b, int off, int len) throws IOException {
		long numBytes = Math.min(len, remain);
		int r = is.read(b, off, (int)numBytes);
		if (r == -1)
			return -1;
		decrement(numBytes);
		return r;
	}

	@Override /* Overridden from InputStream */
	public synchronized void reset() throws IOException {
		is.reset();
	}

	@Override /* Overridden from ServletInputStream */
	public void setReadListener(ReadListener arg0) {
		if (nn(sis))
			sis.setReadListener(arg0);
	}

	@Override /* Overridden from InputStream */
	public long skip(long n) throws IOException {
		long toSkip = Math.min(n, remain);
		long r = is.skip(toSkip);
		decrement(r);
		return r;
	}

	private void decrement() throws IOException {
		remain--;
		if (remain < 0)
			throw ioex("Input limit exceeded.  See @Rest(maxInput).");
	}

	private void decrement(long count) throws IOException {
		remain -= count;
		if (remain < 0)
			throw ioex("Input limit exceeded.  See @Rest(maxInput).");
	}
}