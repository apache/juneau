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
package org.apache.juneau.rest.server.terminal.bus;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.LogRecord;

import org.apache.juneau.*;
import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.terminal.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link TerminalInput}: validation, the two access checks, the {@code term.input-status} answers and the
 * audit records.
 */
class TerminalInput_Test extends TestBase {

	private static final String AUDIT = "juneau.terminal.input";

	/** Records every accepted frame (copied from {@code ServerBus_Test}, which is test-private). */
	static final class RecordingSink implements FrameSink {
		final List<String> frames = new ArrayList<>();
		int closeCode;

		@Override public boolean offer(String frame) {
			if (closeCode != 0)
				return false;
			frames.add(frame);
			return true;
		}

		@Override public void close(int code, String reason) {
			closeCode = code;
		}

		List<JsonMap> decoded() { return frames.stream().map(RecordingSink::parse).toList(); }
		JsonMap last() { return parse(frames.get(frames.size() - 1)); }
		void reset() { frames.clear(); }

		/** The payloads of the {@code term.input-status} frames received. */
		List<JsonMap> status() {
			return decoded().stream()
				.filter(f -> TerminalInput.STATUS_TOPIC.equals(f.get("topic")))
				.map(f -> (JsonMap) f.get("payload"))
				.toList();
		}

		static JsonMap parse(String frame) {
			try {
				return BusFrames.decode(frame);
			} catch (BusRefusal e) {
				throw new AssertionError(frame, e);
			}
		}
	}

	/** A sink that records writes. */
	static final class FakeSink implements TerminalSink {
		final List<byte[]> writes = new ArrayList<>();
		boolean accepts = true, writeResult = true, throwOnWrite, closeOnWrite;

		@Override public boolean acceptsInput() { return accepts; }

		@Override public boolean write(byte[] bytes) throws IOException {
			if (throwOnWrite)
				throw new IOException("disk on fire");
			if (closeOnWrite)
				accepts = false;
			if (writeResult)
				writes.add(bytes.clone());
			return writeResult;
		}

		List<String> text() { return writes.stream().map(b -> new String(b, UTF_8)).toList(); }
	}

	/** Principal names (anonymous is {@code ""}) that may watch. */
	private final Set<String> watchers = new HashSet<>(Set.of("alice", "bob", ""));
	/** Principal names (anonymous is {@code ""}) to the sink they may type into. */
	private final Map<String,TerminalSink> typists = new HashMap<>();
	/** Every caller the access stub saw. */
	private final List<TerminalCaller> callers = new ArrayList<>();

	private final TerminalAccess access = new TerminalAccess() {
		@Override public boolean canWatch(TerminalCaller c, String id) {
			callers.add(c);
			return watchers.contains(key(c));
		}
		@Override public Optional<TerminalSink> inputSink(TerminalCaller c, String id) {
			callers.add(c);
			return Optional.ofNullable(typists.get(key(c)));
		}
	};

	private final FakeSink fake = new FakeSink();
	private final Logger auditLogger = Logger.getLogger(AUDIT);
	private Level savedLevel;

	private TerminalInput input;
	private ServerBus bus;
	private BusSession alice, bob;
	private RecordingSink aliceWs, bobWs;

	private static String key(TerminalCaller c) {
		return c.principalName() == null ? "" : c.principalName();
	}

	@BeforeEach void setUp() throws Exception {
		savedLevel = auditLogger.getLevel();
		auditLogger.setLevel(Level.ALL);
		typists.put("alice", fake);
		input = TerminalInput.create(access);
		bus = bus(policy().build());
		input.bind(bus);
		alice = open(bus, "alice");
		aliceWs = connect(bus, alice);
		bob = open(bus, "bob");
		bobWs = connect(bus, bob);
	}

	@AfterEach void tearDown() {
		bus.close();
		auditLogger.setLevel(savedLevel);
	}

	private BusPolicy.Builder policy() {
		return BusPolicy.create().upstream(TerminalInput.TOPIC, input).downstream(TerminalInput.STATUS_TOPIC, false);
	}

