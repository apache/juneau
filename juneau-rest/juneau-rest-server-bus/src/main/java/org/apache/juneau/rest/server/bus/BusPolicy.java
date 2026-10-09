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

import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.rest.server.filter.*;

import jakarta.servlet.http.*;

/**
 * What a {@link ServerBus} will carry, in which direction, to whom.  Everything not allowed here is denied.
 *
 * <p>
 * Downstream entries are topic patterns: a custom topic ({@code ns.name}), a keyed custom topic or key pattern
 * ({@code ns.name:key}, {@code ns.name:*}), a job topic or pattern ({@code job:<id>}, {@code job:*}), or a card command
 * topic ({@code cmd:<cardId>}).  Upstream entries are exact custom topics, each with its {@link UpstreamHandler}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   BusPolicy <jv>p</jv> = BusPolicy.<jsm>create</jsm>()
 *     .downstream(<js>"ops.jobs"</js>, <jk>true</jk>)                       <jc>// retained state</jc>
 *     .downstream(<js>"ops.alert:*"</js>, <jk>false</jk>)                  <jc>// events, any key</jc>
 *     .upstream(<js>"ops.cancel-all"</js>, (<jv>session</jv>, <jv>topic</jv>, <jv>payload</jv>) -&gt; <jv>jobs</jv>.cancelAll())
 *     .authorizer((<jv>req</jv>, <jv>topic</jv>) -&gt; <jv>req</jv>.isUserInRole(<js>"ops"</js>))  <jc>// per-user, both directions</jc>
 *     .boundary(<jv>loopbackBoundary</jv>)                                   <jc>// loopback apps: WS Origin + Host</jc>
 *     .build();
 *
 *   <jc>// In a page test: every topic the page's bridges request is granted (E-55 otherwise).</jc>
 *   <jv>p</jv>.check(PageCapture.<jsm>of</jsm>(<jv>html</jv>).contract());
 * </p>
 *
 * @since 10.0.0
 */
public final class BusPolicy {

	static final String CUSTOM = "[a-z][a-z0-9-]{0,31}\\.[a-z][a-z0-9-]{0,31}";
	static final String KEY = "[A-Za-z0-9_.-]{1,128}";
	static final String CARD_ID = "[A-Za-z][A-Za-z0-9_-]{0,63}";

	/** Valid downstream topic patterns. */
	static final Pattern DOWNSTREAM_PATTERN = Pattern.compile(
		"^(" + CUSTOM + "(:(" + KEY + "|\\*))?|job:(" + KEY + "|\\*)|cmd:" + CARD_ID + ")$");

	/** Valid upstream topics: exact custom topics only. */
	static final Pattern UPSTREAM_TOPIC = Pattern.compile("^" + CUSTOM + "(:" + KEY + ")?$");

	/** The C1 id pattern, used for bridge ids. */
	static final Pattern BRIDGE_ID = Pattern.compile("^" + CARD_ID + "$");

	private final Map<String,Boolean> downstream;
	private final Map<String,UpstreamHandler> upstream;
	private final BiPredicate<HttpServletRequest,String> authorizer;
	private final LoopbackBoundary boundary;
	private final List<String> allowedOrigins;
	private final Duration sessionGrace;
	private final Duration maxSessionAge;
	private final Duration heartbeat;
	private final int maxSessions;
	private final int maxQueuedFrames;
	private final int maxFrameBytes;
	private final int maxUpstreamPerSecond;
	private final Function<HttpServletRequest,Map<String,Object>> sessionAttributes;

