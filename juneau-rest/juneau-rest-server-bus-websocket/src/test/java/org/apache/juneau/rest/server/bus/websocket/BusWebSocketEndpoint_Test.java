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
package org.apache.juneau.rest.server.bus.websocket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.bus.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import jakarta.websocket.*;

/**
 * The per-connection endpoint, against a real {@link ServerBus} (sessions from the real session POST): it closes
 * refused handshakes, connects an adapted {@link Session} (resync first), forwards inbound text to the bus, and closes
 * with 1009 on oversize.  Only the jakarta.websocket interfaces are mocked.
 */
@SuppressWarnings({
	"resource" // The shared MockRestClient lives for the whole class; each test's bus is closed in @AfterEach.
})
class BusWebSocketEndpoint_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements BusEventsMixin {
		private static final long serialVersionUID = 1L;
		static volatile ServerBus bus;
		@Override public ServerBus serverBus() { return bus; }
	}

	private static final MockRestClient C = MockRestClient.buildLax(R.class);
	private static final Principal JB = () -> "jb";
	private static final int MAX_BYTES = 256;
	private static final String CANCEL = "{\"v\":1,\"type\":\"pub\",\"topic\":\"ops.cancel-all\",\"payload\":{\"reason\":\"r1\"}}";

	private ServerBus bus;
	private BusSession busSession;
	private Session ws;
	private RemoteEndpoint.Async remote;
	private EndpointConfig config;
	private Map<String,Object> props;
	private final List<String> sent = new CopyOnWriteArrayList<>();
	private final List<Object> handled = new CopyOnWriteArrayList<>();
	private final Queue<Runnable> closer = new ConcurrentLinkedQueue<>();
	private boolean autoComplete = true;

	@BeforeEach void setUp() throws Exception {
		bus = ServerBus.create(BusPolicy.create()
			.downstream("ops.jobs", true)
			.upstream("ops.cancel-all", (s, t, p) -> handled.add(p.get("reason")))
			.allowedOrigins("https://ops.example.com")
			.maxFrameBytes(MAX_BYTES)
			.maxQueuedFrames(2)
			.build());
		bus.enableWebSocket(BusWebSockets.PREFIX);
		R.bus = bus;
		var id = BusFixture.open(C, JB, "websocket", List.of("ops.jobs"), List.of("ops.cancel-all"));
		busSession = bus.session(id, JB).orElseThrow();
		ws = mock(Session.class);
		remote = mock(RemoteEndpoint.Async.class);
		config = mock(EndpointConfig.class);
		props = new HashMap<>();
		when(ws.getAsyncRemote()).thenReturn(remote);
		when(config.getUserProperties()).thenReturn(props);
		doAnswer(i -> {
			sent.add(i.getArgument(0));
			if (autoComplete)
				i.<SendHandler>getArgument(1).onResult(new SendResult(ws));
			return null;
		}).when(remote).sendText(anyString(), any(SendHandler.class));
	}

	@AfterEach void tearDown() {
		bus.close();
	}

	private BusWebSocketEndpoint endpoint() {
		return new BusWebSocketEndpoint(bus, closer::add);
	}

	private CloseReason closedWith() throws IOException {
		var c = ArgumentCaptor.forClass(CloseReason.class);
		verify(ws).close(c.capture());
		return c.getValue();
	}

	private void open(BusWebSocketEndpoint e) {
		props.put(BusWebSocketConfigurator.SESSION_PROPERTY, busSession);
		e.onOpen(ws, config);
	}

	@SuppressWarnings("unchecked")
	private MessageHandler.Whole<String> textHandler() {
		var h = ArgumentCaptor.forClass(MessageHandler.Whole.class);
		verify(ws).addMessageHandler(eq(String.class), h.capture());
		return h.getValue();
	}

	private static String type(String frame) {
		return JsonMap.ofString(frame).getString("type");
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) onOpen
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_refusalClosesWithItsCode_andNeverConnects() throws Exception {
		props.put(BusWebSocketConfigurator.SESSION_PROPERTY, busSession);
		props.put(BusWebSocketConfigurator.REFUSAL_PROPERTY, BusWebSocketConfigurator.REFUSED);
		endpoint().onOpen(ws, config);
		var r = closedWith();
		assertEquals(4403, r.getCloseCode().getCode());
		assertEquals("bus:refused", r.getReasonPhrase());
		assertFalse(busSession.isConnected());
		assertTrue(sent.isEmpty());
	}

	@Test void a02_unknownSessionClosesWith4401() throws Exception {
		props.put(BusWebSocketConfigurator.REFUSAL_PROPERTY, BusWebSocketConfigurator.UNKNOWN_SESSION);
		endpoint().onOpen(ws, config);
		assertEquals(4401, closedWith().getCloseCode().getCode());
	}

	@Test void a03_noSessionPropertyClosesWith4401() throws Exception {
		endpoint().onOpen(ws, config);
		var r = closedWith();
		assertEquals(4401, r.getCloseCode().getCode());
		assertEquals("bus:unknown-session", r.getReasonPhrase());
		assertFalse(busSession.isConnected());
	}

	@Test void a04_goodOpenConnects_sendsTheResync_andCapsTheBuffer() {
		open(endpoint());
		verify(ws).setMaxTextMessageBufferSize(MAX_BYTES);
		assertTrue(busSession.isConnected());
		assertEquals(List.of("resync-begin", "resync-end"), sent.stream().map(BusWebSocketEndpoint_Test::type).toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Inbound: text -> the bus; oversize -> 1009
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_inboundTextReachesTheUpstreamHandler() {
		open(endpoint());
		textHandler().onMessage(CANCEL);
		assertEquals(List.of("r1"), handled);
	}

	@Test void b02_oversizeClosesWith1009_andIsNotForwarded() throws Exception {
		open(endpoint());
		textHandler().onMessage(CANCEL.replace("r1", "x".repeat(MAX_BYTES)));
		var r = closedWith();
		assertEquals(1009, r.getCloseCode().getCode());
		assertEquals("bus:frame-too-large", r.getReasonPhrase());
		assertTrue(handled.isEmpty());
	}

	@Test void b03_sizeIsMeasuredInUtf8Bytes() throws Exception {
		open(endpoint());
		var chars = "é".repeat(MAX_BYTES / 2 + 1);   // MAX_BYTES / 2 + 1 chars, MAX_BYTES + 2 bytes
		assertTrue(chars.length() <= MAX_BYTES);
		textHandler().onMessage(chars);
		assertEquals(1009, closedWith().getCloseCode().getCode());
	}

	@Test void b04_aFrameAtTheLimitIsForwarded() throws Exception {
		open(endpoint());
		var pad = MAX_BYTES - CANCEL.replace("r1", "").length();
		textHandler().onMessage(CANCEL.replace("r1", "x".repeat(pad)));
		verify(ws, never()).close(any(CloseReason.class));
		assertEquals(1, handled.size());
	}

	@Test void b05_aNonObjectPayloadNeverReachesTheHandler() {
		open(endpoint());
		sent.clear();
		textHandler().onMessage("{\"v\":1,\"type\":\"pub\",\"topic\":\"ops.cancel-all\",\"payload\":[1,2]}");
		textHandler().onMessage("{\"v\":1,\"type\":\"pub\",\"topic\":\"ops.cancel-all\",\"payload\":\"text\"}");
		assertTrue(handled.isEmpty());
		assertEquals(List.of("error", "error"), sent.stream().map(BusWebSocketEndpoint_Test::type).toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Outbound: live frames, and the slow consumer's close is handed off
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_aPublishReachesTheSocket() {
		open(endpoint());
		sent.clear();
		bus.publish("ops.jobs", JsonMap.of("schemaVersion", 1));
		assertEquals(List.of("pub"), sent.stream().map(BusWebSocketEndpoint_Test::type).toList());
	}

	@Test void c02_aSlowConsumerIsClosedWith4429_onAnotherThread() throws Exception {
		autoComplete = false;
		open(endpoint());                                      // resync-begin in flight, resync-end queued (1 of 2)
		bus.publish("ops.jobs", JsonMap.of("n", 1));           // queued (2 of 2)
		bus.publish("ops.jobs", JsonMap.of("n", 2));           // full: the bus detaches the sink while holding its lock
		verify(ws, never()).close(any(CloseReason.class));
		assertFalse(busSession.isConnected());
		var r = closer.poll();
		assertNotNull(r, "the close must have been handed off");
		r.run();
		var c = closedWith();
		assertEquals(4429, c.getCloseCode().getCode());
		assertEquals("bus:slow-consumer", c.getReasonPhrase());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) onClose / onError
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_closeDisconnectsTheSession_andStopsTheSink() {
		var e = endpoint();
		open(e);
		assertTrue(busSession.isConnected());
		e.onClose(ws, new CloseReason(CloseReason.CloseCodes.GOING_AWAY, ""));
		assertFalse(busSession.isConnected());
		sent.clear();
		bus.publish("ops.jobs", JsonMap.of("n", 1));
		assertTrue(sent.isEmpty());
	}

	@Test void d02_closeOfARefusedConnectionDoesNotDisconnectAnotherConnection() {
		var live = endpoint();
		open(live);
		props.put(BusWebSocketConfigurator.REFUSAL_PROPERTY, BusWebSocketConfigurator.REFUSED);
		var refused = endpoint();
		refused.onOpen(ws, config);
		refused.onClose(ws, new CloseReason(CloseReason.CloseCodes.getCloseCode(4403), "bus:refused"));
		assertTrue(busSession.isConnected(), "the refused connection never owned the session");
	}

	@Test void d03_aReplacedConnectionClosingDoesNotDisconnectItsReplacement() {
		var first = endpoint();
		open(first);
		var second = endpoint();
		open(second);
		first.onClose(ws, new CloseReason(CloseReason.CloseCodes.GOING_AWAY, ""));
		assertTrue(busSession.isConnected());
	}

	@Test void d04_errorDoesNotThrow() {
		var e = endpoint();
		open(e);
		assertDoesNotThrow(() -> e.onError(ws, new IOException("reset")));
	}

	@Test void d05_errorLogsOnlyTheExceptionClass() {
		var records = new ArrayList<java.util.logging.LogRecord>();
		var log = java.util.logging.Logger.getLogger(BusWebSocketEndpoint.class.getName());
		var handler = new java.util.logging.Handler() {
			@Override public void publish(java.util.logging.LogRecord r) { records.add(r); }
			@Override public void flush() {}
			@Override public void close() {}
		};
		var old = log.getLevel();
		log.setLevel(java.util.logging.Level.ALL);
		log.addHandler(handler);
		try {
			var e = endpoint();
			open(e);
			e.onError(ws, new IOException("secret-session-id"));
		} finally {
			log.removeHandler(handler);
			log.setLevel(old);
		}
		assertFalse(records.isEmpty());
		for (var r : records) {
			assertNull(r.getThrown());
			var text = java.text.MessageFormat.format(r.getMessage(), r.getParameters());
			assertFalse(text.contains("secret-session-id"), text);
		}
		assertTrue(records.stream().anyMatch(r -> r.getParameters() != null && r.getParameters()[0].equals(IOException.class.getName())));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) connect ordering and failure
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_theMessageHandlerIsRegisteredAfterTheBusConnects() {
		var handlerAtFirstSend = new boolean[] {false};
		var added = new boolean[] {false};
		doAnswer(i -> { added[0] = true; return null; }).when(ws).addMessageHandler(eq(String.class), any(MessageHandler.Whole.class));
		doAnswer(i -> {
			if (sent.isEmpty())
				handlerAtFirstSend[0] = added[0];
			sent.add(i.getArgument(0));
			i.<SendHandler>getArgument(1).onResult(new SendResult(ws));
			return null;
		}).when(remote).sendText(anyString(), any(SendHandler.class));
		open(endpoint());
		assertFalse(handlerAtFirstSend[0], "the resync went out before any inbound frame could be handled");
		assertTrue(added[0]);
	}

	@Test void e02_aFailedConnectClosesWith1011_andRegistersNoHandler() throws Exception {
		props.put(BusWebSocketConfigurator.SESSION_PROPERTY, mock(BusSession.class));
		var records = new ArrayList<java.util.logging.LogRecord>();
		var log = java.util.logging.Logger.getLogger(BusWebSocketEndpoint.class.getName());
		var handler = new java.util.logging.Handler() {
			@Override public void publish(java.util.logging.LogRecord r) { records.add(r); }
			@Override public void flush() {}
			@Override public void close() {}
		};
		var useParent = log.getUseParentHandlers();
		log.setUseParentHandlers(false);  // the expected warning stays out of the test output
		log.addHandler(handler);
		try {
			endpoint().onOpen(ws, config);
		} finally {
			log.removeHandler(handler);
			log.setUseParentHandlers(useParent);
		}
		assertEquals(1, records.size());
		assertEquals(java.util.logging.Level.WARNING, records.get(0).getLevel());
		var r = closedWith();
		assertEquals(1011, r.getCloseCode().getCode());
		assertEquals("bus:connect-failed", r.getReasonPhrase());
		verify(ws, never()).addMessageHandler(eq(String.class), any(MessageHandler.Whole.class));
	}
}
