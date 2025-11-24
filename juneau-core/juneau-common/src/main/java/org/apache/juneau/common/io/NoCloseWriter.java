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
package org.apache.juneau.common.io;

import java.io.*;

/**
 * Wraps an existing {@link Writer} where the {@link #close()} method is a no-op.
 *
 * <p>
 * Useful in cases where you're working with streams that should not be implicitly closed.
 *
 */
public class NoCloseWriter extends Writer {

	private final Writer w;

	/**
	 * Constructor.
	 *
	 * @param w The writer to wrap.
	 */
	public NoCloseWriter(Writer w) {
		this.w = w;
	}

	@Override /* Overridden from Writer */
	public Writer append(char c) throws IOException {
		return w.append(c);
	}

	@Override /* Overridden from Writer */
	public Writer append(CharSequence csq) throws IOException {
		return w.append(csq);
	}

	@Override /* Overridden from Writer */
	public Writer append(CharSequence csq, int start, int end) throws IOException {
		return w.append(csq, start, end);
	}

	@Override /* Overridden from Writer */
	public void close() throws IOException {
		w.flush();
	}

	@Override /* Overridden from Writer */
	public void flush() throws IOException {
		w.flush();
	}

	@Override /* Overridden from Object */
	public String toString() {
		return w.toString();
	}

	@Override /* Overridden from Writer */
	public void write(char[] cbuf) throws IOException {
		w.write(cbuf);
	}

	@Override /* Overridden from Writer */
	public void write(char[] cbuf, int off, int len) throws IOException {
		w.write(cbuf, off, len);
	}

	@Override /* Overridden from Writer */
	public void write(int c) throws IOException {
		w.write(c);
	}

	@Override /* Overridden from Writer */
	public void write(String str) throws IOException {
		w.write(str);
	}

	@Override /* Overridden from Writer */
	public void write(String str, int off, int len) throws IOException {
		w.write(str, off, len);
	}
}