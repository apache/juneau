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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.mcp.v20260728.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * End-to-end coverage for MCP {@code 2026-07-28} SEP-2575 {@code subscriptions/listen} (C8), driven through the
 * real {@link McpClient#listen} against a live embedded-Jetty server. This is the repo's first real-Jetty test to
 * exercise a held-open SSE response over the actual {@code AsyncContext}/{@code startAsync()} branch — every prior
 * SSE test uses {@code MockRestClient}'s synchronous fallback path, which cannot represent a held-open response at
 * all.
 *
 * <p>
 * The fixture's {@link McpSubscriptionsConfig#getHeartbeatIntervalMs() heartbeatIntervalMs} is tuned down to
 * {@value #HEARTBEAT_INTERVAL_MS}&nbsp;ms (from the 15s production default) so heartbeat-arrival coverage
 * ({@link #c01_heartbeat_arrivesRepeatedlyOverLiveConnection_whileIdle}) does not need to wait through a full
 * production-scale interval; this also means every other test in this class incidentally exercises the client
 * pump's "skip a named {@code ping} event" path for real, over the wire, on every run.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class McpSubscriptionsListenIntegration_Test extends TestBase {

	private static final long HEARTBEAT_INTERVAL_MS = 150L;

	private static McpToolHandler publishTool() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName("publish").setDescription("Publishes a resources/updated change for the given uri");
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var uri = (String) arguments.get("uri");
				ctx.getBean(McpSubscriptions.class).get().resourceUpdated(uri);
				return McpToolOutcome.text("published:" + uri);
			}
		};
	}

	private static McpToolHandler activeCountTool() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() {
				return new McpToolSpec().setName("activeCount").setDescription("Returns the current active-subscription count");
			}
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var count = ctx.getBean(McpSubscriptionBroker.class).get().activeCount();
				return McpToolOutcome.text(String.valueOf(count));
			}
		};
	}

	// SseSerializer must be explicitly registered for Accept: text/event-stream negotiation to succeed against
	// a real servlet container - unlike MockRestClient (which every prior MCP test relies on), a live Jetty
	// container enforces strict content negotiation, and the neutral/v2 McpRestServlet base classes only wire
	// JSON serializers by default (see ReactiveResponseProcessor_Test's "Accept-header-driven SSE opt-in" case).
	@Rest(serializers = {JsonSerializer.class, SseSerializer.class}, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Fixture extends org.apache.juneau.rest.server.mcp.v20260728.McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("it-subscriptions-fixture").setVersion("1.0.0")
				.addTool(publishTool()).addTool(activeCountTool());
		}

		@Override
		protected ServerCapabilities capabilities() {
			return new ServerCapabilities()
				.setResources(new ResourceCapability().setSubscribe(true).setListChanged(true))
				.setTools(new ToolCapability().setListChanged(true));
		}

		// Short heartbeat so c01 (below) can observe repeated "ping" frames over a real held-open connection
		// without waiting through the 15s production default.
		@Override
		protected McpSubscriptionsConfig createSubscriptionsConfig() {
			return new McpSubscriptionsConfig().setHeartbeatIntervalMs(HEARTBEAT_INTERVAL_MS);
		}
	}

	@Configuration
	public static class FixtureConfig {
		@Bean public Servlet mcpServlet() { return new Fixture(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(FixtureConfig.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

	private static McpClient.Builder clientBuilder() {
		return McpClient.builder().endpoint(fixture.getRootUrl() + "/");
	}

	// =================================================================================================================
	// A: acknowledged -> published notification round-trip -> server-side cleanup on close (assertions 1, 2, 4).
	// =================================================================================================================

	@Test void a01_listen_receivesAcknowledgedThenPublishedNotification_thenBrokerCleansUpOnClose() throws Exception {
		try (var client = clientBuilder().build()) {
			var events = new LinkedBlockingQueue<String>();
			var filter = new SubscriptionFilter().setResourceSubscriptions(List.of("file:///a.txt")).setResourcesListChanged(true);
			var acknowledged = new LinkedBlockingQueue<SubscriptionFilter>();
			var handle = client.listen(filter, new McpSubscriptionListener() {
				@Override public void onAcknowledged(SubscriptionFilter honoredFilter) {
					acknowledged.add(honoredFilter);
					events.add("ack");
				}
				@Override public void onResourceUpdated(String uri) {
					events.add("updated:" + uri);
				}
				@Override public void onListChanged(McpListChangedKind kind) {
					events.add("listChanged:" + kind);
				}
				@Override public void onError(Throwable t) {
					events.add("error:" + t);
				}
			});
			try {
				// Assertion 1: no -32600 "Missing required header" error frame - the mandatory acknowledged
				// frame is the very first thing received. This is precisely what the McpClient.listen(...)
				// header fix enables: without it, this poll would instead see "error:..." (or time out, since
				// the client would never receive a well-formed acknowledged frame at all).
				assertEquals("ack", events.poll(15, TimeUnit.SECONDS));
				var honoredFilter = acknowledged.poll(15, TimeUnit.SECONDS);
				assertNotNull(honoredFilter);
				// Compared by content (List#equals), not by List#toString() format - the parsed list's
				// concrete runtime type renders its toString() as JSON (quoting string elements), which is a
				// distinct concern from the (unquoted) String content each element actually carries.
				assertEquals(List.of("file:///a.txt"), honoredFilter.getResourceSubscriptions());
				assertEquals(Boolean.TRUE, honoredFilter.getResourcesListChanged());

				var countBefore = client.callTool("activeCount", Map.of());
				assertEquals("1", ((TextContent) countBefore.getContent().get(0)).getText());

				// Idle window spanning several heartbeat intervals before publishing: the fixture's live
				// heartbeat frames are certainly interleaved on the wire during this wait (proven directly by
				// c01, below), so an empty poll here demonstrates the client pump silently skips them rather
				// than raising a spurious callback (onError, a bogus "updated", etc.). BlockingQueue#poll's
				// bounded wait races against the real background pump thread - it returns the instant a
				// (spurious) event arrives, and only blocks the full duration when none does.
				assertNull(events.poll(6 * HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS),
					"a heartbeat frame must never surface as a listener callback");

				// Assertion 2: a server-emitted change (via the neutral McpSubscriptions SPI, reached through
				// the tool handler's BeanStore) round-trips end-to-end to the decoded onResourceUpdated
				// callback.
				client.callTool("publish", Map.of("uri", "file:///a.txt"));
				assertEquals("updated:file:///a.txt", events.poll(15, TimeUnit.SECONDS));
			} finally {
				handle.close();
			}

			// Assertion 4: cleanup is write-failure/close-driven, not proactive (spec's "Transport/threading
			// detail") - the server only notices the client's disconnect on its next write attempt (heartbeat
			// or otherwise), so poll rather than assert immediately. Bounded poll loop, not a fixed sleep for
			// synchronization: each iteration's brief sleep is only the between-poll delay, gated by the outer
			// deadline.
			var deadline = System.currentTimeMillis() + 15_000;
			String countAfter;
			do {
				Thread.sleep(200);
				countAfter = ((TextContent) client.callTool("activeCount", Map.of()).getContent().get(0)).getText();
			} while (! "0".equals(countAfter) && System.currentTimeMillis() < deadline);
			assertEquals("0", countAfter);
		}
	}

	// =================================================================================================================
	// B: raw java.net.http.HttpClient + SseEventReader - validates the wire shape independent of the client facade.
	// =================================================================================================================

	@Test void b01_rawHttpClient_holdsOpenSseResponse_deliversAcknowledgedFirst() throws Exception {
		var listenId = "raw-1";
		// params._meta.protocolVersion/clientCapabilities are mandatory on every v2 request (see
		// McpRevision.validateMeta) - McpClient builds this automatically; the raw path must supply it itself.
		var body = "{\"jsonrpc\":\"2.0\",\"id\":\"" + listenId + "\",\"method\":\"subscriptions/listen\","
			+ "\"params\":{\"notifications\":{\"resourcesListChanged\":true},\"_meta\":{"
			+ "\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\",\"io.modelcontextprotocol/clientCapabilities\":{}}}}";
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + "/"))
			.header("Content-Type", "application/json")
			.header("Accept", "text/event-stream")
			// SEP-2243 Mcp-Method/Mcp-Name headers - McpRevision.dispatch() validates these unconditionally
			// before branching on the method (see the McpClient.listen() header fix, above); this raw request
			// bypasses McpClient entirely, so it must stamp them itself exactly as McpClient.listen() does.
			.header("Mcp-Method", McpMethods.SUBSCRIPTIONS_LISTEN)
			.header("Mcp-Name", "")
			.timeout(Duration.ofSeconds(15))
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofInputStream());
		assertEquals(200, resp.statusCode());
		try (var reader = new SseEventReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
			assertTrue(reader.hasNext());
			var first = reader.next();
			assertTrue(first.getData() != null && first.getData().contains("notifications/subscriptions/acknowledged"), "" + first.getData());
		}
	}

	// =================================================================================================================
	// C: heartbeat frames actually arrive over a real held-open HTTP connection while idle (assertion 3).
	// =================================================================================================================

	/**
	 * Opens a raw (client-facade-independent) SSE stream and, on a dedicated reader thread, counts named
	 * {@code "ping"} events with no {@code data:} payload arriving after the mandatory acknowledged frame - the
	 * fixture's tuned-down {@link #HEARTBEAT_INTERVAL_MS} means at least two must arrive well within the bounded
	 * wait below. The read itself is a blocking I/O call (not a sleep-based poll); the bound comes from a
	 * {@link CountDownLatch#await(long, TimeUnit)} racing the reader thread, and the connection is force-closed in
	 * {@code finally} to unblock that thread if the assertion path never reaches the expected count.
	 */
	@Test void c01_heartbeat_arrivesRepeatedlyOverLiveConnection_whileIdle() throws Exception {
		var body = "{\"jsonrpc\":\"2.0\",\"id\":\"hb-1\",\"method\":\"subscriptions/listen\","
			+ "\"params\":{\"notifications\":{},\"_meta\":{"
			+ "\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\",\"io.modelcontextprotocol/clientCapabilities\":{}}}}";
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + "/"))
			.header("Content-Type", "application/json")
			.header("Accept", "text/event-stream")
			// SEP-2243 Mcp-Method/Mcp-Name headers - see b01's comment for why the raw HTTP path must stamp
			// these itself.
			.header("Mcp-Method", McpMethods.SUBSCRIPTIONS_LISTEN)
			.header("Mcp-Name", "")
			.timeout(Duration.ofSeconds(20))
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofInputStream());
		assertEquals(200, resp.statusCode());

		var requiredPings = 2;
		var pingsSeen = new CountDownLatch(requiredPings);
		var failure = new AtomicReference<Throwable>();
		var readerThread = new Thread(() -> {
			try (var reader = new SseEventReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
				assertTrue(reader.hasNext());
				var first = reader.next();
				assertTrue(first.getData() != null && first.getData().contains("notifications/subscriptions/acknowledged"), first.getData());
				while (reader.hasNext() && pingsSeen.getCount() > 0) {
					var event = reader.next();
					if ("ping".equals(event.getEvent())) {
						assertNull(event.getData(), "a heartbeat frame must carry no data: payload");
						pingsSeen.countDown();
					}
				}
			} catch (Throwable t) {
				failure.set(t);
			}
		}, "mcp-heartbeat-integration-reader");
		readerThread.setDaemon(true);
		readerThread.start();
		try {
			assertTrue(pingsSeen.await(15, TimeUnit.SECONDS),
				"expected " + requiredPings + " heartbeat frame(s) over the live connection; saw "
					+ (requiredPings - pingsSeen.getCount()));
			if (failure.get() != null)
				throw new AssertionError(failure.get());
		} finally {
			resp.body().close();
			readerThread.join(5_000);
		}
	}

	// =================================================================================================================
	// Z: dead-client cleanup, driven from a raw (client-facade-independent) connection abort (assertion 4).
	// =================================================================================================================

	/**
	 * Complements a01's cleanup coverage (which closes via the {@code McpClient} facade's {@code handle.close()})
	 * with a facade-independent path: a raw connection whose body is closed without ever calling any client-side
	 * "unsubscribe" API, proving cleanup is driven by the server's own write-failure detection (P4/P5) rather than
	 * any client-initiated protocol message.
	 */
	@Test void z01_rawClientAbort_triggersServerCleanup_withoutAnyClientFacade() throws Exception {
		var body = "{\"jsonrpc\":\"2.0\",\"id\":\"z-1\",\"method\":\"subscriptions/listen\","
			+ "\"params\":{\"notifications\":{},\"_meta\":{"
			+ "\"io.modelcontextprotocol/protocolVersion\":\"2026-07-28\",\"io.modelcontextprotocol/clientCapabilities\":{}}}}";
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + "/"))
			.header("Content-Type", "application/json")
			.header("Accept", "text/event-stream")
			.header("Mcp-Method", McpMethods.SUBSCRIPTIONS_LISTEN)
			.header("Mcp-Name", "")
			.timeout(Duration.ofSeconds(20))
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		var resp = HTTP.send(req, BodyHandlers.ofInputStream());
		assertEquals(200, resp.statusCode());
		try (var reader = new SseEventReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
			assertTrue(reader.hasNext());
			reader.next(); // ack
		}
		resp.body().close();

		try (var probe = clientBuilder().build()) {
			var deadline = System.currentTimeMillis() + 15_000;
			String countAfter;
			do {
				Thread.sleep(200);
				countAfter = ((TextContent) probe.callTool("activeCount", Map.of()).getContent().get(0)).getText();
			} while (! "0".equals(countAfter) && System.currentTimeMillis() < deadline);
			assertEquals("0", countAfter);
		}
	}
}