	/** A bus with the WebSocket transport enabled (copied from {@code ServerBus_Test}). */
	private static ServerBus bus(BusPolicy p) {
		var b = ServerBus.create(p);
		b.enableWebSocket("/juneau-bus/ws");
		return b;
	}

	/** A request that knows only its principal (copied from {@code ServerBus_Test}). */
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

	private static BusSession open(ServerBus bus, String user) throws BusRefusal {
		return bus.openSession(req(user), JsonMap.of("v", 1, "bridge", "term", "transport", "websocket",
			"downstream", List.of(TerminalInput.STATUS_TOPIC), "upstream", List.of(TerminalInput.TOPIC)));
	}

	private static RecordingSink connect(ServerBus bus, BusSession s) {
		var sink = new RecordingSink();
		bus.connect(s, sink);
		sink.reset();
		return sink;
	}

	/** An upstream {@code pub} frame (after {@code ServerBus_Test.up}). */
	private static String up(String topic, String payloadJson) {
		return "{\"v\":1,\"type\":\"pub\",\"topic\":\"" + topic + "\",\"payload\":" + payloadJson + "}";
	}

	private static String frame(String id, String data) {
		return up(TerminalInput.TOPIC, Json.of(JsonMap.of("id", id, "data", data)));
	}

	private void send(BusSession s, String id, String data) {
		bus.receive(s, frame(id, data));
	}

	private static JsonMap answer(String id, String state) {
		return JsonMap.of("id", id, "state", state);
	}

	private static List<LogRecord> audit(Snippet body) {
		return LogRecordCapture.quietly(AUDIT, body);
	}

	private static List<String> messages(List<LogRecord> records, Level level) {
		return records.stream().filter(r -> r.getLevel() == level).map(LogRecord::getMessage).toList();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) create / bind, answers, audit
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_createNullThrows() {
		assertThrows(IllegalArgumentException.class, () -> TerminalInput.create(null));
	}

	@Test void a02_bindTwiceThrows() {
		assertThrows(IllegalStateException.class, () -> input.bind(bus));
	}

	@Test void a03_probeLive() {
		send(alice, "t1", "");
		assertEquals(List.of(answer("t1", "live")), aliceWs.status());
		assertTrue(fake.writes.isEmpty());
	}

	@Test void a04_writeAcceptedNoAnswer() {
		send(alice, "t1", "y\r");
		assertEquals(1, fake.writes.size());
		assertArrayEquals("y\r".getBytes(UTF_8), fake.writes.get(0));
		assertEquals(List.of(), aliceWs.frames, "an accepted data frame gets no answer");
	}

	@Test void a05_deniedNoSink() {
		send(bob, "t1", "");
		send(bob, "t1", "x");
		assertEquals(List.of(answer("t1", "denied"), answer("t1", "denied")), bobWs.status());
		assertEquals(List.of(), aliceWs.frames, "the answer goes to the caller only");
		assertTrue(fake.writes.isEmpty());
	}

	@Test void a06_deniedCannotWatch() throws Exception {
		var carolFake = new FakeSink();
		typists.put("carol", carolFake);
		var carol = open(bus, "carol");
		var carolWs = connect(bus, carol);
		send(carol, "t1", "");
		send(carol, "t1", "x");
		assertEquals(List.of(answer("t1", "denied"), answer("t1", "denied")), carolWs.status());
		assertTrue(carolFake.writes.isEmpty(), "a sink for a caller who may not watch is never written");
	}

	@Test void a07_closed() {
		fake.accepts = false;
		send(alice, "t1", "");
		send(alice, "t1", "x");
		assertEquals(List.of(answer("t1", "closed"), answer("t1", "closed")), aliceWs.status());
		assertTrue(fake.writes.isEmpty());
	}

	@Test void a08b_writeRefusedBecauseTheProcessClosedIsClosedNotBusy() {
		fake.writeResult = false;
		fake.closeOnWrite = true;
		send(alice, "t1", "x");
		assertEquals(List.of(answer("t1", "closed")), aliceWs.status());
	}

	@Test void a08_busy() {
		fake.writeResult = false;
		send(alice, "t1", "x");
		assertEquals(List.of(answer("t1", "busy")), aliceWs.status());
		aliceWs.reset();
		send(alice, "t1", "");
		assertEquals(List.of(answer("t1", "live")), aliceWs.status(), "a probe does not write");
	}

