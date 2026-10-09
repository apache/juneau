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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link ServerBus} (spec §11.3, §11.4, §11.6, §12).
 *
 * <p>
 * Uses a fake clock and a recording {@link FrameSink}.  No timer thread is started: {@code tick()} runs only when a
 * test calls it, so every heartbeat and expiry assertion is deterministic.
 */
class ServerBus_Test extends TestBase {

	static final class FakeClock extends Clock {
		Instant now = Instant.parse("2026-10-01T00:00:00Z");
		void advance(Duration d) { now = now.plus(d); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}

	/** Records every accepted frame; refuses once {@code capacity} frames are held or after a close. */
	static final class RecordingSink implements FrameSink {
		final List<String> frames = new ArrayList<>();
		int capacity = Integer.MAX_VALUE;
		int closeCode;
		String closeReason;

		@Override public boolean offer(String frame) {
			if (closeCode != 0 || frames.size() >= capacity)
				return false;
			frames.add(frame);
			return true;
		}

		@Override public void close(int code, String reason) {
			closeCode = code;
			closeReason = reason;
		}

		List<JsonMap> decoded() { return frames.stream().map(RecordingSink::parse).toList(); }
		List<String> types() { return decoded().stream().map(f -> (String) f.get("type")).toList(); }
		JsonMap last() { return parse(frames.get(frames.size() - 1)); }
		void reset() { frames.clear(); }

		static JsonMap parse(String frame) {
			try {
				return BusFrames.decode(frame);
			} catch (BusRefusal e) {
				throw new AssertionError(frame, e);
			}
		}
	}

	private static final Principal JB = () -> "jb";
	private static final Principal EVE = () -> "eve";

	private final FakeClock clock = new FakeClock();
	private final List<String> handled = new ArrayList<>();

	private BusPolicy.Builder policy() {
		return BusPolicy.create()
			.downstream("ops.jobs", true)
			.downstream("ops.alert:*", false)
			.downstream("ops.host:*", true)
			.downstream("cmd:jobs", false)
			.upstream("ops.cancel-all", (s, t, p) -> handled.add(t + " " + p.get("reason")))
			.upstream("ops.explode", (s, t, p) -> { throw new IllegalStateException("secret detail"); });
	}

	/** A bus with the WebSocket transport enabled and no timer. */
	private ServerBus bus(BusPolicy p) {
		var b = new ServerBus(p, clock, null);
		b.enableWebSocket("/juneau-bus/ws");
		return b;
	}

	/** A request that knows only its principal and the roles it is in; every other method returns null, 0 or false. */
	private static HttpServletRequest req(String user, String...roles) {
		var roleSet = Set.of(roles);
		Principal principal = user == null ? null : () -> user;
		return (HttpServletRequest)Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
			(proxy, method, args) -> switch (method.getName()) {
				case "getUserPrincipal" -> principal;
				case "isUserInRole" -> roleSet.contains(args[0]);
				case "toString" -> "FakeRequest[" + user + "]";
				case "hashCode" -> System.identityHashCode(proxy);
				case "equals" -> proxy == args[0];
				default -> method.getReturnType() == boolean.class ? (Object)false : method.getReturnType() == int.class ? (Object)0 : null;
			});
	}

	private static BusSession open(ServerBus bus, String user, String transport, List<String> down, List<String> up) throws BusRefusal {
		return bus.openSession(req(user), JsonMap.of("v", 1, "bridge", "ops", "transport", transport, "downstream", down, "upstream", up));
	}

	private static BusSession openSse(ServerBus bus, String...down) throws BusRefusal {
		return open(bus, "jb", "sse", List.of(down), List.of());
	}

	private static String up(String topic, String payloadJson) {
		return "{\"v\":1,\"type\":\"pub\",\"topic\":\"" + topic + "\",\"payload\":" + payloadJson + "}";
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) publish, retained values, distinct-until-changed, clear, E-56
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_retainedPublishIsDistinctUntilChanged() {
		var bus = bus(policy().build());
		assertTrue(bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", 2)));
		assertEquals(Optional.of(Json.of(JsonMap.of("schemaVersion", 1, "running", 2))), bus.retainedJson("ops.jobs"));
		assertFalse(bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", 2)), "unchanged: not republished");
		assertTrue(bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1, "running", 3)));
		assertEquals(Optional.of(Json.of(JsonMap.of("schemaVersion", 1, "running", 3))), bus.retainedJson("ops.jobs"));
		assertTrue(bus.publish("ops.host:a", JsonMap.of("up", true)), "a key under a retained pattern is retained");
		assertTrue(bus.retainedJson("ops.host:a").isPresent());
	}

