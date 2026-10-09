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

import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.sse.*;

import jakarta.servlet.http.*;

/**
 * The server half of the console message bus: a retained-value store plus the sessions that bridge it to pages.
 *
 * <p>
 * Publish from anywhere in the application; every connected page whose bridge was granted the topic receives it
 * (and every page that connects later receives the retained value).  Topics not allowed by the {@link BusPolicy}
 * are refused loudly (E-56).
 *
 * <p>
 * The methods under "transport SPI" are public for {@link BusEventsMixin} and {@code BusWebSockets}; application code
 * does not call them.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 *   <jc>// One per application (or per resource), shared by the REST resource and any background work.</jc>
 *   ServerBus <jv>bus</jv> = ServerBus.<jsm>create</jsm>(BusPolicy.<jsm>create</jsm>()
 *     .downstream(<js>"ops.jobs"</js>, <jk>true</jk>)
 *     .downstream(<js>"cmd:jobs"</js>, <jk>false</jk>)
 *     .build());
 *
 *   <jv>bus</jv>.publish(<js>"ops.jobs"</js>, JsonMap.<jsm>of</jsm>(<js>"schemaVersion"</js>, 1, <js>"running"</js>, <jv>running</jv>));
 *   <jv>bus</jv>.publish(<js>"cmd:jobs"</js>, JsonMap.<jsm>of</jsm>(<js>"schemaVersion"</js>, 1, <js>"op"</js>, <js>"reload"</js>));
 *
 *   <jc>// Only to one user's pages (never retained):</jc>
 *   <jv>bus</jv>.publishTo(<jv>s</jv> -&gt; <js>"jb"</js>.equals(<jv>s</jv>.principalName()), <js>"ops.jobs"</js>, <jv>mine</jv>);
 * </p>
 *
 * @since 10.0.0
 */
public final class ServerBus implements AutoCloseable {

	private static final Logger LOGGER = Logger.getLogger(ServerBus.class.getName());
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final AtomicLong STREAM_KEYS = new AtomicLong();

	/** Session id strength: 256 bits, written as 64 hex characters (spec §11.3). */
	static final int ID_BYTES = 32;

	/** The transports a session POST may request. */
	static final Set<String> TRANSPORTS = Set.of("sse", "websocket");

	/** The longest denied topic a 403 body echoes back. */
	static final int MAX_ECHO_LENGTH = 128;

	/** The most topics one session POST may request in each direction. */
	static final int MAX_TOPICS = 256;

	private final BusPolicy policy;
	private final Clock clock;
	private final ScheduledExecutorService timer;
	private final SseBroadcaster sseStreams;
	private final Object lock = new Object();

	// Guarded by lock.
	private final SortedMap<String,String> retained = new TreeMap<>();
	private final Map<String,SessionImpl> sessions = new LinkedHashMap<>();
	private boolean closed;

	private volatile String webSocketPrefix;

	ServerBus(BusPolicy policy, Clock clock, ScheduledExecutorService timer) {
		this.policy = Objects.requireNonNull(policy, "policy");
		this.clock = Objects.requireNonNull(clock, "clock");
		this.timer = timer;
		// One spare slot per queue: SseFrameSink bounds itself at maxQueuedFrames, so drop-oldest never runs.
		this.sseStreams = new SseBroadcaster(policy.maxQueuedFrames() + 1);
	}

