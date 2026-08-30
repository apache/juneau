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
 * Genuinely concurrent requests against <b>one shared</b> {@link PageDef} instance &mdash; the realistic application
 * pattern of a static/field-held definition.
 *
 * <p>
 * Resolution mutates that shared instance in place and restores it, so these are the tests that matter: real threads
 * released from a common latch, many rounds, and a provider that deliberately widens the mutate/restore window so an
 * unguarded implementation would interleave.  A sequential simulation cannot fail either way &mdash; it never puts two
 * resolve windows on the same object at the same time, which is the only condition under which a lost restore or a
 * cross-response leak is observable.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class ServerValuesHostsConcurrency_Test extends TestBase {

	private static final int THREADS = 8;
	private static final int ROUNDS = 12;

	/** Live occupancy of the shared page's resolve window, and the high-water mark across the run. */
	private static final AtomicInteger inWindow = new AtomicInteger();
	private static final AtomicInteger maxInWindow = new AtomicInteger();

	/**
	 * Reads this request's {@code env}, widening the window so a missing guard would interleave rather than
	 * accidentally serializing.
	 */
	@SuppressWarnings({
		"java:S2925" // deliberately widens the shared PageDef's mutate/restore window so an unguarded impl would interleave - the race this test exists to catch (case d).
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

	static final ViewDef VIEW = ViewDef.create("shared")
		.columns(Column.of("name").title("Name"))
		.build();

	/** The one shared, field-held page every thread renders concurrently. */
	static final PageDef PAGE = PageDef.create("shared-page")
		.title("MARK:$FV{env}")
		.tabs(
			Tab.create("a", "TAB:$FV{env}").view(VIEW),
			Tab.create("b", "TAB2:$FV{env}").subtabs(
				Subtab.create("s", "SUB:$FV{env}").view(
					ViewDef.create("shared-sub").columns(Column.of("name").title("Name")).build())))
		.header(AppHeaderDef.create("app")
			.brand(Brand.create().title("BRAND:$FV{env}").crumbs("CRUMB:$FV{env}"))
			.avatar(AvatarChip.of("AVATAR:$FV{env}").initials("AV:$FV{env}"))
			.build())
		.serverValues(ServerValues.create().value("env", ServerValuesHostsConcurrency_Test::markFor))
		.build();

	@Rest(mixins=ViewsMixin.class)
	public static class ServerValuesHostsConcurrencyHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/page") public HttpResource page(RestRequest req) {
			return HttpResourceBean.of(
				ByteArrayBody.of(Html.of(PageTable.of(req, PAGE)).getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	/** The author templates that must survive every round untouched. */
	private static List<String> authorTemplates() {
		var out = new ArrayList<String>();
		out.add(PAGE.title);
		for (var t : PAGE.tabs) {
			out.add(t.label);
			if (t.subtabs != null)
				for (var s : t.subtabs)
					out.add(s.label);
		}
		out.add(PAGE.header.brand.title);
		out.addAll(PAGE.header.brand.crumbs);
		out.add(PAGE.header.avatar.displayName);
		out.add(PAGE.header.avatar.initials);
		return out;
	}

	@Test void a01_simultaneousRequestsOnOneSharedPageDefResolveIndependently() throws Exception {
		var before = authorTemplates();
		var pool = Executors.newFixedThreadPool(THREADS);
		var start = new CountDownLatch(1);
		var failures = new CopyOnWriteArrayList<String>();
		var responses = new AtomicInteger();
		try {
			var futures = new ArrayList<Future<?>>();
			for (var i = 0; i < THREADS; i++) {
				var mine = "T" + i;
				// One client per thread so the only shared mutable state under test is the PageDef itself.
				var client = MockRestClient.buildLax(ServerValuesHostsConcurrencyHost.class);
				futures.add(pool.submit(() -> {
					await(start);
					for (var r = 0; r < ROUNDS; r++) {
						try {
							var html = client.get("/page?env=" + mine).run().assertStatus(200).getContent().asString();
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

		// Failures first: they carry the reason (a leak, an unresolved template, or a request that threw), where the
		// response count only reports that something went wrong.
		assertTrue(failures.isEmpty(), () -> "concurrent requests on one shared PageDef interfered:\n" + String.join("\n", failures));
		assertEquals(THREADS * ROUNDS, responses.get(), "every request must have completed");
		assertEquals(before, authorTemplates(), "the shared PageDef must be restored to its author templates");

		// Every provider call above ran inside the shared def's mutate/restore window and counted its own occupancy,
		// so the high-water mark is a direct measurement of whether that window is mutually exclusive.
		assertTrue(maxInWindow.get() > 0, "the concurrency run must have exercised the resolve window");
		assertEquals(1, maxInWindow.get(),
			"two responses were inside the same shared PageDef's mutate/restore window simultaneously");
	}

	/** Asserts a response carries only its OWN resolved chrome, on every allowlisted page field. */
	private static void check(List<String> failures, String html, String mine) {
		for (var field : list("MARK", "TAB", "TAB2", "SUB", "BRAND", "CRUMB", "AVATAR", "AV")) {
			// MARK is PageDef.title, which the shell paints nowhere - skip its presence check, keep its leak check.
			if (! "MARK".equals(field) && ! html.contains(field + ":" + mine))
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