	private BusPolicy(Builder b) {
		downstream = Collections.unmodifiableMap(new LinkedHashMap<>(b.downstream));
		upstream = Collections.unmodifiableMap(new LinkedHashMap<>(b.upstream));
		authorizer = b.authorizer;
		boundary = b.boundary;
		allowedOrigins = b.allowedOrigins;
		sessionGrace = b.sessionGrace;
		maxSessionAge = b.maxSessionAge;
		heartbeat = b.heartbeat;
		maxSessions = b.maxSessions;
		maxQueuedFrames = b.maxQueuedFrames;
		maxFrameBytes = b.maxFrameBytes;
		maxUpstreamPerSecond = b.maxUpstreamPerSecond;
		sessionAttributes = b.sessionAttributes;
	}

	/**
	 * Creates a new builder.  An unmodified builder builds a policy that grants nothing.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns whether a topic (or a requested pattern) may flow downstream, and if so whether it is retained.
	 *
	 * <p>
	 * A policy entry covers a topic when it is equal to it, or when the entry is {@code family:*} and the topic is
	 * {@code family:<anything>}.
	 *
	 * @param topic A concrete topic or a requested pattern.  Can be <jk>null</jk>.
	 * @return The retain flag, or empty when the topic is denied.
	 */
	public Optional<Boolean> downstreamRetain(String topic) {
		if (topic == null || ! DOWNSTREAM_PATTERN.matcher(topic).matches())
			return Optional.empty();
		var exact = downstream.get(topic);
		if (exact != null)
			return Optional.of(exact);
		var i = topic.indexOf(':');
		if (i > 0) {
			var wild = downstream.get(topic.substring(0, i) + ":*");
			if (wild != null)
				return Optional.of(wild);
		}
		return Optional.empty();
	}

	/**
	 * Returns whether a topic may flow upstream (exact match only).
	 *
	 * @param topic The topic.  Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the policy has a handler for it.
	 */
	public boolean allowsUpstream(String topic) {
		return topic != null && upstream.containsKey(topic);
	}

	UpstreamHandler upstreamHandler(String topic) {
		return upstream.get(topic);
	}

	/**
	 * Checks that this policy grants every topic that a page contract's {@code bridges} request (E-55).
	 *
	 * <p>
	 * Adopter page tests call this next to {@code PageContractAssert.hasNoWiringErrors()}.  The request-dependent
	 * {@link #authorizer()} is not consulted: it can only narrow a grant at session time.
	 *
	 * @param contract The page contract (for example {@code PageCapture.of(html).contract()}).
	 * @throws IllegalStateException Listing every denied topic, one {@code E-55} line each.
	 */
	public void check(Map<String,?> contract) {
		var problems = new ArrayList<String>();
		if (contract != null && contract.get("bridges") instanceof Collection<?> bridges) {
			for (var raw : bridges) {
				if (! (raw instanceof Map<?,?> bridge))
					continue;
				var id = String.valueOf(bridge.get("id"));
				for (var t : strings(bridge.get("downstream")))
					if (downstreamRetain(t).isEmpty())
						problems.add(e55("downstream", t, id));
				for (var t : strings(bridge.get("upstream")))
					if (! allowsUpstream(t))
						problems.add(e55("upstream", t, id));
			}
		}
		if (! problems.isEmpty())
			throw new IllegalStateException(String.join("\n", problems));
	}

	private static String e55(String direction, String topic, String bridge) {
		return "E-55: " + String.format("BusPolicy denies %s topic '%s' that bridge '%s' requests", direction, topic, bridge);
	}

	private static List<String> strings(Object o) {
		return o instanceof Collection<?> c ? c.stream().map(String::valueOf).toList() : List.of();
	}

	/** @return The per-request, per-topic authorizer applied at session open.  Defaults to allow-all. */
	public BiPredicate<HttpServletRequest,String> authorizer() { return authorizer; }

	/** @return The loopback boundary, or <jk>null</jk> when the application has its own guard. */
	public LoopbackBoundary boundary() { return boundary; }

	/** @return The exact origins allowed on a WebSocket handshake when no {@link #boundary()} is set. */
	public List<String> allowedOrigins() { return allowedOrigins; }