	@Test void a02_eventTopicsAreNeverRetained() {
		var bus = bus(policy().build());
		assertTrue(bus.publish("ops.alert:east", JsonMap.of("msg", "disk")));
		assertTrue(bus.publish("ops.alert:east", JsonMap.of("msg", "disk")), "events are not distinct-until-changed");
		assertTrue(bus.retainedJson("ops.alert:east").isEmpty());
		assertTrue(bus.retainedJson("nope.nope").isEmpty());
	}

	@Test void a03_e56WhenThePolicyDeniesTheTopic() {
		var bus = bus(policy().build());
		for (var t : Arrays.asList("ops.secret", "ops.alert:*", "job:1", "ops.jobs:x", null)) {
			var e = assertThrows(IllegalArgumentException.class, () -> bus.publish(t, JsonMap.of()), String.valueOf(t));
			assertEquals("E-56: ServerBus.publish('" + t + "'): the policy does not allow it downstream", e.getMessage());
		}
		var e = assertThrows(IllegalArgumentException.class, () -> bus.publishTo(s -> true, "ops.secret", JsonMap.of()));
		assertEquals("E-56: ServerBus.publishTo('ops.secret'): the policy does not allow it downstream", e.getMessage());
	}

	@Test void a04_e56WhenTheFrameIsTooLarge() {
		var bus = bus(policy().maxFrameBytes(256).build());
		var e = assertThrows(IllegalArgumentException.class, () -> bus.publish("ops.jobs", JsonMap.of("blob", "x".repeat(300))));
		assertTrue(e.getMessage().startsWith("E-56: ServerBus.publish('ops.jobs'): frame too large ("), e.getMessage());
		assertTrue(bus.retainedJson("ops.jobs").isEmpty(), "a refused publish stores nothing");
		assertTrue(bus.publish("ops.jobs", JsonMap.of("blob", "x".repeat(10))));
	}

	@Test void a05_nullPayloadIsRefused() {
		var bus = bus(policy().build());
		var e = assertThrows(IllegalArgumentException.class, () -> bus.publish("ops.jobs", null));
		assertEquals("ServerBus.publish('ops.jobs'): payload must not be null; use clear(topic)", e.getMessage());
	}

	@Test void a06_clearRemovesTheValueAndTellsGrantedSessions() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var sink = new RecordingSink();
		bus.connect(s, sink);
		bus.publish("ops.jobs", JsonMap.of("n", 1));
		sink.reset();

		bus.clear("ops.jobs");
		assertTrue(bus.retainedJson("ops.jobs").isEmpty());
		assertEquals(List.of("clear"), sink.types());
		assertEquals("ops.jobs", sink.last().get("topic"));

		bus.clear("ops.jobs");
		assertEquals(1, sink.frames.size(), "clearing an absent value sends nothing");

		var e = assertThrows(IllegalArgumentException.class, () -> bus.clear("ops.alert:east"));
		assertEquals("E-56: ServerBus.clear('ops.alert:east'): the policy does not retain it", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> bus.clear("ops.secret"));
		assertEquals("E-56: ServerBus.clear('ops.secret'): the policy does not allow it downstream", e.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) openSession: the grant, its defaults, and every refusal
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_openSessionRecordsTheGrant() throws Exception {
		var bus = bus(policy().sessionAttributes(r -> Map.of("region", "east")).build());
		var s = open(bus, "jb", "sse", List.of("ops.jobs", "ops.alert:*", "ops.jobs"), List.of());
		assertEquals("ops", s.bridgeId());
		assertEquals("sse", s.transport());
		assertEquals("jb", s.principalName());
		assertEquals("east", s.attribute("region"));
		assertNull(s.attribute("nope"));
		assertEquals(List.of("ops.jobs", "ops.alert:*"), List.copyOf(s.downstream()), "deduplicated, in request order");
		assertTrue(s.upstream().isEmpty());
		assertFalse(s.isConnected());

		var id = ServerBus.idOf(s);
		assertTrue(id.matches("[0-9a-f]{64}"), id);
		assertFalse(s.toString().contains(id), "toString redacts the capability");
		assertTrue(s.toString().contains("id=<redacted>"), s.toString());
		assertNotEquals(id, ServerBus.idOf(openSse(bus, "ops.jobs")));
		assertEquals(2, bus.sessions().size());
	}

