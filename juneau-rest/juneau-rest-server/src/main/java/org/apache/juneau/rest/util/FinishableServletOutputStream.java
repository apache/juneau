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

import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;

import org.apache.juneau.encoders.*;

import jakarta.servlet.*;

/**
 * A wrapped {@link ServletOutputStream} with an added <c>finish()</c> method.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class FinishableServletOutputStream extends ServletOutputStream implements Finishable {

	final OutputStream os;
	final ServletOutputStream sos;
	final Finishable f;

	/**
	 * Constructor.
	 *
	 * @param os The wrapped output stream.
	 */
	public FinishableServletOutputStream(OutputStream os) {
		this.os = os;
		this.sos = (os instanceof ServletOutputStream ? (ServletOutputStream)os : null);
		this.f = (os instanceof Finishable ? (Finishable)os : null);
	}

	@Override /* Overridden from OutputStream */
	public final void close() throws IOException {
		os.close();
	}

	/**
	 * Calls {@link Finishable#finish()} on the underlying output stream.
	 *
	 * <p>
	 * A no-op if the underlying output stream does not implement the {@link Finishable} interface.
	 */
	@Override /* Overridden from Finishable */
	public void finish() throws IOException {
		if (nn(f))
			f.finish();
	}

	@Override /* Overridden from OutputStream */
	public final void flush() throws IOException {
		os.flush();
	}

	@Override /* Overridden from ServletOutputStream */
	public boolean isReady() { return sos == null ? true : sos.isReady(); }

	@Override /* Overridden from ServletOutputStream */
	public void setWriteListener(WriteListener arg0) {
		if (nn(sos))
			sos.setWriteListener(arg0);
	}

	@Override /* Overridden from OutputStream */
	public final void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	@Override /* Overridden from OutputStream */
	public final void write(int b) throws IOException {
		os.write(b);
	}
}