	/** @return How long a session survives after its last connection closes.  Default 60 s. */
	public Duration sessionGrace() { return sessionGrace; }

	/** @return The absolute session lifetime.  Default 12 h. */
	public Duration maxSessionAge() { return maxSessionAge; }

	/** @return The idle interval after which a {@code ping} frame is sent.  Default 15 s. */
	public Duration heartbeat() { return heartbeat; }

	/** @return The maximum number of live sessions.  Default 256. */
	public int maxSessions() { return maxSessions; }

	/** @return The per-connection outbound queue bound.  Default 1024. */
	public int maxQueuedFrames() { return maxQueuedFrames; }

	/** @return The maximum encoded frame size in UTF-8 bytes, both directions.  Default 65536. */
	public int maxFrameBytes() { return maxFrameBytes; }

	/** @return The per-session upstream frame rate limit.  Default 20. */
	public int maxUpstreamPerSecond() { return maxUpstreamPerSecond; }

	/** @return The function that captures session attributes at session open.  Defaults to an empty map. */
	public Function<HttpServletRequest,Map<String,Object>> sessionAttributes() { return sessionAttributes; }

	/**
	 * Builder for {@link BusPolicy}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 *   BusPolicy <jv>p</jv> = BusPolicy.<jsm>create</jsm>()
	 *     .downstream(<js>"ops.jobs"</js>, <jk>true</jk>)
	 *     .sessionAttributes(<jv>req</jv> -&gt; Map.<jsm>of</jsm>(<js>"region"</js>, <jv>req</jv>.getHeader(<js>"X-Region"</js>)))
	 *     .maxSessions(64)
	 *     .build();
	 * </p>
	 */
	public static final class Builder {

		final Map<String,Boolean> downstream = new LinkedHashMap<>();
		final Map<String,UpstreamHandler> upstream = new LinkedHashMap<>();
		BiPredicate<HttpServletRequest,String> authorizer = (req, topic) -> true;
		LoopbackBoundary boundary;
		List<String> allowedOrigins = List.of();
		Duration sessionGrace = Duration.ofSeconds(60);
		Duration maxSessionAge = Duration.ofHours(12);
		Duration heartbeat = Duration.ofSeconds(15);
		int maxSessions = 256;
		int maxQueuedFrames = 1024;
		int maxFrameBytes = 65536;
		int maxUpstreamPerSecond = 20;
		Function<HttpServletRequest,Map<String,Object>> sessionAttributes = req -> Map.of();

		Builder() {}

		/**
		 * Allows a topic pattern downstream (server → page).
		 *
		 * @param topicPattern {@code ns.name}, {@code ns.name:key}, {@code ns.name:*}, {@code job:<id>}, {@code job:*} or {@code cmd:<cardId>}.
		 * @param retain Whether values are retained and resent on every (re)connect.
		 * @return This object.
		 * @throws IllegalArgumentException E-57 on an invalid or repeated pattern.
		 */
		public Builder downstream(String topicPattern, boolean retain) {
			if (topicPattern == null || ! DOWNSTREAM_PATTERN.matcher(topicPattern).matches())
				throw e57("BusPolicy: '%s' is not a valid topic pattern", topicPattern);
			if (downstream.putIfAbsent(topicPattern, retain) != null)
				throw e57("BusPolicy: '%s' is declared twice", topicPattern);
			return this;
		}

		/**
		 * Allows an exact custom topic upstream (page → server, WebSocket only) and names its handler.
		 *
		 * @param topic The topic ({@code ns.name} or {@code ns.name:key}).
		 * @param handler The handler.  Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException E-57 on an invalid or repeated topic, or a <jk>null</jk> handler.
		 */
		public Builder upstream(String topic, UpstreamHandler handler) {
			if (topic == null || ! UPSTREAM_TOPIC.matcher(topic).matches())
				throw e57("BusPolicy: '%s' is not a valid topic pattern", topic);
			if (handler == null)
				throw e57("BusPolicy: upstream topic '%s' has no handler", topic);
			if (upstream.putIfAbsent(topic, handler) != null)
				throw e57("BusPolicy: '%s' is declared twice", topic);
			return this;
		}