	@Test void b02_versionAndTransportDefault() throws Exception {
		var bus = bus(policy().build());
		var s = bus.openSession(req(null), JsonMap.of("bridge", "ops", "downstream", List.of("ops.jobs")));
		assertEquals("sse", s.transport());
		assertNull(s.principalName());
	}

	@Test void b03_malformedBodiesAre400() {
		var bus = bus(policy().build());
		var bodies = List.of(
			JsonMap.of("v", 2, "bridge", "ops", "downstream", List.of("ops.jobs")),
			JsonMap.of("downstream", List.of("ops.jobs")),
			JsonMap.of("bridge", "9ops", "downstream", List.of("ops.jobs")),
			JsonMap.of("bridge", "ops", "transport", "carrier-pigeon", "downstream", List.of("ops.jobs")),
			JsonMap.of("bridge", "ops", "downstream", "ops.jobs"),
			JsonMap.of("bridge", "ops", "downstream", List.of(1)),
			JsonMap.of("bridge", "ops"),
			JsonMap.of("bridge", "ops", "transport", "sse", "downstream", List.of("ops.jobs"), "upstream", List.of("ops.cancel-all")));
		for (var b : bodies) {
			var e = assertThrows(BusRefusal.class, () -> bus.openSession(req("jb"), b), b.toString());
			assertEquals(400, e.status(), b.toString());
			assertEquals("bus:bad-request", e.code(), b.toString());
		}
		assertEquals(400, assertThrows(BusRefusal.class, () -> bus.openSession(req("jb"), null)).status());
		assertTrue(bus.sessions().isEmpty());
	}

	@Test void b04_policyAndAuthorizerDenialsAre403WithTheDeniedList() {
		var bus = bus(policy().authorizer((r, t) -> ! t.startsWith("cmd:")).build());
		var e = assertThrows(BusRefusal.class, () -> open(bus, "jb", "websocket",
			List.of("ops.jobs", "ops.secret", "cmd:jobs"), List.of("ops.cancel-all", "ops.nope")));
		assertEquals(403, e.status());
		assertEquals("bus:topic-denied", e.code());
		assertEquals(List.of("ops.secret", "cmd:jobs", "ops.nope"), e.denied());
		assertEquals(JsonMap.of("code", "bus:topic-denied", "message", "the bus policy denies a requested topic",
			"denied", List.of("ops.secret", "cmd:jobs", "ops.nope")), e.toJson());
		assertTrue(bus.sessions().isEmpty(), "the whole request is refused");
	}

	@Test void b05_theAuthorizerSeesTheRequest() throws Exception {
		var bus = bus(policy().authorizer((r, t) -> r.isUserInRole("ops")).build());
		var ops = req("jb", "ops");
		assertNotNull(bus.openSession(ops, JsonMap.of("bridge", "ops", "downstream", List.of("ops.jobs"))));
		var e = assertThrows(BusRefusal.class, () -> openSse(bus, "ops.jobs"));
		assertEquals(403, e.status());
	}

	@Test void b06_websocketWithoutRegistrationIs501() {
		var bus = new ServerBus(policy().build(), clock, null);
		var e = assertThrows(BusRefusal.class, () -> open(bus, "jb", "websocket", List.of("ops.jobs"), List.of()));
		assertEquals(501, e.status());
		assertEquals("bus:transport-unavailable", e.code());
		bus.enableWebSocket("/juneau-bus/ws");
		assertDoesNotThrow(() -> open(bus, "jb", "websocket", List.of("ops.jobs"), List.of()));
	}

	@Test void b07_atMaxSessionsIs429WithRetryAfter() throws Exception {
		var bus = bus(policy().maxSessions(2).build());
		openSse(bus, "ops.jobs");
		openSse(bus, "ops.jobs");
		var e = assertThrows(BusRefusal.class, () -> openSse(bus, "ops.jobs"));
		assertEquals(429, e.status());
		assertEquals("bus:too-many-sessions", e.code());
		assertEquals(OptionalInt.of(5), e.retryAfterSeconds());
		clock.advance(Duration.ofSeconds(60));
		assertDoesNotThrow(() -> openSse(bus, "ops.jobs"), "never-connected sessions expire after the grace period");
	}

