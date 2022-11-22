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
 * Wraps an existing {@link OutputStream} where the {@link #close()} method is a no-op.
 *
 * <p>
 * Useful in cases where you're working with streams that should not be implicitly closed.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public final class NoCloseOutputStream extends OutputStream {

	private final OutputStream os;

	/**
	 * Constructor.
	 *
	 * @param os The output stream to wrap.
	 */
	public NoCloseOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override /* OutputStream */
	public void write(int b) throws IOException {
		os.write(b);
	}

	@Override /* OutputStream */
	public void write(byte[] b) throws IOException {
		os.write(b);
	}

	@Override /* OutputStream */
	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	@Override /* OutputStream */
	public void flush() throws IOException {
		os.flush();
	}

	@Override /* OutputStream */
	public void close() throws IOException {
		os.flush();
	}
}