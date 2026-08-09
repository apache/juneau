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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.marshall.sse.SseEvent;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
	"resource" // Closeable resources (BeanStore/broker fixtures) in tests are intentionally unassigned/unclosed; closing is handled by test infrastructure.
})
class SubscriptionsListenDispatch_Test {

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static JsonRpcRequest listenRequest(Object id, Object notifications) {
		var params = JsonMap.of("notifications", notifications, "_meta", validMeta());
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(McpMethods.SUBSCRIPTIONS_LISTEN).setParams(params);
	}

	private static McpExchange exchangeFor(JsonRpcRequest req) {
		return exchangeFor(req, "text/event-stream");
	}

	// C1: subscriptions/listen must negotiate SSE via the Accept header before registering with the broker.
	// Every other test in this class goes through this same helper with a valid "text/event-stream" Accept
	// value, so a real regression in the gate itself would fail broadly, not just the dedicated C1 tests below.
	private static McpExchange exchangeFor(JsonRpcRequest req, String accept) {
		return new McpExchange(req, n -> switch (n) {
			case "Mcp-Method" -> McpMethods.SUBSCRIPTIONS_LISTEN;
			case "Mcp-Name" -> "";
			case "Accept" -> accept;
			default -> null;
		});
	}

	@SuppressWarnings({
		"resource" // Returned BeanStore is owned by the caller (a fresh per-test fixture, GC'd with the test); Eclipse JDT @Owning warning is by design.
	})
	private static BasicBeanStore ctxWith(McpSubscriptionBroker broker, McpSubscriptionsConfig config) {
		return new BasicBeanStore().addBean(McpSubscriptionBroker.class, broker)
			.addBean(McpOptions.class, new McpOptions().setSubscriptions(config));
	}

