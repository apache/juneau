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
import java.util.*;

/**
 * Output stream that can send output to multiple output streams.
 */
public class TeeOutputStream extends OutputStream {
	private OutputStream[] outputStreams = new OutputStream[0];
	private Map<String,OutputStream> outputStreamMap;

	/**
	 * Constructor.
	 *
	 * @param outputStreams The list of output streams.
	 */
	public TeeOutputStream(OutputStream...outputStreams) {
		this.outputStreams = outputStreams;
	}

	/**
	 * Constructor.
	 *
	 * @param outputStreams The list of output streams.
	 */
	public TeeOutputStream(Collection<OutputStream> outputStreams) {
		this.outputStreams = outputStreams.toArray(new OutputStream[outputStreams.size()]);
	}

	/**
	 * Adds an output stream to this tee output stream.
	 *
	 * @param os The output stream to add to this tee output stream.
	 * @param close If <jk>false</jk>, then calling {@link #close()} on this stream will not filter to the specified
	 * output stream.
	 * @return This object (for method chaining).
	 */
	public TeeOutputStream add(OutputStream os, boolean close) {
		if (os == null)
			return this;
		if (! close)
			os = new NoCloseOutputStream(os);
		if (os == this)
			throw new RuntimeException("Cannot add this output stream to itself.");
		for (OutputStream os2 : outputStreams)
			if (os2 == os)
				throw new RuntimeException("Cannot add this output stream again.");
		if (os instanceof TeeOutputStream) {
			for (OutputStream os2 : ((TeeOutputStream)os).outputStreams)
				add(os2, true);
		} else {
			outputStreams = ArrayUtils.append(outputStreams, os);
		}
		return this;
	}

	/**
	 * Returns the output stream identified through the <code>id</code> parameter passed in through the
	 * {@link #add(String, OutputStream, boolean)} method.
	 *
	 * @param id The ID associated with the output stream.
	 * @return The output stream, or <jk>null</jk> if no identifier was specified when the output stream was added.
	 */
	public OutputStream getOutputStream(String id) {
		if (outputStreamMap != null)
			return outputStreamMap.get(id);
		return null;
	}

	/**
	 * Same as {@link #add(OutputStream, boolean)} but associates the stream with an identifier so the stream can be
	 * retrieved through {@link #getOutputStream(String)}.
	 *
	 * @param id The ID to associate the output stream with.
	 * @param os The output stream to add.
	 * @param close Close the specified stream afterwards.
	 * @return This object (for method chaining).
	 */
	public TeeOutputStream add(String id, OutputStream os, boolean close) {
		if (id != null) {
			if (outputStreamMap == null)
				outputStreamMap = new TreeMap<String,OutputStream>();
			outputStreamMap.put(id, os);
		}
		return add(os, close);
	}

	/**
	 * Returns the number of inner streams in this tee stream.
	 *
	 * @return The number of streams in this tee stream.
	 */
	public int size() {
		return outputStreams.length;
	}

	@Override /* OutputStream */
	public void write(int b) throws IOException {
		for (OutputStream os : outputStreams)
			os.write(b);
	}

	@Override /* OutputStream */
	public void write(byte b[], int off, int len) throws IOException {
		for (OutputStream os : outputStreams)
			os.write(b, off, len);
	}

	@Override /* OutputStream */
	public void flush() throws IOException {
		for (OutputStream os : outputStreams)
			os.flush();
	}

	@Override /* OutputStream */
	public void close() throws IOException {
		for (OutputStream os : outputStreams)
			os.close();
	}

	private static class NoCloseOutputStream extends OutputStream {
		private OutputStream os;

		private NoCloseOutputStream(OutputStream os) {
			this.os = os;
		}

		@Override /* OutputStream */
		public void write(int b) throws IOException {
			os.write(b);
		}

		@Override /* OutputStream */
		public void write(byte b[], int off, int len) throws IOException {
			os.write(b, off, len);
		}

		@Override /* OutputStream */
		public void flush() throws IOException {
			os.flush();
		}

		@Override /* OutputStream */
		public void close() throws IOException {
			// Do nothing.
		}
	}
}