		/**
		 * Sets a per-request, per-topic authorizer, consulted for every requested topic at session open.
		 *
		 * @param value The authorizer.  Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder authorizer(BiPredicate<HttpServletRequest,String> value) {
			authorizer = Objects.requireNonNull(value, "authorizer");
			return this;
		}

		/**
		 * Sets the loopback boundary.  The session POST and SSE stream apply its {@code check}; the WebSocket handshake
		 * uses its {@code origin()} and {@code authority()}.
		 *
		 * @param value The boundary.  Can be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder boundary(LoopbackBoundary value) {
			boundary = value;
			return this;
		}

		/**
		 * Sets the exact origins a WebSocket handshake may carry when no boundary is set.
		 *
		 * @param values Origins such as {@code https://console.example.org}.  None may be blank.
		 * @return This object.
		 */
		public Builder allowedOrigins(String...values) {
			for (var v : Objects.requireNonNull(values, "allowedOrigins"))
				if (v == null || v.isBlank())
					throw new IllegalArgumentException("BusPolicy: allowedOrigins entries must not be blank");
			allowedOrigins = List.of(values);
			return this;
		}

		/** @param value Grace after the last connection closes.  Default 60 s. @return This object. */
		public Builder sessionGrace(Duration value) { sessionGrace = positive("sessionGrace", value); return this; }

		/** @param value Absolute session lifetime.  Default 12 h. @return This object. */
		public Builder maxSessionAge(Duration value) { maxSessionAge = positive("maxSessionAge", value); return this; }

		/** @param value Idle interval before a {@code ping} frame.  Default 15 s. @return This object. */
		public Builder heartbeat(Duration value) { heartbeat = positive("heartbeat", value); return this; }

		/** @param value Maximum live sessions.  Default 256. @return This object. */
		public Builder maxSessions(int value) { maxSessions = positive("maxSessions", value); return this; }

		/** @param value Per-connection outbound queue bound.  Default 1024. @return This object. */
		public Builder maxQueuedFrames(int value) { maxQueuedFrames = positive("maxQueuedFrames", value); return this; }

		/** @param value Maximum encoded frame size, UTF-8 bytes.  Default 65536. @return This object. */
		public Builder maxFrameBytes(int value) { maxFrameBytes = positive("maxFrameBytes", value); return this; }

		/** @param value Per-session upstream frames per second.  Default 20. @return This object. */
		public Builder maxUpstreamPerSecond(int value) { maxUpstreamPerSecond = positive("maxUpstreamPerSecond", value); return this; }

		/**
		 * Sets the function that captures attributes onto each {@link BusSession} at session open.
		 *
		 * @param value The function.  Must not be <jk>null</jk>; it may return <jk>null</jk> for "none".
		 * @return This object.
		 */
		public Builder sessionAttributes(Function<HttpServletRequest,Map<String,Object>> value) {
			sessionAttributes = Objects.requireNonNull(value, "sessionAttributes");
			return this;
		}

		/**
		 * Builds the policy.
		 *
		 * @return A new immutable policy.
		 */
		public BusPolicy build() {
			return new BusPolicy(this);
		}

		private static Duration positive(String name, Duration v) {
			if (v == null || v.isZero() || v.isNegative())
				throw new IllegalArgumentException(String.format("BusPolicy: %s must be positive", name));
			return v;
		}

		private static int positive(String name, int v) {
			if (v <= 0)
				throw new IllegalArgumentException(String.format("BusPolicy: %s must be positive", name));
			return v;
		}

		private static IllegalArgumentException e57(String format, Object...args) {
			return new IllegalArgumentException("E-57: " + String.format(format, args));
		}
	}
}
