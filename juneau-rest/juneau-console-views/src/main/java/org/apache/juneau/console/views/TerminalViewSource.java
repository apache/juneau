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
package org.apache.juneau.console.views;

import org.apache.juneau.terminal.report.*;

/**
 * The read side of a terminal-view event stream.
 *
 * <p>
 * Contract: events come back in ascending <c>seq</c>; every page carries a <c>next</c> token; an unknown or stale
 * token throws {@link UnknownTokenException}; implementations are thread-safe.  There is no tail or before: the
 * client model needs the events from the start, and an event stream is small next to a log.
 */
public interface TerminalViewSource {

	/**
	 * Returns a forward page of events with a <c>seq</c> greater than the token's position.
	 *
	 * @param after The <c>next</c> token of the previous page, or <jk>null</jk> for the start.
	 * @param max The maximum number of events.
	 * @return A forward page.
	 * @throws UnknownTokenException If the token is unknown or stale.
	 */
	TerminalViewPage page(String after, int max);

	/**
	 * Thrown for an unknown or stale token; endpoints answer <c>410 Gone</c>.
	 */
	@SuppressWarnings({
		"java:S9398" // Public API: the exception is part of the TerminalViewSource contract and is referenced as TerminalViewSource.UnknownTokenException.
	})
	class UnknownTokenException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param token The offending token.
		 */
		public UnknownTokenException(String token) {
			super("Unknown or stale terminal-view token: '" + TerminalViewChecks.clip(token) + "'.");
		}
	}
}
