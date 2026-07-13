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
package org.apache.juneau.marshall.sse;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * Represents a single <a class="doclink" href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Event</a>
 * (SSE) frame on the wire.
 *
 * <p>
 * Each event maps to a sequence of <c>field: value</c> lines terminated by a blank line in the
 * <c>text/event-stream</c> wire format:
 * <p class='bcode'>
 * event: progress
 * data: {"step":4,"total":10}
 * id: 42
 * retry: 5000
 *
 * </p>
 *
 * <p>
 * The payload field {@link #getData() data} is a <b>pre-serialized</b> {@link String} — the SSE
 * serializer does not compose with a delegate marshaller. Callers that need a typed payload should
 * serialize it themselves (e.g. via {@link org.apache.juneau.marshall.marshaller.Json#of(Object) Json.of(x)})
 * and assign the resulting string to {@link #setData(String) setData(...)}. The serializer will
 * split that string on {@code \n} into multiple <c>data:</c> lines per spec.
 *
 * <h5 class='figure'>Example: build and serialize an event:</h5>
 * <p class='bjava'>
 * 	SseEvent <jv>event</jv> = <jk>new</jk> SseEvent()
 * 		.setEvent(<js>"progress"</js>)
 * 		.setData(Json.<jsm>of</jsm>(Map.of(<js>"step"</js>, 4, <js>"total"</js>, 10)))
 * 		.setId(<js>"42"</js>)
 * 		.setRetry(5000L);
 *
 * 	String <jv>wire</jv> = Sse.<jsm>of</jsm>(<jv>event</jv>);
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Sse">SSE Basics</a>
 * 	<li class='link'><a class="doclink" href="https://html.spec.whatwg.org/multipage/server-sent-events.html">WHATWG HTML §9.2 — Server-sent events</a>
 * </ul>
 */
@BeanType(properties = "event,data,id,retry")
public class SseEvent {

	/** Default event-type name dispatched by the WHATWG <c>EventSource</c> when no <c>event:</c> field is present. */
	public static final String DEFAULT_EVENT = "message";

	private String event;
	private String data;
	private String id;
	private Long retry;

	/**
	 * Constructor.
	 */
	public SseEvent() {}

	/**
	 * Constructor.
	 *
	 * @param event The event-type name. Can be <jk>null</jk> (dispatches as {@value #DEFAULT_EVENT}).
	 * @param data The payload string. Can be <jk>null</jk> (no <c>data:</c> lines emitted).
	 */
	public SseEvent(String event, String data) {
		this.event = event;
		this.data = data;
	}

	/**
	 * Returns the event-type name.
	 *
	 * @return The event-type name, or <jk>null</jk> if not set.
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * Sets the event-type name.
	 *
	 * @param value The event-type name. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SseEvent setEvent(String value) {
		event = value;
		return this;
	}

	/**
	 * Returns the data payload (pre-serialized string).
	 *
	 * @return The data payload, or <jk>null</jk> if not set.
	 */
	public String getData() {
		return data;
	}

	/**
	 * Sets the data payload.
	 *
	 * @param value The pre-serialized payload string. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SseEvent setData(String value) {
		data = value;
		return this;
	}

	/**
	 * Returns the last-event-id value.
	 *
	 * @return The id, or <jk>null</jk> if not set.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the last-event-id value.
	 *
	 * @param value The id. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SseEvent setId(String value) {
		id = value;
		return this;
	}

	/**
	 * Returns the reconnect-delay hint in milliseconds.
	 *
	 * @return The retry value, or <jk>null</jk> if not set.
	 */
	public Long getRetry() {
		return retry;
	}

	/**
	 * Sets the reconnect-delay hint in milliseconds.
	 *
	 * @param value The retry value. Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public SseEvent setRetry(Long value) {
		retry = value;
		return this;
	}

	@Override /* Overridden from Object */
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SseEvent o2))
			return false;
		return eq(event, o2.event)
			&& eq(data, o2.data)
			&& eq(id, o2.id)
			&& eq(retry, o2.retry);
	}

	@Override /* Overridden from Object */
	public int hashCode() {
		return Objects.hash(event, data, id, retry);
	}

	@Override /* Overridden from Object */
	public String toString() {
		return "SseEvent[event=" + event + ",data=" + data + ",id=" + id + ",retry=" + retry + "]";
	}
}
