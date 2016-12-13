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
 * Writer that can send output to multiple writers.
 */
public class TeeWriter extends Writer {
	private Writer[] writers = new Writer[0];
	private Map<String,Writer> writerMap;

	/**
	 * Constructor.
	 *
	 * @param writers The list of writers.
	 */
	public TeeWriter(Writer...writers) {
		this.writers = writers;
	}

	/**
	 * Constructor.
	 *
	 * @param writers The list of writers.
	 */
	public TeeWriter(Collection<Writer> writers) {
		this.writers = writers.toArray(new Writer[writers.size()]);
	}

	/**
	 * Adds a writer to this tee writer.
	 *
	 * @param w The writer to add to this tee writer.
	 * @param close If <jk>false</jk>, then calling {@link #close()} on this tee writer
	 * 	will not filter to the specified writer.
	 * @return This object (for method chaining).
	 */
	public TeeWriter add(Writer w, boolean close) {
		if (w == null)
			return this;
		if (! close)
			w = new NoCloseWriter(w);
		if (w == this)
			throw new RuntimeException("Cannot add this writer to itself.");
		for (Writer w2 : writers)
			if (w2 == w)
				throw new RuntimeException("Cannot add this writer again.");
		if (w instanceof TeeWriter) {
			for (Writer w2 : ((TeeWriter)w).writers)
				add(w2, true);
		} else {
			writers = ArrayUtils.append(writers, w);
		}
		return this;
	}

	/**
	 * Same as {@link #add(Writer, boolean)} but associates the writer with an identifier
	 * so the writer can be retrieved through {@link #getWriter(String)}.
	 *
	 * @param id The ID to associate the writer with.
	 * @param w The writer to add.
	 * @param close Close the specified writer afterwards.
	 * @return This object (for method chaining).
	 */
	public TeeWriter add(String id, Writer w, boolean close) {
		if (id != null) {
			if (writerMap == null)
				writerMap = new TreeMap<String,Writer>();
			writerMap.put(id, w);
		}
		return add(w, close);
	}

	/**
	 * Returns the number of inner writers in this tee writer.
	 *
	 * @return The number of writers.
	 */
	public int size() {
		return writers.length;
	}

	/**
	 * Returns the writer identified through the <code>id</code> parameter
	 * passed in through the {@link #add(String, Writer, boolean)} method.
	 *
	 * @param id The ID associated with the writer.
	 * @return The writer, or <jk>null</jk> if no identifier was specified when the writer was added.
	 */
	public Writer getWriter(String id) {
		if (writerMap != null)
			return writerMap.get(id);
		return null;
	}

	@Override /* Writer */
	public void write(char[] cbuf, int off, int len) throws IOException {
		for (Writer w : writers)
			if (w != null)
			w.write(cbuf, off, len);
	}

	@Override /* Writer */
	public void flush() throws IOException {
		for (Writer w : writers)
			if (w != null)
			w.flush();
	}

	@Override /* Writer */
	public void close() throws IOException {
		IOException e = null;
		for (Writer w : writers) {
			if (w != null) {
				try {
			w.close();
				} catch (IOException e2) {
					e = e2;
				}
			}
		}
		if (e != null)
			throw e;
	}

	private static class NoCloseWriter extends Writer {
		private Writer writer;

		private NoCloseWriter(Writer writer) {
			this.writer = writer;
		}

		@Override /* Writer */
		public void write(char[] cbuf, int off, int len) throws IOException {
			writer.write(cbuf, off, len);
		}

		@Override /* Writer */
		public void flush() throws IOException {
			writer.flush();
		}

		@Override /* Writer */
		public void close() throws IOException {
			// Do nothing.
		}
	}
}
