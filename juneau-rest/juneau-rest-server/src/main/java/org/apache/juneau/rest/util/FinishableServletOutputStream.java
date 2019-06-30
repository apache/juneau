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
package org.apache.juneau.rest.util;

import java.io.*;

import javax.servlet.*;

import org.apache.juneau.encoders.*;

/**
 * A wrapped {@link ServletOutputStream} with an added <c>finish()</c> method.
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

	@Override /* OutputStream */
	public final void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	@Override /* OutputStream */
	public final void write(int b) throws IOException {
		os.write(b);
	}

	@Override /* OutputStream */
	public final void flush() throws IOException {
		os.flush();
	}

	@Override /* OutputStream */
	public final void close() throws IOException {
		os.close();
	}

	@Override /* ServletOutputStream */
	public boolean isReady() {
		return sos == null ? true : sos.isReady();
	}

	@Override /* ServletOutputStream */
	public void setWriteListener(WriteListener arg0) {
		if (sos != null)
			sos.setWriteListener(arg0);
	}

	/**
	 * Calls {@link Finishable#finish()} on the underlying output stream.
	 *
	 * <p>
	 * A no-op if the underlying output stream does not implement the {@link Finishable} interface.
	 */
	@Override /* Finishable */
	public void finish() throws IOException {
		if (f != null)
			f.finish();
	}
}