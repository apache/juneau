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
package org.apache.juneau.rest.server.logging;

import java.util.logging.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.test.assertions.*;

/**
 * A {@link RestDebugFormatter} that captures the last fully-rendered (cumulative) debug message for test assertions.
 *
 * <p>
 * Replaces the pre-10.0 {@code CapturingFormat}/{@code BasicTestCaptureCallLogger} test utilities. Instead of emitting
 * the rendered message to a log file, the cumulative message is retained in an internal holder alongside the resolved
 * tier level and thrown exception. After a request, tests can inspect them via {@link #getMessage()}/
 * {@link #assertMessage()} (and the {@code *AndReset} variants), {@link #getLevel()}, and {@link #getThrown()}.
 *
 * <p>
 * The tier {@link #getLevel() level} is inferred from which tier methods the pipeline invoked for the current record:
 * {@link #formatBasic(RestRequest,RestResponse) formatBasic} always begins a new capture ({@code INFO});
 * {@link #formatHeaders(RestRequest,RestResponse) formatHeaders} raises it to {@code FINE};
 * {@link #formatBody(RestRequest,RestResponse) formatBody} raises it to {@code FINEST}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerLoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class CapturingRestDebugFormatter extends BasicRestDebugFormatter {

	private StringBuilder buffer;
	private String message;
	private Level level;
	private Throwable thrown;

	@Override /* Overridden from BasicRestDebugFormatter */
	public synchronized String formatBasic(RestRequest req, RestResponse res) {
		var s = super.formatBasic(req, res);
		buffer = new StringBuilder(s);
		message = buffer.toString();
		level = Level.INFO;
		thrown = req.getException();
		return s;
	}

	@Override /* Overridden from BasicRestDebugFormatter */
	public synchronized String formatHeaders(RestRequest req, RestResponse res) {
		var s = super.formatHeaders(req, res);
		if (buffer != null) {
			buffer.append(s);
			message = buffer.toString();
		}
		level = Level.FINE;
		return s;
	}

	@Override /* Overridden from BasicRestDebugFormatter */
	public synchronized String formatBody(RestRequest req, RestResponse res) {
		var s = super.formatBody(req, res);
		if (buffer != null) {
			buffer.append(s);
			message = buffer.toString();
		}
		level = Level.FINEST;
		return s;
	}

	/**
	 * Returns an assertion of the last captured message.
	 *
	 * @return The last captured message as an assertion object. Never <jk>null</jk>.
	 */
	public StringAssertion assertMessage() {
		return new StringAssertion(getMessage());
	}

	/**
	 * Returns an assertion of the last captured message and then clears the holder.
	 *
	 * @return The last captured message as an assertion object. Never <jk>null</jk>.
	 */
	public StringAssertion assertMessageAndReset() {
		return new StringAssertion(getMessageAndReset());
	}

	/**
	 * Returns an assertion of the last captured throwable.
	 *
	 * @return The last captured throwable as an assertion object. Never <jk>null</jk>.
	 */
	public ThrowableAssertion<Throwable> assertThrown() {
		return new ThrowableAssertion<>(getThrown());
	}

	/**
	 * Returns the resolved tier level of the last captured message.
	 *
	 * @return The last captured level, or <jk>null</jk> if nothing was captured.
	 */
	public synchronized Level getLevel() {
		return level;
	}

	/**
	 * Returns the last captured (cumulative) message.
	 *
	 * @return The last captured message, or <jk>null</jk> if nothing was captured.
	 */
	public synchronized String getMessage() {
		return message;
	}

	/**
	 * Returns the last captured message and then clears the holder.
	 *
	 * @return The last captured message, or <jk>null</jk> if nothing was captured.
	 */
	public synchronized String getMessageAndReset() {
		var m = message;
		reset();
		return m;
	}

	/**
	 * Returns the last captured throwable.
	 *
	 * @return The last captured throwable, or <jk>null</jk> if nothing was captured.
	 */
	public synchronized Throwable getThrown() {
		return thrown;
	}

	/**
	 * Clears the internal holder.
	 *
	 * @return This object.
	 */
	public synchronized CapturingRestDebugFormatter reset() {
		buffer = null;
		message = null;
		level = null;
		thrown = null;
		return this;
	}
}
