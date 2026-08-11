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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Mock HttpTransport lambdas and McpSubscriptionHandle test doubles are exercised/closed via cancel()/close() or the try-with-resources client elsewhere in each test; suppressing resource-leak noise class-wide rather than per call site.
})
class McpClientSubscriptionsListen_Test extends TestBase {

	@Test
	void a01_listen_singleStream_deliversAckThenNotificationsThenComplete_inOrder() throws Exception {
		var ack = new SubscriptionsAcknowledgedNotification()
			.setNotifications(new SubscriptionFilter().setResourcesListChanged(true))
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-1"));
		var resourceUpdated = new ResourceUpdatedNotification().setUri("file:///a.txt")
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-1"));
		var toolsChanged = new ToolsListChangedNotification()
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-1"));

		// A "ping" heartbeat (named event, no data:) is interleaved between real frames to pin that the
		// client's decode loop must skip it rather than choking on a null/empty data payload (see CONTRACT.md /
		// SubscriptionsListenPublisher's HEARTBEAT_EVENT_NAME="ping" real-transport shape).
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
			+ "event: ping\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_RESOURCES_UPDATED + "\",\"params\":" + JsonSerializer.DEFAULT.write(resourceUpdated) + "}\n\n"
			+ "event: ping\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(toolsChanged) + "}\n\n"
			+ "event: ping\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"id\":\"sub-1\",\"result\":{\"resultType\":\"complete\"}}\n\n";

		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var events = new CopyOnWriteArrayList<String>();
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onAcknowledged(SubscriptionFilter honoredFilter) {
				events.add("ack:" + honoredFilter.getResourcesListChanged());
			}
			@Override public void onResourceUpdated(String uri) {
				events.add("updated:" + uri);
			}
			@Override public void onListChanged(McpListChangedKind kind) {
				events.add("listChanged:" + kind);
			}
			@Override public void onComplete() {
				events.add("complete");
				done.countDown();
			}
			@Override public void onError(Throwable t) {
				events.add("error:" + t);
				done.countDown();
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter().setResourcesListChanged(true).setToolsListChanged(true), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS), "listener never completed: " + events);
			assertEquals(List.of("ack:true", "updated:file:///a.txt", "listChanged:TOOLS", "complete"), events);
			assertFalse(handle.isOpen());
		}
	}

	@Test
	void a01b_listen_stampsMcpMethodAndMcpNameHeaders_onTheOpeningRequest() throws Exception {
		var seenMethod = new AtomicReference<String>();
		var seenName = new AtomicReference<String>();
		var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-1b"));
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"id\":\"sub-1b\",\"result\":{\"resultType\":\"complete\"}}\n\n";
		HttpTransport transport = tReq -> {
			var m = tReq.getFirstHeader("Mcp-Method");
			var n = tReq.getFirstHeader("Mcp-Name");
			seenMethod.set(m == null ? null : m.value());
			seenName.set(n == null ? null : n.value());
			return TransportResponse.builder().statusCode(200)
				.header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();
		};
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onComplete() { done.countDown(); }
		};
		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS));
			handle.close();
		}
		// This is precisely the header contract McpRevision.dispatch()'s validateHeaders(...) enforces
		// unconditionally before branching on the method (server-side); a real v2 server would otherwise
		// reject the opening subscriptions/listen POST with -32600 "Missing required header: Mcp-Method".
		assertEquals(McpMethods.SUBSCRIPTIONS_LISTEN, seenMethod.get());
		assertEquals("", seenName.get());
	}

	// M2: McpSubscriptionHandle.id() must return the client-generated JSON-RPC request id sent on the
	// opening subscriptions/listen POST, so callers can correlate the handle against server-side logs/traces.
	@Test
	void a01c_handleId_returnsTheClientGeneratedRequestIdSentOnTheOpeningPost() throws Exception {
		var sentId = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			var out = new ByteArrayOutputStream();
			try {
				tReq.getBody().writeTo(out);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			var body = JsonParser.DEFAULT.read(out.toString(StandardCharsets.UTF_8), org.apache.juneau.marshall.collections.JsonMap.class);
			sentId.set((String) body.get("id"));
			var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
				.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, sentId.get()));
			var inbound =
				"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
				+ "data: {\"jsonrpc\":\"2.0\",\"id\":\"" + sentId.get() + "\",\"result\":{\"resultType\":\"complete\"}}\n\n";
			return TransportResponse.builder().statusCode(200)
				.header("Content-Type", "text/event-stream")
				.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();
		};
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onComplete() { done.countDown(); }
		};
		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS));
			assertNotNull(sentId.get());
			assertFalse(sentId.get().isEmpty());
			assertEquals(sentId.get(), handle.id(), "handle.id() must return the exact request id sent on the opening POST");
		}
	}

	@Test
	void a02_cancel_isIdempotent() throws Exception {
		var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-2"));
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"id\":\"sub-2\",\"result\":{\"resultType\":\"complete\"}}\n\n";
		var done = new CountDownLatch(1);
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();
		var listener = new McpSubscriptionListener() {
			@Override public void onComplete() { done.countDown(); }
		};
		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS));
			assertFalse(handle.isOpen());
			assertDoesNotThrow(handle::cancel);
			assertDoesNotThrow(handle::cancel);
			assertDoesNotThrow(handle::close);
		}
	}

	@Test
	void a03_malformedFrame_invokesOnError_notOnAcknowledged() throws Exception {
		var inbound = "data: {not valid json\n\n";
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();
		var errorLatch = new CountDownLatch(1);
		var seenError = new AtomicReference<Throwable>();
		var listener = new McpSubscriptionListener() {
			@Override public void onAcknowledged(SubscriptionFilter honoredFilter) {
				fail("must not acknowledge a malformed frame");
			}
			@Override public void onError(Throwable t) {
				seenError.set(t);
				errorLatch.countDown();
			}
		};
		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(errorLatch.await(5, TimeUnit.SECONDS));
			assertNotNull(seenError.get());
			assertFalse(handle.isOpen());
		}
	}

	@Test
	void a04_streamEndsWithoutTerminalFrame_firesOnError_notOnComplete() throws Exception {
		var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-4"));
		var resourceUpdated = new ResourceUpdatedNotification().setUri("file:///a.txt")
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-4"));
		// Deliberately NO terminal frame: the stream just ends (clean EOF) after two real frames, simulating
		// an abrupt server-side drop rather than a graceful subscriptions/listen completion.
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_RESOURCES_UPDATED + "\",\"params\":" + JsonSerializer.DEFAULT.write(resourceUpdated) + "}\n\n";

		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var errorLatch = new CountDownLatch(1);
		var seenError = new AtomicReference<Throwable>();
		var completeCalled = new AtomicBoolean();
		var listener = new McpSubscriptionListener() {
			@Override public void onError(Throwable t) {
				seenError.set(t);
				errorLatch.countDown();
			}
			@Override public void onComplete() {
				completeCalled.set(true);
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "onError never fired for an abrupt EOF with no terminal frame");
			assertInstanceOf(EOFException.class, seenError.get());
			assertFalse(completeCalled.get(), "onComplete must not fire when the stream ended without a terminal frame");
			assertFalse(handle.isOpen());
		}
	}

	@Test
	void a05_listenerCallbackThrows_doesNotFireOnError_continuesDeliveringSubsequentFrames() throws Exception {
		var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-5"));
		var resourceUpdated = new ResourceUpdatedNotification().setUri("file:///a.txt")
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-5"));
		var toolsChanged = new ToolsListChangedNotification()
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-5"));
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_RESOURCES_UPDATED + "\",\"params\":" + JsonSerializer.DEFAULT.write(resourceUpdated) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(toolsChanged) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"id\":\"sub-5\",\"result\":{\"resultType\":\"complete\"}}\n\n";

		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var events = new CopyOnWriteArrayList<String>();
		var done = new CountDownLatch(1);
		var errorCount = new AtomicInteger();
		var listener = new McpSubscriptionListener() {
			@Override public void onAcknowledged(SubscriptionFilter honoredFilter) {
				events.add("ack");
			}
			@Override public void onResourceUpdated(String uri) {
				events.add("updated");
				throw new RuntimeException("listener bug: must be contained, not routed through onError");
			}
			@Override public void onListChanged(McpListChangedKind kind) {
				events.add("listChanged:" + kind);
			}
			@Override public void onComplete() {
				events.add("complete");
				done.countDown();
			}
			@Override public void onError(Throwable t) {
				errorCount.incrementAndGet();
				done.countDown();
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(done.await(5, TimeUnit.SECONDS), "listener never completed: " + events);
			assertEquals(0, errorCount.get(), "a listener's own exception must not be routed through onError");
			assertEquals(List.of("ack", "updated", "listChanged:TOOLS", "complete"), events,
				"pump must contain the throwing callback and keep delivering subsequent frames");
			assertFalse(handle.isOpen());
		}
	}

	@Test
	void a06_cancelWhileBlockedInRead_terminatesPumpThread_andClosesStream() throws Exception {
		var stream = new BlockingInputStream();
		// The connection-release callback - not the body InputStream's own close() - is what unblocks a
		// parked read here, exactly like a real transport: closing an actual socket/connection interrupts a
		// blocked native read from outside the JVM, without needing the InputStreamReader's own lock, which
		// the parked reader thread holds for the full duration of its blocking read() call.
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(stream)
			.closeCallback(stream::release)
			.build();
		var listener = new McpSubscriptionListener() {};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			assertTrue(stream.awaitBlocked(5, TimeUnit.SECONDS), "pump never parked in a blocking read");
			assertTrue(handle instanceof McpClient.SubscriptionPump);
			var pump = (McpClient.SubscriptionPump) handle;

			handle.cancel();

			pump.pumpThread.join(5_000);
			assertFalse(pump.pumpThread.isAlive(), "pump thread failed to terminate after cancel()");
			assertTrue(stream.isReleased(), "cancel() never released the parked read via the connection close callback");
		}
	}

	/**
	 * Blocks on {@code latch} swallowing {@link InterruptedException} (re-asserting the interrupt flag), so
	 * the {@link McpSubscriptionListener} callbacks below - which override interfaces methods that declare no
	 * checked exception - can wait on it inline.
	 */
	private static void awaitUninterruptibly(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Regression coverage for the happens-before race root-caused from a CI flake in {@link #a01_listen_singleStream_deliversAckThenNotificationsThenComplete_inOrder()}:
	 * {@code assertFalse(handle.isOpen())} right after {@code done.await()} intermittently saw {@code true}
	 * because {@code SubscriptionPump} used to flip its {@code open} flag to <jk>false</jk> in the
	 * {@code finally} block - i.e. AFTER invoking the terminal listener callback - so a caller thread woken
	 * from a latch/future the callback itself completes could observe the callback's effects while
	 * {@code open} was still <jk>true</jk> ({@code CountDownLatch}'s happens-before is one-directional: it
	 * only orders {@code countDown()} before the matching {@code await()} return, not the reverse).
	 *
	 * <p>
	 * Rather than reproduce that as a timing-dependent flake, {@code b01}-{@code b04} below observe
	 * {@code isOpen()} from a place where the ordering is no longer a race at all: <i>from inside the
	 * terminal callback itself</i>, which always runs on the pump thread strictly before the pump thread
	 * reaches its {@code finally} block. By the time {@code onComplete()}/{@code onError(Throwable)} starts
	 * running, the pump has therefore already made its "did I flip {@code open} before or after invoking
	 * this callback" decision - reading {@code isOpen()} from inside the callback can only ever observe
	 * whichever the pump did first, deterministically:
	 * <ul>
	 * 	<li>Buggy ({@code open=false} only in {@code finally}): the callback observes {@code open==true}.
	 * 	<li>Fixed ({@code open=false} immediately before {@code invokeListener(...)} at each terminal site):
	 * 		the callback observes {@code open==false}.
	 * </ul>
	 *
	 * <p>
	 * The one wrinkle is that the callback needs a reference to the handle {@link McpClient#listen} returns,
	 * and that return does not happen until after the pump thread has already been started - so the pump
	 * thread could reach the callback before the test thread has published the handle. Each test below closes
	 * that window with a small handoff gate ({@code handleReady}): the callback blocks on it before reading
	 * {@code isOpen()}. That gate only delays <i>when</i> {@code isOpen()} is read, never <i>what</i> it
	 * reads - the buggy-vs-fixed ordering decision the assertion is pinning was already made by the pump
	 * before it ever called into the callback, so blocking briefly inside the callback cannot change it.
	 */
	@Test
	void b01_isOpenObservedFromWithinOnComplete_isFalse_notTrueEvenThoughFinallyHasNotRunYet() throws Exception {
		var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-b1"));
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n"
			+ "data: {\"jsonrpc\":\"2.0\",\"id\":\"sub-b1\",\"result\":{\"resultType\":\"complete\"}}\n\n";
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var handleRef = new AtomicReference<McpSubscriptionHandle>();
		var handleReady = new CountDownLatch(1);
		var capturedOpen = new AtomicBoolean(true);
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onComplete() {
				awaitUninterruptibly(handleReady);
				capturedOpen.set(handleRef.get().isOpen());
				done.countDown();
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			handleRef.set(handle);
			handleReady.countDown();
			assertTrue(done.await(5, TimeUnit.SECONDS), "onComplete never fired");
			assertFalse(capturedOpen.get(),
				"isOpen() read from inside onComplete() itself must already be false - open must be flipped "
				+ "before the terminal callback is invoked, not after, in the finally block");
		}
	}

	@Test
	void b02_isOpenObservedFromWithinOnError_forServerErrorFrame_isFalse_notTrueEvenThoughFinallyHasNotRunYet() throws Exception {
		var inbound = "data: {\"jsonrpc\":\"2.0\",\"id\":\"sub-b2\",\"error\":{\"code\":-32000,\"message\":\"boom\"}}\n\n";
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var handleRef = new AtomicReference<McpSubscriptionHandle>();
		var handleReady = new CountDownLatch(1);
		var capturedOpen = new AtomicBoolean(true);
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onError(Throwable t) {
				awaitUninterruptibly(handleReady);
				capturedOpen.set(handleRef.get().isOpen());
				done.countDown();
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			handleRef.set(handle);
			handleReady.countDown();
			assertTrue(done.await(5, TimeUnit.SECONDS), "onError never fired for a server JSON-RPC error terminal frame");
			assertFalse(capturedOpen.get(),
				"isOpen() read from inside onError() itself must already be false for the dispatch(...) "
				+ "error-terminal-frame branch");
		}
	}

	@Test
	void b03_isOpenObservedFromWithinOnError_forAbruptEof_isFalse_notTrueEvenThoughFinallyHasNotRunYet() throws Exception {
		var ack = new SubscriptionsAcknowledgedNotification().setNotifications(new SubscriptionFilter())
			.setMeta(new RequestMeta().set(RequestMeta.KEY_SUBSCRIPTION_ID, "sub-b3"));
		// Deliberately no terminal frame, exactly like a04: the stream just ends (clean EOF).
		var inbound =
			"data: {\"jsonrpc\":\"2.0\",\"method\":\"" + McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED + "\",\"params\":" + JsonSerializer.DEFAULT.write(ack) + "}\n\n";
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var handleRef = new AtomicReference<McpSubscriptionHandle>();
		var handleReady = new CountDownLatch(1);
		var capturedOpen = new AtomicBoolean(true);
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onError(Throwable t) {
				awaitUninterruptibly(handleReady);
				capturedOpen.set(handleRef.get().isOpen());
				done.countDown();
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			handleRef.set(handle);
			handleReady.countDown();
			assertTrue(done.await(5, TimeUnit.SECONDS), "onError never fired for an abrupt EOF with no terminal frame");
			assertFalse(capturedOpen.get(),
				"isOpen() read from inside onError() itself must already be false for run()'s abrupt-EOF "
				+ "(reached-terminal==false) branch");
		}
	}

	@Test
	void b04_isOpenObservedFromWithinOnError_forMalformedFrameDecodeException_isFalse_notTrueEvenThoughFinallyHasNotRunYet() throws Exception {
		// Same malformed payload as a03, but this exercises run()'s catch (Exception e) branch specifically
		// (the decode failure is thrown out of dispatch(...) itself, not returned as a JSON-RPC error frame).
		var inbound = "data: {not valid json\n\n";
		HttpTransport transport = tReq -> TransportResponse.builder().statusCode(200)
			.header("Content-Type", "text/event-stream")
			.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();

		var handleRef = new AtomicReference<McpSubscriptionHandle>();
		var handleReady = new CountDownLatch(1);
		var capturedOpen = new AtomicBoolean(true);
		var done = new CountDownLatch(1);
		var listener = new McpSubscriptionListener() {
			@Override public void onError(Throwable t) {
				awaitUninterruptibly(handleReady);
				capturedOpen.set(handleRef.get().isOpen());
				done.countDown();
			}
		};

		try (var client = McpClient.builder().endpoint("http://x/mcp").transport(transport).build()) {
			var handle = client.listen(new SubscriptionFilter(), listener);
			handleRef.set(handle);
			handleReady.countDown();
			assertTrue(done.await(5, TimeUnit.SECONDS), "onError never fired for a malformed frame");
			assertFalse(capturedOpen.get(),
				"isOpen() read from inside onError() itself must already be false for run()'s catch "
				+ "(Exception e) decode-failure branch");
		}
	}

	/**
	 * A genuinely blocking {@link InputStream}: {@link #read()} parks on a latch (no busy-polling) until
	 * {@link #release()} is called from another thread, then throws — modeling a real closed-socket read
	 * failure so {@link McpClientSubscriptionsListen_Test#a06_cancelWhileBlockedInRead_terminatesPumpThread_andClosesStream}
	 * can deterministically exercise the "closing the connection unblocks the parked pump" cancellation
	 * contract. {@code release()} is deliberately invoked via the transport's close callback rather than this
	 * stream's own {@link #close()}: {@link java.io.InputStreamReader#close()} synchronizes on the same
	 * per-reader monitor its {@code read()} holds for the entire duration of a blocking call, so unblocking
	 * from {@link #close()} itself would deadlock against the parked pump thread.
	 */
	private static final class BlockingInputStream extends InputStream {
		private final CountDownLatch blockedSignal = new CountDownLatch(1);
		private final CountDownLatch releaseSignal = new CountDownLatch(1);
		private volatile boolean released;

		boolean awaitBlocked(long timeout, TimeUnit unit) throws InterruptedException {
			return blockedSignal.await(timeout, unit);
		}

		boolean isReleased() {
			return released;
		}

		void release() {
			released = true;
			releaseSignal.countDown();
		}

		@Override
		public int read() throws IOException {
			blockedSignal.countDown();
			try {
				releaseSignal.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("Interrupted while blocked in read()");
			}
			throw new IOException("Stream released while blocked in read()");
		}
	}
}
