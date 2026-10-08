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
package org.apache.juneau.rest.server.views;

import java.util.stream.*;

/**
 * The read side of a console-output log, served by {@link ConsoleOutputMixin} and {@link ConsoleOutputEndpoints}.
 *
 * <p>
 * Implementations must honour these obligations:
 * <ul>
 * 	<li>Tokens are opaque URL-safe strings matching {@code ^[A-Za-z0-9._~-]{1,128}$}, and exclusive: {@code next}
 * 		is the position just after the last line returned; {@code before} is the position of {@code lines[0]}.
 * 	<li>Every forward or tail page carries {@code next}, even when it has no lines.
 * 	<li>An unknown or stale token throws {@link UnknownTokenException} (mapped to {@code 410 Gone}).
 * 	<li>A partial trailing line is never emitted until it is complete or the log is terminal.
 * 	<li>{@link #stream()} returns lines in ascending {@code n}; the caller closes it.
 * 	<li>Methods may be called concurrently from different requests.
 * </ul>
 *
 * <p>
 * A source that cannot tail cheaply may implement {@link #tail(int)} as {@code page(null, max)}, but only when its
 * logs are known to stay small: the client then reads the whole log. A file-backed or otherwise unbounded source
 * must implement a real tail (see {@link FileConsoleOutputSource}). A source that answers {@code tail} must also
 * answer {@code before}.
 *
 * @since 10.0.0
 */
public interface ConsoleOutputSource {

	/**
	 * Returns a forward page.
	 *
	 * @param after The {@code next} token of the previous page, or <jk>null</jk> for the start.
	 * @param max The maximum number of lines.
	 * @return A forward page.
	 * @throws UnknownTokenException If the token is unknown or stale.
	 */
	ConsoleOutputPage page(String after, int max);

	/**
	 * Returns the last {@code n} complete lines as a forward page.
	 *
	 * @param n The number of lines.
	 * @return A forward page whose {@code next} continues from the end.
	 */
	ConsoleOutputPage tail(int n);

	/**
	 * Returns up to {@code limit} lines immediately preceding a {@code before} token.
	 *
	 * @param token A {@code before} token.
	 * @param limit The maximum number of lines.
	 * @return An earlier page.
	 * @throws UnknownTokenException If the token is unknown or stale.
	 */
	ConsoleOutputPage before(String token, int limit);

	/**
	 * Returns the whole log, ascending. The caller closes the stream.
	 *
	 * @return A stream of lines.
	 */
	Stream<ConsoleOutputLine> stream();

	/**
	 * Thrown for an unknown or stale token; endpoints answer {@code 410 Gone}.
	 */
	class UnknownTokenException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param token The offending token.
		 */
		public UnknownTokenException(String token) {
			super("Unknown or stale console-output token: '" + ConsoleOutputChecks.clip(token) + "'.");
		}
	}
}