	/**
	 * Creates a bus and starts its timer (one daemon thread: heartbeat pings and session expiry).
	 *
	 * @param policy The policy.  Must not be <jk>null</jk>.
	 * @return A new bus.  {@link #close()} it when the application stops.
	 */
	public static ServerBus create(BusPolicy policy) {
		Objects.requireNonNull(policy, "policy");
		var timer = Executors.newSingleThreadScheduledExecutor(r -> {
			var t = new Thread(r, "juneau-bus-timer");
			t.setDaemon(true);
			return t;
		});
		var bus = new ServerBus(policy, Clock.systemUTC(), timer);
		var period = Math.min(1000L, policy.heartbeat().toMillis());
		timer.scheduleAtFixedRate(bus::tickSafely, period, period, TimeUnit.MILLISECONDS);
		return bus;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Application API
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Publishes a value to every connected session granted the topic, and stores it if the topic is retained.
	 *
	 * <p>
	 * For a retained topic, a value whose JSON equals the stored JSON is not republished (distinct-until-changed).
	 *
	 * @param topic A concrete topic the policy allows downstream (not a {@code :*} pattern).
	 * @param payload The payload, serialized with {@link Json#of(Object)}.  Must not be <jk>null</jk>; use
	 * 	{@link #clear(String)} to remove a retained value.
	 * @return <jk>false</jk> when the topic is retained and the value is unchanged; otherwise <jk>true</jk>.
	 * @throws IllegalArgumentException E-56 when the policy denies the topic or the frame is larger than
	 * 	{@link BusPolicy#maxFrameBytes()}.
	 */
	public boolean publish(String topic, Object payload) {
		var retain = allowed("publish", topic);
		var json = encode("publish", topic, payload);
		synchronized (lock) {
			if (retain) {
				if (json.equals(retained.get(topic)))
					return false;
				retained.put(topic, json);
			}
			for (var s : List.copyOf(sessions.values()))
				if (s.grants(topic))
					s.send(seq -> BusFrames.pub(topic, json, retain, seq));
		}
		return true;
	}

	/**
	 * Removes a retained value and sends {@code clear} to every connected session granted the topic.
	 *
	 * <p>
	 * Clearing a topic that holds no value does nothing.
	 *
	 * @param topic A retained topic.
	 * @throws IllegalArgumentException E-56 when the policy denies the topic or does not retain it.
	 */
	public void clear(String topic) {
		if (! allowed("clear", topic))
			throw e56("ServerBus.clear('%s'): the policy does not retain it", topic);
		synchronized (lock) {
			if (retained.remove(topic) == null)
				return;
			for (var s : List.copyOf(sessions.values()))
				if (s.grants(topic))
					s.send(seq -> BusFrames.clear(topic, seq));
		}
	}

	/**
	 * Publishes a value to the connected sessions that match an audience.  Never retained, even on a retained topic.
	 *
	 * @param audience Selects sessions.  Runs under the bus lock: keep it cheap and side-effect free.
	 * @param topic A concrete topic the policy allows downstream.
	 * @param payload The payload.  Must not be <jk>null</jk>.
	 * @return The number of sessions that accepted the frame.
	 * @throws IllegalArgumentException E-56, as for {@link #publish(String, Object)}.
	 */
	public int publishTo(Predicate<BusSession> audience, String topic, Object payload) {
		Objects.requireNonNull(audience, "audience");
		allowed("publishTo", topic);
		var json = encode("publishTo", topic, payload);
		var reached = 0;
		synchronized (lock) {
			for (var s : List.copyOf(sessions.values()))
				if (s.isConnected() && s.grants(topic) && audience.test(s) && s.send(seq -> BusFrames.pub(topic, json, false, seq)))
					reached++;
		}
		return reached;
	}

	/**
	 * The stored JSON of a retained topic.
	 *
	 * @param topic The topic.
	 * @return The JSON, or empty when nothing is stored.
	 */
	public Optional<String> retainedJson(String topic) {
		synchronized (lock) {
			return Optional.ofNullable(retained.get(topic));
		}
	}

	/**
	 * The live sessions (connected, or disconnected within their grace period).
	 *
	 * @return An immutable snapshot.
	 */
	public List<BusSession> sessions() {
		synchronized (lock) {
			sweep(clock.instant());
			return List.copyOf(sessions.values());
		}
	}

	/** @return The policy this bus enforces. */
	public BusPolicy policy() {
		return policy;
	}

	/**
	 * Closes every connection with {@code 1001} ({@code bus:closed}), forgets every session and stops the timer.
	 * Retained values are kept.  Idempotent.
	 */
	@Override /* AutoCloseable */
	public void close() {
		synchronized (lock) {
			if (closed)
				return;
			closed = true;
			for (var s : sessions.values())
				s.detach(1001, "bus:closed");
			sessions.clear();
		}
		if (timer != null)
			timer.shutdownNow();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transport SPI
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Transport SPI: opens a session from a session POST body (spec §11.3).
	 *
	 * <p>
	 * Body: {@code {"v":1, "bridge":"ops", "transport":"sse"|"websocket", "downstream":[...], "upstream":[...]}}.
	 * {@code v} defaults to {@code 1} and {@code transport} to {@code "sse"}; {@code bridge} is required.
	 *
	 * @param req The POST request: the authorizer's argument and the source of the principal and session attributes.
	 * @param body The parsed body.  <jk>null</jk> is a 400.
	 * @return The new session.
	 * @throws BusRefusal 400 {@code bus:bad-request}, 403 {@code bus:topic-denied}, 429 {@code bus:too-many-sessions},
	 * 	501 {@code bus:transport-unavailable}, or 503 {@code bus:closed}.
	 */
	public BusSession openSession(HttpServletRequest req, JsonMap body) throws BusRefusal {
		Objects.requireNonNull(req, "req");
		if (body == null)
			throw BusRefusal.badRequest("the session request must be a JSON object");
		var v = body.get("v");
		if (v != null && ! (v instanceof Number n && n.doubleValue() == BusFrames.VERSION))
			throw BusRefusal.badRequest("unsupported bus version");
		if (! (body.get("bridge") instanceof String bridge) || ! BusPolicy.BRIDGE_ID.matcher(bridge).matches())
			throw BusRefusal.badRequest("bridge must be a valid id");
		var t = body.get("transport");
		if (t != null && ! (t instanceof String x && TRANSPORTS.contains(x)))
			throw BusRefusal.badRequest("transport must be 'sse' or 'websocket'");
		var transport = t == null ? "sse" : (String) t;
		var downstream = topics(body, "downstream");
		var upstream = topics(body, "upstream");
		if (downstream.isEmpty() && upstream.isEmpty())
			throw BusRefusal.badRequest("the session requests no topics");
		if (! upstream.isEmpty() && ! "websocket".equals(transport))
			throw BusRefusal.badRequest("upstream topics require the websocket transport");
		if ("websocket".equals(transport) && webSocketPrefix == null)
			throw BusRefusal.transportUnavailable();

		var authorizer = policy.authorizer();
		var denied = new ArrayList<String>();
		for (var x : downstream)
			if (policy.downstreamRetain(x).isEmpty() || ! authorizer.test(req, x))
				denied.add(x);
		for (var x : upstream)
			if (! policy.allowsUpstream(x) || ! authorizer.test(req, x))
				denied.add(x);
		if (! denied.isEmpty())
			throw BusRefusal.topicDenied(echoable(denied));

		var principal = req.getUserPrincipal();
		var attributes = policy.sessionAttributes().apply(req);
		synchronized (lock) {
			if (closed)
				throw new BusRefusal(503, "bus:closed", "the bus is closed", null, 0);
			var now = clock.instant();
			sweep(now);
			if (sessions.size() >= policy.maxSessions())
				throw BusRefusal.tooManySessions();
			var s = new SessionImpl(newId(), bridge, transport, principal == null ? null : principal.getName(),
				attributes, downstream, upstream, now);
			sessions.put(s.id, s);
			return s;
		}
	}

	/**
	 * Transport SPI: finds a live session by its capability id, for the principal that opened it.
	 *
	 * @param id The session id from the request path.  Can be <jk>null</jk>.
	 * @param principal The request principal.  Can be <jk>null</jk> (anonymous).
	 * @return The session, or empty when the id is unknown or expired, or the principal differs.  The three cases are
	 * 	deliberately indistinguishable.
	 */
	public Optional<BusSession> session(String id, Principal principal) {
		if (id == null)
			return Optional.empty();
		synchronized (lock) {
			var s = sessions.get(id);
			if (s == null)
				return Optional.empty();
			if (s.expired(clock.instant())) {
				expire(s);
				return Optional.empty();
			}
			if (! Objects.equals(s.principalName, principal == null ? null : principal.getName()))
				return Optional.empty();
			return Optional.of(s);
		}
	}

	/**
	 * Transport SPI: attaches a connection to a session, then sends the resync (spec §11.6).
	 *
	 * <p>
	 * Any live sink is replaced and closed with {@code 4409} ({@code bus:replaced}).  The resync is
	 * {@code resync-begin}, one retained {@code pub} for every stored value the session's grant covers, in topic order,
	 * then {@code resync-end}.  When the bus is closed or the session has expired, the sink is closed at once
	 * ({@code 1001} / {@code 4401}).
	 *
	 * @param session A session of this bus.
	 * @param sink The connection's outbound side.
	 */
	public void connect(BusSession session, FrameSink sink) {
		var s = impl(session);
		Objects.requireNonNull(sink, "sink");
		synchronized (lock) {
			if (closed) {
				sink.close(1001, "bus:closed");
				return;
			}
			if (sessions.get(s.id) != s) {
				sink.close(4401, "bus:unknown-session");
				return;
			}
			if (s.expired(clock.instant())) {
				expire(s);
				sink.close(4401, "bus:unknown-session");
				return;
			}
			var old = s.sink;
			s.sink = sink;
			s.lastSent = clock.instant();
			if (old != null)
				old.close(4409, "bus:replaced");
			if (! s.send(BusFrames::resyncBegin))
				return;
			for (var e : retained.entrySet())
				if (s.grants(e.getKey()) && ! s.send(seq -> BusFrames.pub(e.getKey(), e.getValue(), true, seq)))
					return;
			s.send(BusFrames::resyncEnd);
		}
	}

	/**
	 * Transport SPI: the connection behind a sink has closed.  Starts the session's grace period.
	 *
	 * <p>
	 * A sink that is no longer the session's live sink (it was replaced, or detached as a slow consumer) is ignored.
	 *
	 * @param session A session of this bus.
	 * @param sink The sink passed to {@link #connect(BusSession, FrameSink)}.
	 */
	public void disconnect(BusSession session, FrameSink sink) {
		var s = impl(session);
		synchronized (lock) {
			if (sink != null && s.sink == sink) {
				s.sink = null;
				s.disconnectedAt = clock.instant();
			}
		}
	}

	/**
	 * Transport SPI: one upstream frame from a WebSocket connection (spec §11.4, §11.6).
	 *
	 * <p>
	 * Refusals are answered with an {@code error} frame and the frame is dropped; the connection stays open.
	 *
	 * @param session A session of this bus.
	 * @param frameText The frame text.
	 */
	public void receive(BusSession session, String frameText) {
		var s = impl(session);
		synchronized (lock) {
			if (sessions.get(s.id) != s)
				return;
			if (s.expired(clock.instant())) {
				expire(s);
				return;
			}
			if (! s.isConnected())
				return;
			if (! s.admitUpstream(clock.instant(), policy.maxUpstreamPerSecond())) {
				s.offer(BusFrames.error("bus:rate-limited", "too many upstream frames", null));
				return;
			}
		}
		JsonMap frame;
		try {
			if (frameText != null && BusFrames.utf8Length(frameText) > policy.maxFrameBytes())
				throw BusRefusal.badFrame("frame too large");
			frame = BusFrames.decode(frameText);
			if (! "pub".equals(frame.get("type")))
				throw BusRefusal.badFrame("only pub frames may be sent upstream");
			if (! (frame.get("payload") instanceof Map<?,?>))
				throw BusRefusal.badFrame("an upstream payload must be a JSON object");
		} catch (BusRefusal e) {
			reply(s, BusFrames.error(e.code(), e.getMessage(), null));
			return;
		}
		var topic = (String) frame.get("topic");
		if (! s.upstream.contains(topic) || ! policy.allowsUpstream(topic)) {
			var echo = BusPolicy.UPSTREAM_TOPIC.matcher(topic).matches() ? topic : null;
			reply(s, BusFrames.error("bus:upstream-denied", "the session is not granted this upstream topic", echo));
			return;
		}
		var p = (Map<?,?>) frame.get("payload");
		var payload = p instanceof JsonMap j ? j : JsonMap.of(p);
		try {
			policy.upstreamHandler(topic).handle(s, topic, payload);
		} catch (Exception e) {
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			LOGGER.log(Level.WARNING, e, () -> String.format("bus upstream handler for topic '%s' failed", topic));
			reply(s, BusFrames.error("bus:upstream-failed", "handler failed", topic));
		}
	}

	/**
	 * Transport SPI: records that the WebSocket transport is registered, so {@code transport:"websocket"} sessions may
	 * open and the session POST response carries {@code paths.websocket}.
	 *
	 * @param contextRelativePathPrefix The endpoint path without its {@code /{sessionId}} segment, for example
	 * 	{@code /juneau-bus/ws}.  Must start with {@code /} and not end with {@code /}.
	 */
	public void enableWebSocket(String contextRelativePathPrefix) {
		if (contextRelativePathPrefix == null || ! contextRelativePathPrefix.startsWith("/") || contextRelativePathPrefix.endsWith("/"))
			throw new IllegalArgumentException(String.format(
				"ServerBus.enableWebSocket: '%s' must start with '/' and must not end with '/'", contextRelativePathPrefix));
		webSocketPrefix = contextRelativePathPrefix;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Package-private: BusEventsMixin and tests
	//-----------------------------------------------------------------------------------------------------------------

	/** The WebSocket path prefix passed to {@link #enableWebSocket(String)}, or <jk>null</jk>. */
	String webSocketPrefix() {
		return webSocketPrefix;
	}

	/** The capability id of a session of any {@code ServerBus}.  Only the session POST response may carry it. */
	static String idOf(BusSession session) {
		return impl(session).id;
	}

	/**
	 * The SSE stream's subscription for a session (P1: built on {@link SseBroadcaster}).
	 *
	 * <p>
	 * One per session, keyed by the session's non-secret stream key, never its capability id.  Subscribing again
	 * closes the previous subscription, which ends the earlier stream quietly; {@link #connect(BusSession, FrameSink)}
	 * then closes the earlier sink with {@code 4409}.  The queue holds {@link BusPolicy#maxQueuedFrames()} {@code + 1}
	 * events; see {@code SseFrameSink}.
	 *
	 * @param session A session of this bus.
	 * @return A new subscription.  The caller closes it when its stream ends.
	 */
	SseSubscription subscribeSse(BusSession session) {
		return sseStreams.subscribe(impl(session).streamKey);
	}

	/**
	 * The session POST's {@code 200} body (spec §11.3).
	 *
	 * @param session A session of this bus.
	 * @param busBase The root-relative servlet path plus {@link BusEventsMixin#BUS_PREFIX}, e.g. {@code /rest/ops/juneau-bus}.
	 * @param contextPath The servlet context path ({@code ""} for the root context): WebSocket endpoints are
	 * 	context-relative, not servlet-relative.
	 * @return The grant.  {@code paths.websocket} is present only after {@link #enableWebSocket(String)}.
	 */
	JsonMap grant(BusSession session, String busBase, String contextPath) {
		var s = impl(session);
		var down = new ArrayList<JsonMap>();
		for (var t : s.downstream)
			down.add(JsonMap.of("topic", t, "retain", policy.downstreamRetain(t).orElse(false)));
		var paths = JsonMap.of("sse", busBase + "/stream/" + s.id);
		var ws = webSocketPrefix;
		if (ws != null)
			paths.put("websocket", (contextPath == null ? "" : contextPath) + ws + "/" + s.id);
		return JsonMap.of("v", BusFrames.VERSION, "sessionId", s.id, "heartbeatMs", policy.heartbeat().toMillis(),
			"graceMs", policy.sessionGrace().toMillis(), "downstream", down, "upstream", List.copyOf(s.upstream), "paths", paths);
	}

	/** Expires old sessions, closes connections past {@code maxSessionAge}, and pings idle connections. */
	void tick() {
		synchronized (lock) {
			if (closed)
				return;
			var now = clock.instant();
			sweep(now);
			var idle = policy.heartbeat();
			for (var s : List.copyOf(sessions.values()))
				if (s.sink != null && ! now.isBefore(s.lastSent.plus(idle)))
					s.offer(BusFrames.ping());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Internals
	//-----------------------------------------------------------------------------------------------------------------

	private void tickSafely() {
		try {
			tick();
		} catch (RuntimeException e) {
			// A scheduled task that throws is never run again; log and keep the timer alive.
			LOGGER.log(Level.WARNING, "bus timer tick failed", e);
		}
	}

	/** Returns the retain flag, or throws E-56.  A {@code :*} pattern is not a publishable topic. */
	private boolean allowed(String method, String topic) {
		if (topic == null || topic.endsWith(":*"))
			throw e56("ServerBus.%s('%s'): the policy does not allow it downstream", method, topic);
		return policy.downstreamRetain(topic)
			.orElseThrow(() -> e56("ServerBus.%s('%s'): the policy does not allow it downstream", method, topic));
	}

	/** Serializes a payload and checks the worst-case frame size (a {@code false} flag and the longest {@code seq}). */
	private String encode(String method, String topic, Object payload) {
		if (payload == null)
			throw new IllegalArgumentException(String.format("ServerBus.%s('%s'): payload must not be null; use clear(topic)", method, topic));
		var json = Json.of(payload);
		var size = BusFrames.utf8Length(BusFrames.pub(topic, json, false, Long.MAX_VALUE));
		if (size > policy.maxFrameBytes())
			throw e56("ServerBus.%s('%s'): frame too large (%s bytes, maxFrameBytes %s)", method, topic, size, policy.maxFrameBytes());
		return json;
	}

	private void reply(SessionImpl s, String frame) {
		synchronized (lock) {
			s.offer(frame);
		}
	}

	/** Guarded by lock. */
	private void sweep(Instant now) {
		for (var s : List.copyOf(sessions.values()))
			if (s.expired(now))
				expire(s);
	}

	/** Guarded by lock. */
	private void expire(SessionImpl s) {
		sessions.remove(s.id);
		s.detach(4401, "bus:unknown-session");
	}

	/** Keeps only denied topics that are safe to echo in a 403 body: inside a topic grammar and short. */
	private static List<String> echoable(List<String> denied) {
		var out = new ArrayList<String>();
		for (var x : denied)
			if (x.length() <= MAX_ECHO_LENGTH && (BusPolicy.DOWNSTREAM_PATTERN.matcher(x).matches() || BusPolicy.UPSTREAM_TOPIC.matcher(x).matches()))
				out.add(x);
		return out;
	}

	private static List<String> topics(JsonMap body, String key) throws BusRefusal {
		var o = body.get(key);
		if (o == null)
			return List.of();
		if (! (o instanceof List<?> list) || list.size() > MAX_TOPICS)
			throw BusRefusal.badRequest(String.format("%s must be a list of at most %s topics", key, MAX_TOPICS));
		var out = new LinkedHashSet<String>();
		for (var x : list) {
			if (! (x instanceof String s) || s.isEmpty())
				throw BusRefusal.badRequest(String.format("%s must be a list of at most %s topics", key, MAX_TOPICS));
			out.add(s);
		}
		return List.copyOf(out);
	}

	private static String newId() {
		var bytes = new byte[ID_BYTES];
		RANDOM.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private static SessionImpl impl(BusSession session) {
		if (session instanceof SessionImpl s)
			return s;
		throw new IllegalArgumentException("ServerBus: not a session of a ServerBus");
	}

	private static IllegalArgumentException e56(String format, Object...args) {
		return new IllegalArgumentException("E-56: " + String.format(format, args));
	}

	/** A session.  Every mutable field is guarded by the bus lock; {@code sink} is also volatile for {@link #isConnected()}. */
	private final class SessionImpl implements BusSession {

		final String id;
		final String streamKey = "bus-" + STREAM_KEYS.incrementAndGet();
		final String bridgeId;
		final String transport;
		final String principalName;
		final Map<String,Object> attributes;
		final Set<String> downstream;
		final Set<String> upstream;
		final Instant createdAt;

		volatile FrameSink sink;
		Instant disconnectedAt;
		Instant lastSent;
		long seq;
		Instant windowStart = Instant.EPOCH;
		int windowCount;

		SessionImpl(String id, String bridgeId, String transport, String principalName, Map<String,Object> attributes,
				List<String> downstream, List<String> upstream, Instant now) {
			this.id = id;
			this.bridgeId = bridgeId;
			this.transport = transport;
			this.principalName = principalName;
			this.attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
			this.downstream = Collections.unmodifiableSet(new LinkedHashSet<>(downstream));
			this.upstream = Collections.unmodifiableSet(new LinkedHashSet<>(upstream));
			this.createdAt = now;
			this.disconnectedAt = now;
			this.lastSent = now;
		}

		@Override public String bridgeId() { return bridgeId; }
		@Override public String transport() { return transport; }
		@Override public String principalName() { return principalName; }
		@Override public Object attribute(String name) { return attributes.get(name); }
		@Override public Set<String> downstream() { return downstream; }
		@Override public Set<String> upstream() { return upstream; }
		@Override public boolean isConnected() { return sink != null; }

		/** Whether the grant covers a concrete topic: an exact entry, or {@code family:*} for {@code family:key}. */
		boolean grants(String topic) {
			if (downstream.contains(topic))
				return true;
			var i = topic.indexOf(':');
			return i > 0 && downstream.contains(topic.substring(0, i) + ":*");
		}

		/** Sends a frame that carries the next {@code seq}.  Not connected: nothing happens and seq is not consumed. */
		boolean send(LongFunction<String> frame) {
			if (sink == null)
				return false;
			return offer(frame.apply(++seq));
		}

		/** Offers a frame; on a full queue detaches the sink and closes it with 4429. */
		boolean offer(String frame) {
			var k = sink;
			if (k == null)
				return false;
			if (k.offer(frame)) {
				lastSent = clock.instant();
				return true;
			}
			detach(4429, "bus:slow-consumer");
			return false;
		}

		void detach(int code, String reason) {
			var k = sink;
			if (k == null)
				return;
			sink = null;
			disconnectedAt = clock.instant();
			k.close(code, reason);
		}

		boolean expired(Instant now) {
			if (! now.isBefore(createdAt.plus(policy.maxSessionAge())))
				return true;
			return sink == null && ! now.isBefore(disconnectedAt.plus(policy.sessionGrace()));
		}

		boolean admitUpstream(Instant now, int max) {
			if (! now.isBefore(windowStart.plusSeconds(1))) {
				windowStart = now;
				windowCount = 0;
			}
			return ++windowCount <= max;
		}

		@Override /* Object */
		public String toString() {
			return String.format("BusSession[bridge=%s, transport=%s, principal=%s, connected=%s, id=<redacted>]",
				bridgeId, transport, principalName, isConnected());
		}
	}
}
