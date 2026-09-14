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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Genuinely concurrent requests against <b>one shared</b> {@link ViewDef} instance &mdash; the realistic application
 * pattern of a static/field-held definition.
 *
 * <p>
 * Resolution mutates that shared instance in place and restores it, so these are the tests that matter: real threads
 * released from a common latch, many rounds, and a provider that deliberately widens the mutate/restore window so an
 * unguarded implementation would interleave.
 */
@SuppressWarnings({
	"resource",  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
	"java:S125" // Comments are explanatory; they are not commented-out code.
})
class ServerValuesHostsConcurrency_Test extends TestBase {

	private static final int THREADS = 8;
	private static final int ROUNDS = 12;

	/** Live occupancy of the shared view's resolve window, and the high-water mark across the run. */
	private static final AtomicInteger inWindow = new AtomicInteger();
	private static final AtomicInteger maxInWindow = new AtomicInteger();

	/**
	 * Reads this request's {@code env}, widening the window so a missing guard would interleave rather than
	 * accidentally serializing.
	 */
	@SuppressWarnings({
		"java:S2925" // deliberately widens the shared ViewDef's mutate/restore window so an unguarded impl would interleave - the race this test exists to catch.
	})
	private static String markFor(VarResolverSession s) {
		var live = inWindow.incrementAndGet();
		maxInWindow.accumulateAndGet(live, Math::max);
		try {
			Thread.sleep(1);
			return s.getBean(RestRequest.class).map(r -> r.getQueryParam("env").orElse("?")).orElse("?");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		} finally {
			inWindow.decrementAndGet();
		}
	}

	/** The one shared, field-held view every thread renders concurrently. */
	static final ViewDef VIEW = ViewDef.create("shared")
		.columns(Column.of("name").title("TAB:$FV{env}"))
		.serverValues(ServerValues.create().value("env", ServerValuesHostsConcurrency_Test::markFor))
		.build();

	@Rest(mixins=ViewsMixin.class)
	public static class ServerValuesHostsConcurrencyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/view") public HttpResource view(RestRequest req) {
			return HttpResourceBean.of(
				ByteArrayBody.of(Html.of(ViewTable.of(req, VIEW)).getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static List<String> authorTemplates() {
		return list(VIEW.columns.get(0).title);
	}

	@Test void a01_simultaneousRequestsOnOneSharedViewDefResolveIndependently() throws Exception {
		var before = authorTemplates();
		var pool = Executors.newFixedThreadPool(THREADS);
		var start = new CountDownLatch(1);
		var failures = new CopyOnWriteArrayList<String>();
		var responses = new AtomicInteger();
		try {
			var futures = new ArrayList<Future<?>>();
			for (var i = 0; i < THREADS; i++) {
				var mine = "T" + i;
				var client = MockRestClient.buildLax(ServerValuesHostsConcurrencyHost.class);
				futures.add(pool.submit(() -> {
					await(start);
					for (var r = 0; r < ROUNDS; r++) {
						try {
							var html = client.get("/view?env=" + mine).run().assertStatus(200).getContent().asString();
							responses.incrementAndGet();
							check(failures, html, mine);
						} catch (Exception e) {
							failures.add(mine + " round " + r + ": " + e);
						}
					}
				}));
			}
			start.countDown();
			for (var f : futures)
				f.get(120, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}

		assertTrue(failures.isEmpty(), () -> "concurrent requests on one shared ViewDef interfered:\n" + String.join("\n", failures));
		assertEquals(THREADS * ROUNDS, responses.get(), "every request must have completed");
		assertEquals(before, authorTemplates(), "the shared ViewDef must be restored to its author templates");

		assertTrue(maxInWindow.get() > 0, "the concurrency run must have exercised the resolve window");
		assertEquals(1, maxInWindow.get(),
			"two responses were inside the same shared ViewDef's mutate/restore window simultaneously");
	}

	private static void check(List<String> failures, String html, String mine) {
		for (var field : list("TAB")) {
			if (! html.contains(field + ":" + mine))
				failures.add(mine + ": missing own " + field);
			for (var t = 0; t < THREADS; t++) {
				var other = "T" + t;
				if (! other.equals(mine) && html.contains(field + ":" + other))
					failures.add(mine + ": observed " + field + ":" + other);
			}
		}
		if (html.contains("$FV{"))
			failures.add(mine + ": an unresolved template reached the response");
	}

	private static void await(CountDownLatch l) {
		try {
			l.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
