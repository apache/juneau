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
package org.apache.juneau.rest.server.metrics.micrometer;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.*;

/**
 * Tests for the {@code /metrics} management endpoint (mixin + resource flavors + the {@link MetricsManager}
 * worker), covering the Prometheus-scrape happy path and the clean degradation when no scrapeable registry
 * is present.
 */
@SuppressWarnings({
	"resource" // Closeable MockRestClient fixtures; lifecycle managed by the test/framework, not a real leak.
})
class Metrics_Test extends TestBase {

	/**
	 * A minimal scrapeable registry standing in for a {@code PrometheusMeterRegistry} (which lives in a
	 * separate artifact this module does not depend on).  {@link MetricsManager#scrape} finds {@code scrape()}
	 * reflectively, so any registry exposing a no-arg {@code scrape()} is treated as scrapeable.
	 */
	public static class ScrapeableRegistry extends SimpleMeterRegistry {
		@SuppressWarnings("unused")
		public String scrape() {
			return "# HELP demo_total Demo.\n# TYPE demo_total counter\ndemo_total 1.0\n";
		}
	}

	/** A registry whose {@code scrape()} returns {@code null} — exercises the null-result branch. */
	public static class NullScrapeRegistry extends SimpleMeterRegistry {
		@SuppressWarnings("unused")
		public String scrape() {
			return null;
		}
	}

	/** A registry whose {@code scrape()} throws — exercises the reflective-failure rethrow branch. */
	public static class ThrowingScrapeRegistry extends SimpleMeterRegistry {
		@SuppressWarnings("unused")
		public String scrape() {
			throw new IllegalStateException("boom");
		}
	}

	// =================================================================================
	// A. MetricsManager worker
	// =================================================================================

	@Test void a01_scrapeReflective() {
		var m = new MetricsManager();
		assertTrue(m.scrape(new ScrapeableRegistry()).contains("demo_total"));
	}

	@Test void a02_scrapeNonScrapeableNull() {
		// A plain SimpleMeterRegistry has no scrape() method -> null (caller degrades to 501).
		assertNull(new MetricsManager().scrape(new SimpleMeterRegistry()));
	}

	@Test void a03_scrapeNullRegistryNull() {
		assertNull(new MetricsManager().scrape(null));
	}

	@Test void a04_resolveNullContextNull() {
		assertNull(new MetricsManager().resolveRegistry(null));
	}

	@Test void a05_scrapeNullResultNull() {
		// A scrapeable registry whose scrape() returns null -> manager returns null (caller degrades to 501).
		assertNull(new MetricsManager().scrape(new NullScrapeRegistry()));
	}

	@Test void a06_scrapeThrowsWrapped() {
		// A reflective scrape() failure is rethrown wrapped, not swallowed.
		var m = new MetricsManager();
		var r = new ThrowingScrapeRegistry();
		var e = assertThrows(RuntimeException.class, () -> m.scrape(r));
		assertTrue(e.getMessage().contains("Failed to scrape"));
	}

	// =================================================================================
	// B. Mixin flavor — scrape present
	// =================================================================================

	@Rest(mixins={MetricsMixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public MeterRegistry registry() { return new ScrapeableRegistry(); }
	}

	@Test void b01_mixinRendersScrape() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/metrics").run().assertStatus(200).assertContent().asString().isContains("demo_total");
	}

	// =================================================================================
	// C. Degrade cleanly — non-scrapeable registry and no registry both yield 501
	// =================================================================================

	@Rest(mixins={MetricsMixin.class})
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public MeterRegistry registry() { return new SimpleMeterRegistry(); }
	}

	@Test void c01_nonScrapeableRegistry501() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/metrics").run().assertStatus(501);
	}

	@Rest(mixins={MetricsMixin.class})
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void c02_noRegistry501() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		c.get("/metrics").run().assertStatus(501);
	}

	// =================================================================================
	// D. Resource flavor
	// =================================================================================

	// A routed child resolves beans from its own bean store (not the parent host's), so the registry bean
	// is declared on the child subclass — mirroring the HealthResource child-flavor test precedent.
	@Rest(path="/metrics")
	public static class MetricsChild extends MetricsResource {
		@Bean public MeterRegistry registry() { return new ScrapeableRegistry(); }
	}

	@Rest(children={MetricsChild.class})
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d01_resourceRendersScrape() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		c.get("/metrics").run().assertStatus(200).assertContent().asString().isContains("demo_total");
	}
}
