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
package org.apache.juneau.internal;

import java.io.*;

/**
 * Similar to {@link StringWriter}, but uses a {@link StringBuilder} instead to avoid synchronization overhead.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class StringBuilderWriter extends Writer {

	private StringBuilder sb;

	/**
	 * Create a new string writer using the default initial string-builder size.
	 */
	public StringBuilderWriter() {
		sb = new StringBuilder();
		lock = null;
	}

	/**
	 * Create a new string writer around an existing string builder.
	 *
	 * @param sb The string builder being wrapped.
	 */
	public StringBuilderWriter(StringBuilder sb) {
		this.sb = sb;
		lock = null;
	}

	/**
	 * Create a new string writer using the specified initial string-builder size.
	 *
	 * @param initialSize
	 * 	The number of <tt>char</tt> values that will fit into this buffer before it is automatically expanded.
	 * @throws IllegalArgumentException If <tt>initialSize</tt> is negative.
	 */
	public StringBuilderWriter(int initialSize) {
		sb = new StringBuilder(initialSize);
		lock = null;
	}

	@Override /* Writer */
	public void write(int c) {
		sb.append((char) c);
	}

	@Override /* Writer */
	public void write(char cbuf[], int start, int length) {
		sb.append(cbuf, start, length);
	}

	@Override /* Writer */
	public void write(String str) {
		sb.append(str);
	}

	@Override /* Writer */
	public void write(String str, int off, int len) {
		sb.append(str.substring(off, off + len));
	}

	@Override /* Writer */
	public StringBuilderWriter append(CharSequence csq) {
		if (csq == null)
			write("null");
		else
			write(csq.toString());
		return this;
	}

	@Override /* Writer */
	public StringBuilderWriter append(CharSequence csq, int start, int end) {
		CharSequence cs = (csq == null ? "null" : csq);
		write(cs.subSequence(start, end).toString());
		return this;
	}

	@Override /* Writer */
	public StringBuilderWriter append(char c) {
		write(c);
		return this;
	}

	@Override /* Object */
	public String toString() {
		return sb.toString();
	}

	@Override /* Writer */
	public void flush() {}

	@Override /* Writer */
	public void close() throws IOException {}
}