	@Test void b08_enableWebSocketValidatesThePrefix() {
		var bus = new ServerBus(policy().build(), clock, null);
		for (var bad : Arrays.asList("juneau-bus/ws", "/juneau-bus/ws/", null))
			assertThrows(IllegalArgumentException.class, () -> bus.enableWebSocket(bad), String.valueOf(bad));
		assertNull(bus.webSocketPrefix());
		bus.enableWebSocket("/juneau-bus/ws");
		assertEquals("/juneau-bus/ws", bus.webSocketPrefix());
	}

	@Test void b09_grantDescribesTheSession() throws Exception {
		var bus = bus(policy().build());
		var s = open(bus, "jb", "websocket", List.of("ops.jobs", "ops.alert:*"), List.of("ops.cancel-all"));
		var id = ServerBus.idOf(s);
		assertEquals(JsonMap.of("v", 1, "sessionId", id, "heartbeatMs", 15000L, "graceMs", 60000L,
			"downstream", List.of(JsonMap.of("topic", "ops.jobs", "retain", true), JsonMap.of("topic", "ops.alert:*", "retain", false)),
			"upstream", List.of("ops.cancel-all"),
			"paths", JsonMap.of("sse", "/rest/ops/juneau-bus/stream/" + id, "websocket", "/app/juneau-bus/ws/" + id)),
			bus.grant(s, "/rest/ops/juneau-bus", "/app"));

		var plain = new ServerBus(policy().build(), clock, null);
		var p = plain.grant(openSse(plain, "ops.jobs"), "/juneau-bus", "");
		assertFalse(((Map<?,?>) p.get("paths")).containsKey("websocket"), "no websocket path until enableWebSocket");
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) session(id, principal): unknown, mismatch and expired are all empty
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_lookupMatchesIdAndPrincipal() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var id = ServerBus.idOf(s);
		assertSame(s, bus.session(id, JB).orElseThrow());
		assertTrue(bus.session(id, EVE).isEmpty(), "a different principal");
		assertTrue(bus.session(id, null).isEmpty(), "an anonymous request for a named session");
		assertTrue(bus.session("0".repeat(64), JB).isEmpty(), "unknown");
		assertTrue(bus.session(null, JB).isEmpty());

