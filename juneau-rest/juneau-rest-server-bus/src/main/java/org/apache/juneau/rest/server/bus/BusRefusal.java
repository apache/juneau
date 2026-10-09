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
package org.apache.juneau.rest.server.bus;

import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * A named refusal from the bus: the session POST's {@code 400/403/429/501} answers (spec §11.3) and the codec's
 * {@code bus:bad-frame}.  The message is fixed server text; {@link #denied()} lists the topic names the caller requested, which is the only caller input in a refusal.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jk>try</jk> {
 *     BusSession <jv>s</jv> = <jv>bus</jv>.openSession(<jv>req</jv>, <jv>body</jv>);
 *     ...
 *   } <jk>catch</jk> (BusRefusal <jv>e</jv>) {
 *     <jv>res</jv>.setStatus(<jv>e</jv>.status());
 *     <jv>e</jv>.retryAfterSeconds().ifPresent(<jv>s</jv> -&gt; <jv>res</jv>.setHeader(<js>"Retry-After"</js>, String.<jsm>valueOf</jsm>(<jv>s</jv>)));
 *     <jk>return</jk> <jv>e</jv>.toJson();   <jc>// {"code":"bus:topic-denied","message":"...","denied":["ops.secret"]}</jc>
 *   }
 * </p>
 *
 * @since 10.0.0
 */
public final class BusRefusal extends Exception {

	private static final long serialVersionUID = 1L;

	/** {@code Retry-After} seconds sent with {@code bus:too-many-sessions}. */
	public static final int TOO_MANY_SESSIONS_RETRY_AFTER_SECONDS = 5;

	private final int status;
	private final String code;
	private final List<String> denied;
	private final int retryAfterSeconds;

	/**
	 * Constructor.
	 *
	 * @param status The HTTP status.
	 * @param code The {@code bus:*} code.
	 * @param message Fixed server text.
	 * @param denied The denied topics, for {@code bus:topic-denied}.  Can be <jk>null</jk>.
	 * @param retryAfterSeconds {@code Retry-After} seconds, or {@code 0} for none.
	 */
	public BusRefusal(int status, String code, String message, List<String> denied, int retryAfterSeconds) {
		super(message, null, false, false);
		this.status = status;
		this.code = code;
		this.denied = denied == null ? List.of() : List.copyOf(denied);
		this.retryAfterSeconds = retryAfterSeconds;
	}

	static BusRefusal badRequest(String message) {
		return new BusRefusal(400, "bus:bad-request", message, null, 0);
	}

	static BusRefusal badFrame(String message) {
		return new BusRefusal(400, "bus:bad-frame", message, null, 0);
	}

	static BusRefusal topicDenied(List<String> denied) {
		return new BusRefusal(403, "bus:topic-denied", "the bus policy denies a requested topic", denied, 0);
	}

	static BusRefusal tooManySessions() {
		return new BusRefusal(429, "bus:too-many-sessions", "too many bus sessions", null, TOO_MANY_SESSIONS_RETRY_AFTER_SECONDS);
	}

	static BusRefusal transportUnavailable() {
		return new BusRefusal(501, "bus:transport-unavailable", "the websocket transport is not registered", null, 0);
	}

	/** @return The HTTP status. */
	public int status() { return status; }

	/** @return The {@code bus:*} code. */
	public String code() { return code; }

	/** @return The denied topics (empty unless {@code bus:topic-denied}). */
	public List<String> denied() { return denied; }

	/** @return The {@code Retry-After} seconds, if any. */
	public OptionalInt retryAfterSeconds() {
		return retryAfterSeconds > 0 ? OptionalInt.of(retryAfterSeconds) : OptionalInt.empty();
	}

	/**
	 * The refusal body: {@code {code, message, denied?}}.
	 *
	 * @return A new map.
	 */
	public JsonMap toJson() {
		var m = JsonMap.of("code", code, "message", getMessage());
		if (! denied.isEmpty())
			m.put("denied", denied);
		return m;
	}
}
