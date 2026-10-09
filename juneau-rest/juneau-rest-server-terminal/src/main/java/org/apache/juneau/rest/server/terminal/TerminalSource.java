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
package org.apache.juneau.rest.server.terminal;

import java.io.*;

/**
 * A growing stream of raw terminal bytes, read by absolute byte offset.
 *
 * <p>
 * The bytes are exactly what the producer's terminal wrote, escape sequences included.  The server never decodes
 * them.
 */
public interface TerminalSource {

	/** The largest number of bytes one {@link #read(long, int)} returns. */
	int MAX_READ_BYTES = 256 * 1024;

	/**
	 * Reads up to {@code maxBytes} bytes starting at {@code fromOffset}.
	 *
	 * <p>
	 * When {@code fromOffset} equals the end, the chunk is empty.  When it is past the end, the chunk is empty, its
	 * {@code next} is its {@code end}, and that is below {@code fromOffset}, which the endpoint answers with {@code 416}.  When it is no longer
	 * held, the chunk is {@code truncated} and starts at the oldest offset held.  When the backing store has
	 * disappeared, the chunk is empty and {@code gone}, with {@code next} and {@code end} equal to {@code fromOffset}.
	 *
	 * @param fromOffset The offset to read from, at least 0.
	 * @param maxBytes The most bytes to return, clamped to 0-{@link #MAX_READ_BYTES}.
	 * @return The chunk.
	 * @throws IOException If the backing store could not be read.
	 */
	TerminalChunk read(long fromOffset, int maxBytes) throws IOException;

	/**
	 * Copies every byte still held, oldest first, up to the end at the time of the call.
	 *
	 * <p>
	 * If the oldest bytes have been overwritten, the copy starts at the oldest offset held and the gap is skipped.
	 *
	 * @param out The stream to write to.
	 * @throws IOException If the source could not be read or the stream written.
	 */
	default void copyTo(OutputStream out) throws IOException {
		var from = 0L;
		var stop = -1L;
		while (true) {
			var c = read(from, MAX_READ_BYTES);
			if (stop < 0)
				stop = c.end();
			out.write(c.bytes());
			if (c.gone() || c.bytes().length == 0 || c.next() >= stop)
				return;
			from = c.next();
		}
	}
}
