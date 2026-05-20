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
package org.apache.juneau.sse;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;

/**
 * Line-driven {@link Iterator} over the events in a <c>text/event-stream</c>.
 *
 * <p>
 * Wraps a {@link Reader} the caller owns and dispatches one {@link SseEvent} at a time using the
 * WHATWG parse state machine (<a class="doclink" href="https://html.spec.whatwg.org/multipage/server-sent-events.html">HTML §9.2</a>).
 * Designed for long-lived SSE streams that outlive any reasonable per-call buffer.
 *
 * <h5 class='section'>Why a separate class:</h5>
 * <p>
 * No other Juneau marshaller ships a separate iterator-style helper. SSE warrants the deviation
 * because SSE responses are open until the client disconnects or the server pushes its last event;
 * forcing callers through the eager {@code List<SseEvent>} path would defeat the entire point of
 * using SSE.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>try</jk> (SseEventReader <jv>r</jv> = <jk>new</jk> SseEventReader(<jv>responseReader</jv>)) {
 * 		<jk>while</jk> (<jv>r</jv>.hasNext()) {
 * 			SseEvent <jv>e</jv> = <jv>r</jv>.next();
 * 			<jc>// Dispatch...</jc>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SseBasics">SSE Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S6541" // Brain method acceptable for readLine / nextEvent — they encode the SSE lexical state machine
})
public class SseEventReader implements Iterator<SseEvent>, Closeable {

	@SuppressWarnings("java:S115") // Match AssertionUtils arg-name style used throughout Juneau.
	private static final String ARG_in = "in";

	/** UTF-8 BOM character that must be stripped exactly once from the start of an SSE stream. */
	private static final int BOM = 0xFEFF;

	private final Reader in;

	private boolean bomChecked;
	private boolean eof;
	private SseEvent peeked;

	private final StringBuilder dataBuf = new StringBuilder();
	private String eventName;
	private String lastEventId;
	private Long retryValue;
	private boolean haveAnyField;

	private int pendingChar = -1;

	/**
	 * Constructor.
	 *
	 * @param in The reader to consume events from. Must not be <jk>null</jk>. Caller retains ownership;
	 * 	{@link #close()} closes this reader.
	 */
	@SuppressWarnings({ "java:S2095", "resource" }) // Reader ownership is transferred to this Closeable and released by close().
	public SseEventReader(Reader in) {
		this.in = assertArgNotNull(ARG_in, in);
	}

	@Override /* Overridden from Iterator */
	public boolean hasNext() {
		if (peeked != null)
			return true;
		if (eof)
			return false;
		try {
			peeked = nextEvent();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return peeked != null;
	}

	@Override /* Overridden from Iterator */
	public SseEvent next() {
		if (!hasNext())
			throw new NoSuchElementException();
		var out = peeked;
		peeked = null;
		return out;
	}

	@Override /* Overridden from Closeable */
	public void close() throws IOException {
		in.close();
	}

	/**
	 * Drains the remaining events into a list, then closes this reader.
	 *
	 * @return The list of events in order.
	 * @throws IOException If an I/O error occurs while reading.
	 */
	public List<SseEvent> toList() throws IOException {
		var out = new ArrayList<SseEvent>();
		try {
			while (hasNext())
				out.add(next());
		} catch (UncheckedIOException e) {
			throw e.getCause();
		} finally {
			close();
		}
		return out;
	}

	private SseEvent nextEvent() throws IOException {
		while (true) {
			var line = readLine();
			if (line == null) {
				eof = true;
				if (haveAnyField)
					return dispatch();
				return null;
			}
			if (line.isEmpty()) {
				if (haveAnyField)
					return dispatch();
				continue;
			}
			processField(line);
		}
	}

	private SseEvent dispatch() {
		var out = new SseEvent();
		out.setEvent(eventName);
		if (!dataBuf.isEmpty()) {
			if (dataBuf.charAt(dataBuf.length() - 1) == '\n')
				dataBuf.deleteCharAt(dataBuf.length() - 1);
			out.setData(dataBuf.toString());
		}
		out.setId(lastEventId);
		out.setRetry(retryValue);
		resetEventBuffers();
		return out;
	}

	private void resetEventBuffers() {
		dataBuf.setLength(0);
		eventName = null;
		lastEventId = null;
		retryValue = null;
		haveAnyField = false;
	}

	private void processField(String line) {
		if (line.charAt(0) == ':') {
			haveAnyField = true;
			return;
		}
		String fieldName;
		String fieldValue;
		var colon = line.indexOf(':');
		if (colon < 0) {
			fieldName = line;
			fieldValue = "";
		} else {
			fieldName = line.substring(0, colon);
			fieldValue = line.substring(colon + 1);
			if (!fieldValue.isEmpty() && fieldValue.charAt(0) == ' ')
				fieldValue = fieldValue.substring(1);
		}
		haveAnyField = true;
		applyField(fieldName, fieldValue);
	}

	private void applyField(String fieldName, String fieldValue) {
		switch (fieldName) {
			case "event":
				eventName = fieldValue;
				break;
			case "data":
				dataBuf.append(fieldValue).append('\n');
				break;
			case "id":
				if (fieldValue.indexOf('\u0000') < 0)
					lastEventId = fieldValue;
				break;
			case "retry":
				retryValue = parseRetry(fieldValue);
				break;
			default:
				break;
		}
	}

	private static Long parseRetry(String s) {
		if (s.isEmpty())
			return null;
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			if (c < '0' || c > '9')
				return null;
		}
		try {
			return Long.valueOf(s);
		} catch (@SuppressWarnings("unused") NumberFormatException e) {
			return null;
		}
	}

	@SuppressWarnings("java:S3776") // SSE line parsing is a deliberate lexical state machine.
	private String readLine() throws IOException {
		var sb = new StringBuilder();
		int c;
		var sawAny = false;
		while (true) {
			if (pendingChar != -1) {
				c = pendingChar;
				pendingChar = -1;
			} else {
				c = in.read();
			}
			if (c == -1) {
				if (!sawAny)
					return null;
				return sb.toString();
			}
			if (!bomChecked) {
				bomChecked = true;
				if (c == BOM)
					continue;
			}
			sawAny = true;
			if (c == '\n')
				return sb.toString();
			if (c == '\r') {
				var next = in.read();
				if (next != -1 && next != '\n')
					pendingChar = next;
				return sb.toString();
			}
			sb.append((char) c);
		}
	}
}
