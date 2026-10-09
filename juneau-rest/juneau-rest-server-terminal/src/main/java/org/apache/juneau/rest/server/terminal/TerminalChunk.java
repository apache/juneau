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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * One read from a {@link TerminalSource}.
 *
 * <p>
 * Offsets are absolute byte positions since the stream started; they never reset.  {@code bytes} starts at
 * {@code next - bytes.length}, which is the requested offset unless {@code truncated} is set.
 *
 * <p>
 * The record holds the {@code bytes} array itself, not a copy, and equality on it is by identity; treat it as read-only.
 *
 * @param bytes The bytes read, never <jk>null</jk>; empty at the end of the stream.
 * @param next The offset to read from next.
 * @param end The source's current end offset.
 * @param done Whether the producer will write no more and {@code next == end}.
 * @param cols The producer's terminal width.
 * @param rows The producer's terminal height.
 * @param truncated Whether the requested offset was no longer held, so the bytes start at the oldest offset held.
 * @param gone Whether the backing store disappeared; implies {@code done}.  A gone chunk is empty, and its {@code next}
 * 	and {@code end} are the requested offset.
 */
public record TerminalChunk(byte[] bytes, long next, long end, boolean done, int cols, int rows, boolean truncated, boolean gone) {

	/**
	 * Checks the members.
	 *
	 * @throws IllegalArgumentException If a member is out of range.
	 */
	public TerminalChunk {
		Objects.requireNonNull(bytes, "bytes");
		if (next < bytes.length || next > end)
			throw iaex("TerminalChunk next must be between %s and end %s; got '%s'.", bytes.length, end, next);
		if (cols < 1 || rows < 1)
			throw iaex("TerminalChunk size must be at least 1x1; got '%sx%s'.", cols, rows);
		if (gone && ! done)
			throw iaex("TerminalChunk gone requires done.");
	}
}
