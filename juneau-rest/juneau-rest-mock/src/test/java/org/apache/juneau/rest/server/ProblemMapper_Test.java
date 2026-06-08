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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.rfc7807.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end tests for {@link ProblemMapper} bean-store discovery and the {@link ProblemLocalizationStrategy}
 * future-work seam in {@code ProblemDetailsProcessor}.
 *
 * <p>
 * Covers four behaviors:
 * <ul>
 * 	<li>A custom {@link ProblemMapper} registered via {@code @Bean} translates a thrown domain exception into a
 * 		{@link Problem} with a custom {@code type} URI and extension fields.
 * 	<li>A {@link ProblemMapper} for {@code BasicHttpException} replaces the default
 * 		{@code ProblemAdapters.fromException(...)} adaptation.
 * 	<li>When no mapper matches, the default {@code ProblemAdapters.fromException(BasicHttpException)} fallback is
 * 		still used (Phase 1 regression bar).
 * 	<li>The localization seam: a registered {@link ProblemLocalizationStrategy} is consulted, and a no-op default
 * 		passes the {@link Problem} through unchanged.
 * </ul>
 */
@SuppressWarnings("resource")  // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
class ProblemMapper_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Shared domain types.
	// -----------------------------------------------------------------------------------------------------------------

	/** A custom RuntimeException not derived from {@code BasicHttpException}. */
	public static class InsufficientCreditException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final int balance;
		private final int cost;

		public InsufficientCreditException(int balance, int cost) {
			super("Balance " + balance + " < cost " + cost);
			this.balance = balance;
			this.cost = cost;
		}

		public int getBalance() { return balance; }
		public int getCost() { return cost; }
	}

	public static class InsufficientCreditMapper implements ProblemMapper<InsufficientCreditException> {
		@Override
		public Class<InsufficientCreditException> getExceptionType() { return InsufficientCreditException.class; }
		@Override
		public Problem map(InsufficientCreditException e) {
			return Problem.fromStatus(403, "Insufficient credit", e.getMessage())
				.setType(URI.create("https://example.com/probs/out-of-credit"))
				.setInstance(URI.create("/account/12345/msgs/abc"))
				.set("balance", e.getBalance())
				.set("cost", e.getCost());
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: Custom mapper for a domain exception — full custom Problem with type URI + extension fields.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class A {
		@Bean public ProblemMapper<InsufficientCreditException> creditMapper() {
			return new InsufficientCreditMapper();
		}
		@RestGet
		public String buy() { throw new InsufficientCreditException(30, 50); }
	}

	@Test
	void a01_customMapper_emitsCustomTypeAndExtensions() throws Exception {
		var a = MockRestClient.buildLax(A.class);
		a.get("/buy")
			.run()
			.assertStatus(403)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains(
				"\"status\":403",
				"\"title\":\"Insufficient credit\"",
				"\"detail\":\"Balance 30 < cost 50\"",
				"\"type\":\"https://example.com/probs/out-of-credit\"",
				"\"instance\":\"/account/12345/msgs/abc\"",
				"\"balance\":30",
				"\"cost\":50");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: Custom mapper for BasicHttpException subclass — replaces the default ProblemAdapters fallback.
	// -----------------------------------------------------------------------------------------------------------------

	public static class B_CustomNotFoundMapper implements ProblemMapper<NotFound> {
		@Override
		public Class<NotFound> getExceptionType() { return NotFound.class; }
		@Override
		public Problem map(NotFound e) {
			return Problem.fromStatus(404, "Order not found", e.getMessage())
				.setType(URI.create("https://example.com/probs/missing-order"))
				.set("supportTicket", "T-12345");
		}
	}

	@Rest(problemDetails="true")
	public static class B {
		@Bean public ProblemMapper<NotFound> nfMapper() { return new B_CustomNotFoundMapper(); }
		@RestGet("/order/{id}")
		public String order(@org.apache.juneau.http.Path("id") int id) {
			throw new NotFound("Order {0} missing", id);
		}
	}

	@Test
	void b01_mapperOverridesDefaultAdapterForBasicHttpException() throws Exception {
		var b = MockRestClient.buildLax(B.class);
		b.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains(
				"\"status\":404",
				"\"title\":\"Order not found\"",
				"\"detail\":\"Order 42 missing\"",
				"\"type\":\"https://example.com/probs/missing-order\"",
				"\"supportTicket\":\"T-12345\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: Default fallback path — with no registered mapper, ProblemAdapters.fromException is still used.
	//    (Phase 1 regression bar: the new mapper-discovery path doesn't displace the default adapter.)
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class C {
		@RestGet("/order/{id}")
		public String order(@org.apache.juneau.http.Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void c01_defaultAdapterFallback_whenNoMapperRegistered() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":404", "\"title\":\"Not Found\"", "\"detail\":\"Order 42 not found\"")
			.assertContent().isNotContains("\"type\"", "\"instance\"", "supportTicket");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// D: Most-specific mapper wins — when both a subclass-targeted mapper and a superclass-targeted mapper are
	//    registered, the subclass mapper is selected for thrown subclass instances.
	// -----------------------------------------------------------------------------------------------------------------

	public static class D_BasicHttpMapper implements ProblemMapper<BasicHttpException> {
		@Override
		public Class<BasicHttpException> getExceptionType() { return BasicHttpException.class; }
		@Override
		public Problem map(BasicHttpException e) {
			return Problem.fromStatus(e.getStatusCode(), "Generic HTTP failure", e.getMessage()).set("by", "broad");
		}
	}

	public static class D_NotFoundMapper implements ProblemMapper<NotFound> {
		@Override
		public Class<NotFound> getExceptionType() { return NotFound.class; }
		@Override
		public Problem map(NotFound e) {
			return Problem.fromStatus(404, "Specific NotFound", e.getMessage()).set("by", "specific");
		}
	}

	@Rest(problemDetails="true")
	public static class D {
		// Multiple mappers must be aggregated through a ProblemMapperList so the bean-store walk doesn't
		// collapse them onto the single ProblemMapper.class slot.
		@Bean public ProblemMapperList problemMappers() {
			return ProblemMapperList.of(new D_BasicHttpMapper(), new D_NotFoundMapper());
		}
		@RestGet("/nf")
		public String nf() { throw new NotFound("missing"); }
	}

	@Test
	void d01_mostSpecificMapperWins() throws Exception {
		var d = MockRestClient.buildLax(D.class);
		d.get("/nf")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"title\":\"Specific NotFound\"", "\"by\":\"specific\"")
			.assertContent().isNotContains("\"by\":\"broad\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E: Mapper returning null defers to next-most-specific mapper (and ultimately the default adapter).
	// -----------------------------------------------------------------------------------------------------------------

	public static class E_AlwaysAbstainMapper implements ProblemMapper<NotFound> {
		@Override
		public Class<NotFound> getExceptionType() { return NotFound.class; }
		@Override
		public Problem map(NotFound e) { return null; }
	}

	@Rest(problemDetails="true")
	public static class E {
		@Bean public ProblemMapper<NotFound> abstain() { return new E_AlwaysAbstainMapper(); }
		@RestGet("/order/{id}")
		public String order(@org.apache.juneau.http.Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void e01_nullReturnFallsThroughToDefaultAdapter() throws Exception {
		var e = MockRestClient.buildLax(E.class);
		e.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"status\":404", "\"title\":\"Not Found\"", "\"detail\":\"Order 42 not found\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// E2: Mapper returning null falls through to the next-most-specific mapper in a ProblemMapperList chain.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class E2 {
		@Bean public ProblemMapperList problemMappers() {
			return ProblemMapperList.of(new E_AlwaysAbstainMapper(), new D_BasicHttpMapper());
		}
		@RestGet("/nf")
		public String nf() { throw new NotFound("missing"); }
	}

	@Test
	void e02_nullReturnChainsToNextMapperInList() throws Exception {
		var e2 = MockRestClient.buildLax(E2.class);
		e2.get("/nf")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"title\":\"Generic HTTP failure\"", "\"by\":\"broad\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// F: Localization seam (Q8 scaffolding-only) — verifies the strategy is consulted with the request locale.
	//    Q8 itself (full Messages-driven localization) remains out of scope; this test only exercises the seam.
	// -----------------------------------------------------------------------------------------------------------------

	private static final java.util.concurrent.atomic.AtomicReference<Locale> CAPTURED_LOCALE = new java.util.concurrent.atomic.AtomicReference<>();

	public static class F_TaggingLocalizationStrategy implements ProblemLocalizationStrategy {
		@Override
		public Problem localize(Problem problem, Locale locale) {
			CAPTURED_LOCALE.set(locale);
			problem.set("locale", locale == null ? null : locale.toLanguageTag());
			return problem;
		}
	}

	@Rest(problemDetails="true")
	public static class F {
		@Bean public ProblemLocalizationStrategy loc() { return new F_TaggingLocalizationStrategy(); }
		@RestGet("/order/{id}")
		public String order(@org.apache.juneau.http.Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void f01_localizationStrategy_consultedWithRequestLocale() throws Exception {
		CAPTURED_LOCALE.set(null);
		var f = MockRestClient.buildLax(F.class);
		f.get("/order/42")
			.header("Accept-Language", "fr-CA")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isContains("\"locale\":\"fr-CA\"");
		// Sanity: the strategy ran at least once.
		assertNotNull(CAPTURED_LOCALE.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// G: Default localization seam is no-op (identity) when no strategy bean is registered — Problem flows through
	//    unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(problemDetails="true")
	public static class G {
		@RestGet("/order/{id}")
		public String order(@org.apache.juneau.http.Path("id") int id) {
			throw new NotFound("Order {0} not found", id);
		}
	}

	@Test
	void g01_noStrategyRegistered_problemPassesThroughUnchanged() throws Exception {
		var g = MockRestClient.buildLax(G.class);
		g.get("/order/42")
			.run()
			.assertStatus(404)
			.assertHeader("Content-Type").isContains("application/problem+json")
			.assertContent().isNotContains("\"locale\"");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// H: IDENTITY constant returns the input unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_identityStrategy_returnsInputUnchanged() {
		var p = Problem.fromStatus(404, "Not Found", "Gone").set("k", "v");
		var result = ProblemLocalizationStrategy.IDENTITY.localize(p, Locale.US);
		assertSame(p, result);
	}

	@Test
	void h02_identityStrategy_acceptsNullLocale() {
		var p = new Problem().setTitle("t");
		var result = ProblemLocalizationStrategy.IDENTITY.localize(p, null);
		assertSame(p, result);
	}
}
