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
package org.apache.juneau.rest.server.console;

import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * Declares one server bridge in the page contract's {@code bridges} list (§11.2).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   PageSpec.<jsm>create</jsm>()
 *     .topic(TopicDecl.<jsm>of</jsm>(<js>"ops.jobs"</js>).retain(<jk>true</jk>).publisher(TopicDecl.Publisher.<jsf>SERVER</jsf>))
 *     .bridge(BridgeDecl.<jsm>sse</jsm>(<js>"ops"</js>, <js>"servlet:/juneau-bus/session"</js>).downstream(<js>"ops.jobs"</js>, Topics.<jsm>cmd</jsm>(<js>"jobs"</js>)));
 *
 *   <jc>// WebSocket, with an upstream topic a ribbon item publishes:</jc>
 *   BridgeDecl.<jsm>websocket</jsm>(<js>"ops"</js>, <js>"servlet:/juneau-bus/session"</js>)
 *     .downstream(<js>"ops.jobs"</js>).upstream(<js>"ops.cancel-all"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class BridgeDecl {

	private final String id, transport, session;
	private final List<String> downstream = new ArrayList<>(), upstream = new ArrayList<>();
	private Integer maxAttempts;

	private BridgeDecl(String id, String transport, String session) {
		if (id == null || ! Topics.ID.matcher(id).matches())
			throw e51(id, "id must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$");
		if (session == null || ! Topics.SESSION.matcher(session).matches())
			throw e51(id, "session '" + session + "' must be a same-origin path or a servlet:/context: URI");
		this.id = id;
		this.transport = transport;
		this.session = session;
	}

	/** @param id The bridge id. @param sessionUrl The session-POST URL. @return A new downstream-only SSE bridge. */
	public static BridgeDecl sse(String id, String sessionUrl) {
		return new BridgeDecl(id, "sse", sessionUrl);
	}

	/** @param id The bridge id. @param sessionUrl The session-POST URL. @return A new WebSocket bridge. */
	public static BridgeDecl websocket(String id, String sessionUrl) {
		return new BridgeDecl(id, "websocket", sessionUrl);
	}

	/** @param topicPatterns Topics the page accepts from the server; appends. @return This object. */
	public BridgeDecl downstream(String... topicPatterns) {
		for (var t : topicPatterns) {
			if (t == null || ! (Topics.TOPIC.matcher(t).matches() || Topics.TOPIC_DECL.matcher(t).matches()))
				throw Topics.iae(Topics.E40, t);
			downstream.add(t);
		}
		return this;
	}

	/** @param topics Custom topics forwarded to the server; appends. WebSocket only. @return This object. */
	public BridgeDecl upstream(String... topics) {
		if (! "websocket".equals(transport))
			throw e51(id, "upstream is allowed only on a websocket bridge");
		for (var t : topics) {
			Topics.checkTopic(t);
			upstream.add(t);
		}
		return this;
	}

	/** @param v Give up after this many failed attempts (at least 1); unset retries while the page is open. @return This object. */
	public BridgeDecl maxAttempts(int v) {
		if (v < 1)
			throw e51(id, "maxAttempts must be >= 1; got " + v);
		maxAttempts = v;
		return this;
	}

	/** @return The id. */
	public String id() {
		return id;
	}

	/** @return The {@code $defs/bridgeDecl} entry, with {@code session} unresolved. */
	public JsonMap toMap() {
		if (downstream.isEmpty() && upstream.isEmpty())
			throw e51(id, "carries no topics; add downstream(...) or upstream(...)");
		for (var t : upstream)
			if (downstream.contains(t))
				throw e51(id, "topic '" + t + "' is both downstream and upstream");
		var m = new JsonMap();
		m.put("id", id);
		m.put("transport", transport);
		m.put("session", session);
		m.put("downstream", List.copyOf(downstream));
		if (! upstream.isEmpty())
			m.put("upstream", List.copyOf(upstream));
		if (maxAttempts != null)
			m.put("maxAttempts", maxAttempts);
		return m;
	}

	static IllegalArgumentException e51(String id, String detail) {
		return Topics.iae("bridge '%s': %s", id, detail);
	}
}