		var anon = open(bus, null, "sse", List.of("ops.jobs"), List.of());
		assertSame(anon, bus.session(ServerBus.idOf(anon), null).orElseThrow());
		assertTrue(bus.session(ServerBus.idOf(anon), JB).isEmpty());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) connect: resync, seq, live delivery, replace, slow consumer, publishTo, close
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_connectSendsTheResyncThenLiveFrames() throws Exception {
		var bus = bus(policy().build());
		bus.publish("ops.jobs", JsonMap.of("n", 1));
		bus.publish("ops.host:b", JsonMap.of("up", true));
		bus.publish("ops.host:a", JsonMap.of("up", false));
		var s = open(bus, "jb", "sse", List.of("ops.jobs", "ops.host:*", "ops.alert:*"), List.of());
		var sink = new RecordingSink();

		bus.connect(s, sink);
		assertTrue(s.isConnected());
		assertEquals(List.of("resync-begin", "pub", "pub", "pub", "resync-end"), sink.types());
		var pubs = sink.decoded().subList(1, 4);
		assertEquals(List.of("ops.host:a", "ops.host:b", "ops.jobs"), pubs.stream().map(f -> f.get("topic")).toList());
		pubs.forEach(f -> assertEquals(true, f.get("retained")));
		assertEquals(JsonMap.of("n", 1), pubs.get(2).get("payload"));
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), sink.frames.stream().map(f -> BusFrames.seq(f).getAsLong()).toList());

		bus.publish("ops.alert:east", JsonMap.of("msg", "disk"));
		bus.publish("cmd:jobs", JsonMap.of("schemaVersion", 1, "op", "reload"));
		assertEquals(6, sink.frames.size(), "cmd:jobs is not granted to this session");
		assertEquals("ops.alert:east", sink.last().get("topic"));
		assertEquals(false, sink.last().get("retained"));
		assertEquals(OptionalLong.of(6), BusFrames.seq(sink.frames.get(5)));
	}

	@Test void d02_reconnectResyncsChangedStateAndDropsMissedEvents() throws Exception {
		var bus = bus(policy().build());
		bus.publish("ops.jobs", JsonMap.of("n", 1));
		var s = openSse(bus, "ops.jobs", "ops.alert:*");
		var first = new RecordingSink();
		bus.connect(s, first);                                   // seq 1, 2, 3
		bus.disconnect(s, first);
		assertFalse(s.isConnected());

		bus.publish("ops.jobs", JsonMap.of("n", 2));
		bus.publish("ops.alert:east", JsonMap.of("msg", "missed"));

		var second = new RecordingSink();
		bus.connect(s, second);
		assertEquals(List.of("resync-begin", "pub", "resync-end"), second.types());
		assertEquals(JsonMap.of("n", 2), second.decoded().get(1).get("payload"));
		assertEquals(OptionalLong.of(4), BusFrames.seq(second.frames.get(0)), "seq is per session and keeps counting");
	}

	@Test void d03_aSecondConnectionReplacesTheFirstWith4409() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var first = new RecordingSink();
		var second = new RecordingSink();
		bus.connect(s, first);
		bus.connect(s, second);
		assertEquals(4409, first.closeCode);
		assertEquals("bus:replaced", first.closeReason);

		bus.disconnect(s, first);                                // the replaced transport reports its close late
		assertTrue(s.isConnected(), "a stale disconnect must not detach the live sink");
		bus.publish("ops.jobs", JsonMap.of("n", 1));
		assertEquals("pub", second.last().get("type"));
	}

	@Test void d04_aFullQueueClosesWith4429() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.alert:*");
		var sink = new RecordingSink();
		sink.capacity = 3;
		bus.connect(s, sink);                                    // resync-begin, resync-end
		bus.publish("ops.alert:east", JsonMap.of("n", 1));     // the third frame fits
		assertEquals(0, sink.closeCode);
		bus.publish("ops.alert:east", JsonMap.of("n", 2));     // the fourth does not
		assertEquals(4429, sink.closeCode);
		assertEquals("bus:slow-consumer", sink.closeReason);
		assertFalse(s.isConnected());
		assertTrue(bus.session(ServerBus.idOf(s), JB).isPresent(), "the session survives its grace period; the resync repairs state");
	}

	@Test void d05_publishToReachesOnlyTheAudienceAndIsNeverRetained() throws Exception {
		var bus = bus(policy().build());
		var mine = openSse(bus, "ops.jobs");
		var theirs = open(bus, "eve", "sse", List.of("ops.jobs"), List.of());
		var idle = openSse(bus, "ops.jobs");                     // jb, but not connected
		var mineSink = new RecordingSink();
		var theirSink = new RecordingSink();
		bus.connect(mine, mineSink);
		bus.connect(theirs, theirSink);
		theirSink.reset();

		assertEquals(1, bus.publishTo(x -> "jb".equals(x.principalName()), "ops.jobs", JsonMap.of("mine", true)));
		assertEquals("pub", mineSink.last().get("type"));
		assertEquals(false, mineSink.last().get("retained"));
		assertTrue(theirSink.frames.isEmpty());
		assertTrue(bus.retainedJson("ops.jobs").isEmpty(), "publishTo never retains");
		assertFalse(idle.isConnected());
	}

	@Test void d06_closeEndsEveryConnectionWith1001() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var sink = new RecordingSink();
		bus.connect(s, sink);

		bus.close();
		assertEquals(1001, sink.closeCode);
		assertEquals("bus:closed", sink.closeReason);
		assertTrue(bus.sessions().isEmpty());
		assertEquals(503, assertThrows(BusRefusal.class, () -> openSse(bus, "ops.jobs")).status());

		var late = new RecordingSink();
		bus.connect(s, late);
		assertEquals(1001, late.closeCode, "connecting to a closed bus closes at once");
		assertDoesNotThrow(bus::close, "close is idempotent");
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Lifetime: grace, max age, heartbeat
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_aDisconnectedSessionSurvivesExactlyTheGracePeriod() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var id = ServerBus.idOf(s);
		var sink = new RecordingSink();
		bus.connect(s, sink);
		clock.advance(Duration.ofHours(1));
		assertTrue(bus.session(id, JB).isPresent(), "a connected session does not expire by grace");

		bus.disconnect(s, sink);
		clock.advance(Duration.ofSeconds(59));
		assertTrue(bus.session(id, JB).isPresent());
		clock.advance(Duration.ofSeconds(1));
		assertTrue(bus.session(id, JB).isEmpty());
		assertTrue(bus.sessions().isEmpty());
	}

	@Test void e02_aNeverConnectedSessionExpiresAfterTheGracePeriod() throws Exception {
		var bus = bus(policy().build());
		var id = ServerBus.idOf(openSse(bus, "ops.jobs"));
		clock.advance(Duration.ofSeconds(60));
		assertTrue(bus.session(id, JB).isEmpty());
	}

	@Test void e03_maxAgeClosesALiveConnectionWith4401() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var sink = new RecordingSink();
		bus.connect(s, sink);
		clock.advance(Duration.ofHours(12).minusMinutes(1));
		bus.tick();
		assertEquals(0, sink.closeCode);
		clock.advance(Duration.ofMinutes(1));
		bus.tick();
		assertEquals(4401, sink.closeCode);
		assertEquals("bus:unknown-session", sink.closeReason);
		assertTrue(bus.session(ServerBus.idOf(s), JB).isEmpty());
	}

	@Test void e04_pingAfterHeartbeatOfIdleness() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var sink = new RecordingSink();
		bus.connect(s, sink);
		sink.reset();

		clock.advance(Duration.ofSeconds(14));
		bus.tick();
		assertTrue(sink.frames.isEmpty());
		clock.advance(Duration.ofSeconds(1));
		bus.tick();
		assertEquals(List.of("ping"), sink.types());
		bus.tick();
		assertEquals(1, sink.frames.size(), "the idle timer restarts after every frame");

		clock.advance(Duration.ofSeconds(10));
		bus.publish("ops.jobs", JsonMap.of("n", 1));
		clock.advance(Duration.ofSeconds(10));
		bus.tick();
		assertEquals(List.of("ping", "pub"), sink.types(), "a live frame resets the idle timer");
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Upstream: handler, deny, failure, rate limit, bad frames
	//------------------------------------------------------------------------------------------------------------------

	private RecordingSink connectWs(ServerBus bus, List<String> upstream) throws BusRefusal {
		var s = open(bus, "jb", "websocket", List.of("ops.jobs"), upstream);
		var sink = new RecordingSink();
		bus.connect(s, sink);
		sink.reset();
		return sink;
	}

	private static BusSession only(ServerBus bus) {
		return bus.sessions().get(0);
	}

	@Test void f01_anUpstreamFrameReachesItsHandler() throws Exception {
		var bus = bus(policy().build());
		var sink = connectWs(bus, List.of("ops.cancel-all"));
		bus.receive(only(bus), up("ops.cancel-all", "{\"reason\":\"maintenance\"}"));
		assertEquals(List.of("ops.cancel-all maintenance"), handled);
		assertTrue(sink.frames.isEmpty(), "success sends nothing back");
	}

	@Test void f02_anUngrantedTopicIsDenied() throws Exception {
		var bus = bus(policy().build());
		var sink = connectWs(bus, List.of("ops.cancel-all"));
		bus.receive(only(bus), up("ops.explode", "{}"));
		bus.receive(only(bus), up("ops.other", "{}"));
		bus.receive(only(bus), up("Not A Topic", "{}"));
		assertTrue(handled.isEmpty());
		assertEquals(List.of("error", "error", "error"), sink.types());
		var f = sink.decoded();
		assertEquals("bus:upstream-denied", f.get(0).get("code"));
		assertEquals("ops.explode", f.get(0).get("topic"));
		assertEquals("ops.other", f.get(1).get("topic"));
		assertNull(f.get(2).get("topic"), "a topic outside the grammar is not echoed");
		assertEquals(0, sink.closeCode, "the connection stays open");
	}

	@Test void f03_aHandlerFailureIsGeneric() throws Exception {
		var bus = bus(policy().build());
		var sink = connectWs(bus, List.of("ops.explode"));
		var records = LogRecordCapture.quietly(ServerBus.class, () -> bus.receive(only(bus), up("ops.explode", "{}")));
		assertEquals(1, records.size(), records::toString);
		assertEquals(JsonMap.of("v", 1, "type", "error", "code", "bus:upstream-failed", "message", "handler failed",
			"topic", "ops.explode"), sink.last());
		assertFalse(sink.frames.get(0).contains("secret detail"), "no exception text crosses the wire");
	}

	@Test void f04_upstreamIsRateLimitedPerSecond() throws Exception {
		var bus = bus(policy().maxUpstreamPerSecond(3).build());
		var sink = connectWs(bus, List.of("ops.cancel-all"));
		for (var i = 0; i < 4; i++)
			bus.receive(only(bus), up("ops.cancel-all", "{\"reason\":\"r" + i + "\"}"));
		assertEquals(3, handled.size());
		assertEquals("bus:rate-limited", sink.last().get("code"));
		clock.advance(Duration.ofSeconds(1));
		bus.receive(only(bus), up("ops.cancel-all", "{\"reason\":\"later\"}"));
		assertEquals(4, handled.size());
	}

	@Test void f05_badFramesAreAnsweredAndDropped() throws Exception {
		var bus = bus(policy().build());
		var sink = connectWs(bus, List.of("ops.cancel-all"));
		bus.receive(only(bus), "not json");
		bus.receive(only(bus), "{\"v\":1,\"type\":\"ping\"}");
		bus.receive(only(bus), up("ops.cancel-all", "[1]"));
		bus.receive(only(bus), up("ops.cancel-all", "\"" + "x".repeat(70_000) + "\""));
		assertTrue(handled.isEmpty());
		var f = sink.decoded();
		assertEquals(4, f.size());
		f.forEach(x -> assertEquals("bus:bad-frame", x.get("code")));
		assertEquals("frame too large", f.get(3).get("message"));
		assertEquals(0, sink.closeCode);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) the SSE subscription behind BusEventsMixin's stream (P1: SseBroadcaster)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_oneSseSubscriptionPerSession() throws Exception {
		var bus = bus(policy().build());
		var a = openSse(bus, "ops.jobs");
		var b = openSse(bus, "ops.jobs");
		var first = bus.subscribeSse(a);
		var other = bus.subscribeSse(b);
		assertNotEquals(first.getId(), other.getId(), "one key per session");
		assertFalse(first.getId().contains(ServerBus.idOf(a)), "the stream key is not the capability");
		var second = bus.subscribeSse(a);
		assertTrue(first.isClosed(), "a second stream replaces the first");
		assertFalse(second.isClosed(), "the replacement stays open");
		assertEquals(first.getId(), second.getId());
		assertFalse(other.isClosed(), "another session is unaffected");
		second.close();
		other.close();
	}

	//------------------------------------------------------------------------------------------------------------------
	// h) expiry seen by connect and receive without tick(), unknown sessions, size limits, hostile topics, concurrency
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_connectAfterGraceClosesWith4401WithoutATick() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		clock.advance(Duration.ofSeconds(60));
		var sink = new RecordingSink();
		bus.connect(s, sink);
		assertEquals(4401, sink.closeCode);
		assertEquals("bus:unknown-session", sink.closeReason);
		assertTrue(sink.frames.isEmpty(), "no resync for an expired session");
		assertTrue(bus.sessions().isEmpty());
	}

	@Test void h02_connectAfterMaxAgeClosesBothSinksWith4401WithoutATick() throws Exception {
		var bus = bus(policy().build());
		var s = openSse(bus, "ops.jobs");
		var first = new RecordingSink();
		bus.connect(s, first);
		clock.advance(Duration.ofHours(12));
		var second = new RecordingSink();
		bus.connect(s, second);
		assertEquals(4401, first.closeCode, "the live sink of an expired session is closed");
		assertEquals(4401, second.closeCode);
		assertTrue(second.frames.isEmpty());
		assertFalse(s.isConnected());
		assertTrue(bus.sessions().isEmpty());
	}

	@Test void h03_receiveAfterMaxAgeExpiresTheSessionAndRunsNothing() throws Exception {
		var bus = bus(policy().build());
		var sink = connectWs(bus, List.of("ops.cancel-all"));
		var s = only(bus);
		clock.advance(Duration.ofHours(12));
		bus.receive(s, up("ops.cancel-all", "{\"reason\":\"late\"}"));
		assertTrue(handled.isEmpty());
		assertTrue(sink.frames.isEmpty(), "silent");
		assertEquals(4401, sink.closeCode);
		assertTrue(bus.sessions().isEmpty());
	}

	@Test void h04_connectOnAnUnknownSessionIs4401() throws Exception {
		var other = bus(policy().build());
		var foreign = openSse(other, "ops.jobs");
		var bus = bus(policy().build());
		var sink = new RecordingSink();
		bus.connect(foreign, sink);
		assertEquals(4401, sink.closeCode);
		assertTrue(sink.frames.isEmpty());
	}

	@Test void h05_frameSizeLimitIsInclusive() throws Exception {
		var worst = BusFrames.utf8Length(BusFrames.pub("ops.jobs", Json.of("x".repeat(10)), false, Long.MAX_VALUE));
		var bus = bus(policy().maxFrameBytes(worst).build());
		assertTrue(bus.publish("ops.jobs", "x".repeat(10)), "exactly maxFrameBytes is accepted");
		var e = assertThrows(IllegalArgumentException.class, () -> bus.publish("ops.jobs", "x".repeat(11)));
		assertTrue(e.getMessage().contains("frame too large"), e.getMessage());

		var frame = up("ops.cancel-all", "{\"reason\":\"" + "y".repeat(20) + "\"}");
		var ws = bus(policy().maxFrameBytes(BusFrames.utf8Length(frame)).build());
		var sink = connectWs(ws, List.of("ops.cancel-all"));
		ws.receive(only(ws), frame);
		assertEquals(1, handled.size(), "exactly maxFrameBytes is accepted");
		ws.receive(only(ws), frame + " ");
		assertEquals(1, handled.size());
		assertEquals("frame too large", sink.last().get("message"));
	}

	@Test void h06_hostileDeniedTopicsAreNotEchoed() {
		var bus = bus(policy().build());
		var hostile = "ops.secret" + "x".repeat(500);
		var longKey = "ops.secret:" + "k".repeat(120);   // well-formed, but 131 characters
		var shortKey = "ops.secret:" + "k".repeat(100);  // well-formed, 111 characters
		var ctl = "ops.\u0001\u001b[31m<script>";
		var e = assertThrows(BusRefusal.class, () -> open(bus, "jb", "sse", List.of("ops.jobs", hostile, ctl, "ops.secret", "a".repeat(129), longKey, shortKey), List.of()));
		assertEquals(403, e.status());
		assertEquals(List.of("ops.secret", shortKey), e.denied(), "only short, well-formed topics are echoed");
		var body = e.toJson().toString();
		assertFalse(body.contains("xxxxxxxx"), body);
		assertFalse(body.contains("script"), body);
		assertFalse(body.contains("\u0001"), body);

		var none = assertThrows(BusRefusal.class, () -> open(bus, "jb", "sse", List.of(ctl), List.of()));
		assertEquals(403, none.status(), "still 403 when nothing is safe to echo");
		assertEquals(List.of(), none.denied());
	}

	/** Thread-safe sink that never refuses. */
	static final class SafeSink implements FrameSink {
		final List<String> frames = Collections.synchronizedList(new ArrayList<>());
		@Override public boolean offer(String frame) { return frames.add(frame); }
		@Override public void close(int code, String reason) { /* nothing to do */ }
	}

	@Test void h07_concurrentPublishConnectDisconnectKeepsSeqAndResyncOrder() throws Exception {
		var bus = bus(policy().maxQueuedFrames(1_000_000).build());
		var stop = new AtomicBoolean();
		var errors = new ConcurrentLinkedQueue<Throwable>();
		var sinks = new ConcurrentLinkedQueue<SafeSink>();
		var threads = new ArrayList<Thread>();
		for (var t = 0; t < 4; t++) {
			var id = t;
			threads.add(new Thread(() -> {
				try {
					var n = 0;
					while (! stop.get()) {
						bus.publish("ops.alert:t" + id, JsonMap.of("n", n));
						bus.publish("ops.jobs", JsonMap.of("n", id * 1_000_000 + n++));
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			}));
		}
		for (var t = 0; t < 3; t++)
			threads.add(new Thread(() -> {
				try {
					var s = openSse(bus, "ops.jobs", "ops.alert:*");
					while (! stop.get()) {
						var sink = new SafeSink();
						sinks.add(sink);
						bus.connect(s, sink);
						Thread.yield();
						bus.disconnect(s, sink);
					}
				} catch (Throwable e) {
					errors.add(e);
				}
			}));
		threads.forEach(Thread::start);
		Thread.sleep(200);
		stop.set(true);
		for (var t : threads) {
			t.join(10_000);
			assertFalse(t.isAlive(), "worker finished");
		}
		assertTrue(errors.isEmpty(), errors.toString());
		assertFalse(sinks.isEmpty());
		for (var sink : sinks) {
			var last = 0L;
			var inResync = false;
			for (var f : List.copyOf(sink.frames)) {
				var seq = BusFrames.seq(f).orElse(-1);
				if (seq >= 0) {
					assertTrue(seq > last, "seq strictly increases: " + last + " then " + seq);
					last = seq;
				}
				var type = RecordingSink.parse(f);
				if ("resync-begin".equals(type.get("type")))
					inResync = true;
				else if ("resync-end".equals(type.get("type")))
					inResync = false;
				else if (inResync)
					assertEquals(true, type.get("retained"), "only retained values inside a resync: " + f);
			}
		}
	}
}