	// C1: a subscriptions/listen request that never negotiated SSE (Accept missing, or some other media type)
	// must be rejected with a JSON-RPC error BEFORE any broker registration happens - otherwise it falls into
	// ReactiveResponseProcessor's BUFFER shape (infinite demand, response never flushed), leaking the broker
	// slot + pump thread + heartbeat executor forever once a real socket is involved.
	@Test
	void missingAcceptHeaderIsRejectedBeforeRegisteringWithTheBroker() {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var result = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of()), null), config, ctx);

		assertTrue(result instanceof McpResponseResult, "expected a McpResponseResult error, got: " + result);
		var resp = ((McpResponseResult) result).response();
		assertNotNull(resp.getError());
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
		assertEquals(0, broker.activeCount(), "a rejected non-SSE request must not consume a broker slot");
	}

	@Test
	void wrongAcceptHeaderIsRejectedBeforeRegisteringWithTheBroker() {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var result = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of()), "application/json"), config, ctx);

		assertTrue(result instanceof McpResponseResult, "expected a McpResponseResult error, got: " + result);
		var resp = ((McpResponseResult) result).response();
		assertNotNull(resp.getError());
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
		assertEquals(0, broker.activeCount(), "a rejected non-SSE request must not consume a broker slot");
	}

	@Test
	void acceptHeaderContainingEventStreamAmongOtherValuesIsHonored() {
		// A real browser/client Accept header is rarely just "text/event-stream" alone - it is commonly a
		// comma-separated list. The gate must match by substring/contains, not by exact equality.
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var result = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of()), "text/html, text/event-stream;q=0.9, */*;q=0.8"), config, ctx);

		assertTrue(result instanceof McpStreamResult, "expected a McpStreamResult, got: " + result);
		assertEquals(1, broker.activeCount());
	}

	@Test
	void acceptHeaderMatchIsCaseInsensitive() {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var result = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of()), "TEXT/EVENT-STREAM"), config, ctx);

		assertTrue(result instanceof McpStreamResult, "expected a McpStreamResult, got: " + result);
		assertEquals(1, broker.activeCount());
	}

	// I1: an id-less (notification-form) subscriptions/listen request must be a no-op, not a leaked
	// registration under a null listenId - dispatch's notification check must run before the
	// subscriptions/listen branch, not after.
	@Test
	void idLessListenRequestIsANoOpAndRegistersNothing() {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var result = revision.dispatch(exchangeFor(listenRequest(null, JsonMap.of())), config, ctx);

		assertNull(result, "an id-less subscriptions/listen request must be treated as a notification (no-op)");
		assertEquals(0, broker.activeCount(), "an id-less listen request must never register a subscription");
	}

	@Test
	void validRequestReturnsAFlowPublisherAndRegistersWithTheBroker() {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var notifications = JsonMap.of("resourceSubscriptions", List.of("file:///a"), "toolsListChanged", true);
		var result = revision.dispatch(exchangeFor(listenRequest(7, notifications)), config, ctx);

		assertTrue(result instanceof McpStreamResult, "expected a McpStreamResult, got: " + result);
		assertEquals(1, broker.activeCount());
	}

	@Test
	void capabilityGatingDropsUnadvertisedFieldsBeforeRegistering() {
		// Only resources.subscribe is advertised; toolsListChanged must be dropped from the honored filter.
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var notifications = JsonMap.of("resourceSubscriptions", List.of("file:///a"), "toolsListChanged", true);
		var result = revision.dispatch(exchangeFor(listenRequest(7, notifications)), config, ctx);

		var publisher = (SubscriptionsListenPublisher) ((McpStreamResult) result).stream();
		var honoredWire = publisher.honoredFilter();
		assertNotEquals(Boolean.TRUE, honoredWire.getToolsListChanged());
		assertEquals(List.of("file:///a"), honoredWire.getResourceSubscriptions());
	}

	@Test
	void overLimitReturnsAJsonRpcErrorInsteadOfAPublisher() {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var existing = broker.register("existing", new McpSubscriptionFilter(false, false, false, Set.of()));
		try {
			var config = new McpServerConfig();
			var ctx = ctxWith(broker, new McpSubscriptionsConfig().setMaxConcurrentSubscriptions(1));

			var result = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of())), config, ctx);

			assertTrue(result instanceof McpResponseResult, "expected a McpResponseResult error, got: " + result);
			var resp = ((McpResponseResult) result).response();
			assertNotNull(resp.getError());
			assertEquals(McpRevision.CODE_TOO_MANY_SUBSCRIPTIONS, resp.getError().getCode());
			assertEquals(1, broker.activeCount(), "the rejected request must not register a new subscription");
		} finally {
			existing.close();
		}
	}

	@Test
	void concurrentListenRequestsNeverExceedTheCap_evenAgainstASharedBroker() throws Exception {
		final var max = 10;
		final var seeded = max - 1;
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		for (var i = 0; i < seeded; i++)
			broker.register("seed-" + i, new McpSubscriptionFilter(false, false, false, Set.of()));
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig().setMaxConcurrentSubscriptions(max));

		final var threadCount = 50;
		var ready = new CountDownLatch(threadCount);
		var start = new CountDownLatch(1);
		var done = new CountDownLatch(threadCount);
		var accepted = new AtomicInteger();
		var rejected = new AtomicInteger();
		var threads = new ArrayList<Thread>();
		for (var i = 0; i < threadCount; i++) {
			final var id = 1000 + i;
			var t = new Thread(() -> {
				ready.countDown();
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				var result = revision.dispatch(exchangeFor(listenRequest(id, JsonMap.of())), config, ctx);
				(result instanceof McpStreamResult ? accepted : rejected).incrementAndGet();
				done.countDown();
			});
			threads.add(t);
			t.start();
		}
		assertTrue(ready.await(2, TimeUnit.SECONDS), "threads did not all reach the start gate in time");
		start.countDown();
		assertTrue(done.await(5, TimeUnit.SECONDS), "threads did not all finish in time");
		for (var t : threads)
			t.join(1000);

		assertEquals(max, broker.activeCount(), "active count must never exceed the cap");
		assertEquals(1, accepted.get(), "exactly one racer should have been admitted to fill the last slot");
		assertEquals(threadCount - 1, rejected.get(), "every other racer must be rejected, not silently over-admitted");
	}

	/** Minimal capturing {@code Flow.Subscriber} used only to inspect a stream's own ack frame. */
	private static final class CapturingSubscriber implements Flow.Subscriber<SseEvent> {
		final BlockingQueue<SseEvent> events = new LinkedBlockingQueue<>();

		@Override public void onSubscribe(Flow.Subscription s) { s.request(1); }
		@Override public void onNext(SseEvent item) { events.add(item); }
		@Override public void onComplete() { /* unused */ }
		@Override public void onError(Throwable t) { /* unused */ }
	}

	// C3: the internal broker registry key must be decoupled from the client-facing JSON-RPC id, so two
	// concurrent listens sharing the same client id never evict each other - while each stream still echoes
	// the CLIENT's original id (not the internal registry key) in its own frames.
	@Test
	void concurrentListenRequestsWithTheSameClientIdDoNotEvictEachOther() throws Exception {
		var caps = new ServerCapabilities().setResources(new ResourceCapability().setSubscribe(true));
		var revision = new McpRevision(caps);
		var broker = new BasicMcpSubscriptionBroker(1024);
		var config = new McpServerConfig();
		var ctx = ctxWith(broker, new McpSubscriptionsConfig());

		var result1 = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of())), config, ctx);
		var result2 = revision.dispatch(exchangeFor(listenRequest(7, JsonMap.of())), config, ctx);

		assertTrue(result1 instanceof McpStreamResult, "expected a McpStreamResult, got: " + result1);
		assertTrue(result2 instanceof McpStreamResult, "expected a McpStreamResult, got: " + result2);
		var publisher1 = ((McpStreamResult) result1).stream();
		var publisher2 = ((McpStreamResult) result2).stream();
		assertTrue(publisher1 instanceof SubscriptionsListenPublisher, "expected a SubscriptionsListenPublisher, got: " + publisher1);
		assertTrue(publisher2 instanceof SubscriptionsListenPublisher, "expected a SubscriptionsListenPublisher, got: " + publisher2);
		assertEquals(2, broker.activeCount(),
			"two distinct streams sharing the same client id must both remain registered, not evict each other");

		var sub1 = new CapturingSubscriber();
		((SubscriptionsListenPublisher) publisher1).subscribe(sub1);
		var ack1 = sub1.events.poll(2, TimeUnit.SECONDS);
		assertNotNull(ack1, "expected the first stream's ack frame");
		assertTrue(ack1.getData().contains("\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":7"),
			"the first stream must still echo the CLIENT's original id in its ack frame, not the internal registry key");

		var sub2 = new CapturingSubscriber();
		((SubscriptionsListenPublisher) publisher2).subscribe(sub2);
		var ack2 = sub2.events.poll(2, TimeUnit.SECONDS);
		assertNotNull(ack2, "expected the second stream's ack frame");
		assertTrue(ack2.getData().contains("\"" + RequestMeta.KEY_SUBSCRIPTION_ID + "\":7"),
			"the second stream must still echo the CLIENT's original id in its ack frame, not the internal registry key");
	}
}