	@Test void a09_bad() {
		var records = audit(() -> bus.receive(alice, up(TerminalInput.TOPIC, "{\"id\":\"../x\",\"data\":\"a\"}")));
		assertEquals(List.of("principal=alice terminal=- state=bad"), messages(records, Level.FINE),
			"an invalid id is logged as '-', never raw");
		var f = aliceWs.status();
		assertEquals(1, f.size());
		assertNull(f.get(0).get("id"));
		assertFalse(f.get(0).containsKey("id"), "an invalid id is not echoed: the answer has no id key");
		assertEquals("bad", f.get(0).get("state"));
		for (var p : List.of("{\"id\":\"t1\"}", "{\"id\":\"t1\",\"data\":1}", "{\"id\":\"t1\",\"data\":\"a\",\"x\":1}", "{\"data\":\"a\"}")) {
			aliceWs.reset();
			bus.receive(alice, up(TerminalInput.TOPIC, p));
			var expected = p.startsWith("{\"id\"") ? answer("t1", "bad") : JsonMap.of("state", "bad");
			assertEquals(List.of(expected), aliceWs.status(), p);
		}
		assertTrue(fake.writes.isEmpty());
	}

	@Test void a10_auditLine() {
		var records = audit(() -> send(alice, "t1", "QZX7\u0003\u001b\r\t\u0008\u0004\u001a\u0003"));
		var info = messages(records, Level.INFO);
		assertEquals(List.of("principal=alice terminal=t1 bytes=12 keys=^C,ESC,CR,TAB,BS,^D,^Z"), info);
		for (var r : records)
			assertFalse(r.getMessage().contains("QZX7"), "no typed text in the audit record");
	}

	@Test void a11_auditKeyCap() {
		var records = audit(() -> {
			send(alice, "t1", "\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\t\n");
			send(alice, "t1", "hello world");
			send(alice, "t1", "\u001b[A");
			send(alice, "t1", "x\u007f");
		});
		assertEquals(List.of(
			"principal=alice terminal=t1 bytes=10 keys=^A,^B,^C,^D,^E,^F,^G,BS",
			"principal=alice terminal=t1 bytes=11 keys=",
			"principal=alice terminal=t1 bytes=3 keys=ESC",
			"principal=alice terminal=t1 bytes=2 keys=BS"
		), messages(records, Level.INFO));
	}

	@Test void a12_refusalsAtFine() {
		var records = audit(() -> {
			send(bob, "t1", "s3cret-denied");
			send(alice, "t1", "");
			fake.writeResult = false;
			send(alice, "t1", "s3cret-busy");
			fake.writeResult = true;
			send(alice, "t1", "s3cret-ok");
		});
		assertEquals(List.of("principal=alice terminal=t1 bytes=9 keys="), messages(records, Level.INFO),
			"only the accepted write is audited at INFO");
		assertEquals(List.of("principal=bob terminal=t1 state=denied", "principal=alice terminal=t1 state=busy"),
			messages(records, Level.FINE));
		for (var r : records)
			assertFalse(r.getMessage().contains("s3cret"), r::getMessage);
	}

	@Test void a13_principalDash() throws Exception {
		typists.put("", fake);
		var anon = open(bus, null);
		connect(bus, anon);
		var records = audit(() -> send(anon, "t1", "x"));
		assertEquals(List.of("principal=- terminal=t1 bytes=1 keys="), messages(records, Level.INFO));
	}

	@Test void a19_principalEscaped() throws Exception {
		var typist = "a b\nc=d\\e%\t";
		var watcherOnly = "e=v x";
		var unicode = "u\u0085v\u2028w\u2029";
		watchers.addAll(Set.of(typist, unicode));
		typists.put(typist, fake);
		typists.put(unicode, fake);
		var s3 = open(bus, unicode);
		connect(bus, s3);
		var s1 = open(bus, typist);
		connect(bus, s1);
		var s2 = open(bus, watcherOnly);
		connect(bus, s2);
		var records = audit(() -> {
			send(s1, "t1", "x");
			send(s2, "t1", "x");
			send(s3, "t1", "x");
		});
		assertEquals(List.of("principal=a%20b%0Ac%3Dd%5Ce%25%09 terminal=t1 bytes=1 keys=",
			"principal=u%C2%85v%E2%80%A8w%E2%80%A9 terminal=t1 bytes=1 keys="), messages(records, Level.INFO),
			"C1 controls and U+2028/U+2029 are percent-encoded as their UTF-8 bytes");
		assertEquals(List.of("principal=e%3Dv%20x terminal=t1 state=denied"), messages(records, Level.FINE));
		for (var r : records) {
			assertEquals(4 - (r.getLevel() == Level.FINE ? 1 : 0), r.getMessage().split(" ").length, r::getMessage);
			assertEquals(r.getMessage().split(" ").length, r.getMessage().split("=", -1).length - 1, "no forged key=value");
		}
	}

	@Test void a14_sinkThrows() {
		fake.throwOnWrite = true;
		var records = LogRecordCapture.quietly(ServerBus.class, () -> send(alice, "t1", "x"));
		assertEquals(1, records.size(), records::toString);
		assertEquals(Level.WARNING, records.get(0).getLevel());
		assertInstanceOf(IOException.class, records.get(0).getThrown());
		assertEquals(JsonMap.of("v", 1, "type", "error", "code", "bus:upstream-failed", "message", "handler failed",
			"topic", TerminalInput.TOPIC), aliceWs.last());
		assertEquals(List.of(), aliceWs.status());
	}

	@Test void a15_beforeBind() throws Exception {
		var unbound = TerminalInput.create(access);
		try (var b2 = bus(BusPolicy.create().upstream(TerminalInput.TOPIC, unbound).downstream(TerminalInput.STATUS_TOPIC, false).build())) {
			var s = open(b2, "alice");
			var ws = connect(b2, s);
			var records = LogRecordCapture.quietly(ServerBus.class, () -> b2.receive(s, frame("t1", "")));
			assertEquals(1, records.size(), records::toString);
			assertInstanceOf(IllegalStateException.class, records.get(0).getThrown());
			assertEquals(JsonMap.of("v", 1, "type", "error", "code", "bus:upstream-failed", "message", "handler failed",
				"topic", TerminalInput.TOPIC), ws.last());
			assertEquals(List.of(), ws.status());
		}
	}

	@Test void a16_idGrammar() {
		for (var id : List.of("7build", "a".repeat(65), "b".repeat(128))) {
			aliceWs.reset();
			send(alice, id, "");
			assertEquals(List.of(answer(id, "live")), aliceWs.status(), id);
		}
		aliceWs.reset();
		send(alice, "c".repeat(129), "");
		assertEquals(List.of(JsonMap.of("state", "bad")), aliceWs.status());
	}

	@Test void a17_twoSessionsOneSink() throws Exception {
		var alice2 = open(bus, "alice");
		var alice2Ws = connect(bus, alice2);
		send(alice, "t1", "a");
		send(alice2, "t1", "b");
		send(alice, "t1", "c");
		send(alice2, "t1", "d");
		assertEquals(List.of("a", "b", "c", "d"), fake.text());
		assertEquals(List.of(), aliceWs.frames);
		assertEquals(List.of(), alice2Ws.frames);
	}

	@Test void a18_callerAttributes() throws Exception {
		var in2 = TerminalInput.create(access);
		try (var b2 = bus(BusPolicy.create().upstream(TerminalInput.TOPIC, in2).downstream(TerminalInput.STATUS_TOPIC, false)
				.sessionAttributes(r -> Map.of("role", "typist")).build())) {
			in2.bind(b2);
			var s = open(b2, "dave");
			var ws = connect(b2, s);
			callers.clear();
			b2.receive(s, frame("t1", ""));
			assertEquals(List.of(answer("t1", "denied")), ws.status());
			assertFalse(callers.isEmpty());
			var c = callers.get(0);
			assertEquals("dave", c.principalName());
			assertEquals("typist", c.attribute("role"));
			assertNull(c.attribute("missing"));
			assertFalse(c instanceof BusSession, "no BusSession reaches app code");
		}
	}
}
