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
package org.apache.juneau.microservice;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Cold/warm boot-time benchmark for the Juneau {@link Microservice} + {@link JettyConfiguration} stack.
 *
 * <p>
 * Not a real {@code _Test} — the class name does not match the surefire {@code **&#47;*Test.class} include pattern, so
 * it will not run as part of a normal {@code mvn test} build.  Invoke explicitly:
 *
 * <pre>
 *   mvn -pl juneau-utest test -Dtest=BootTimeBenchmark -Drat.skip=true
 * </pre>
 *
 * <p>
 * Or compile and run the {@link #main(String[])} method directly.
 *
 * <p>
 * What it measures, for both a 1-servlet and a 10-servlet config:
 * <ul>
 *   <li><b>Build phase:</b> {@code Microservice.create()...build()} — config load, bean-store wiring,
 *       {@code @Configuration} class scanning, {@link Microservice#init()}.</li>
 *   <li><b>Start phase:</b> {@code microservice.start()} — Jetty server build (or reuse), servlet mount, port
 *       bind via {@code Server.start()}.</li>
 * </ul>
 *
 * <p>
 * Each (servlet-count) variant runs 1 cold iteration + 5 warm iterations.  Output goes to stdout with the prefix
 * {@code BOOTBENCH:} so it can be greped out of surefire console output.
 */
@SuppressWarnings({
	"java:S5786", // Manual benchmark harness, not a real JUnit test — public visibility is intentional.
	"java:S2699", // Manual benchmark harness — it measures boot timings and has no assertions by design.
	"java:S3577"  // Manual benchmark harness — name intentionally excluded from the surefire *Test include pattern.
})
public class BootTimeBenchmark {

	private static final int WARM_ITERS = 5;
	private static final int TOTAL_ITERS = 1 + WARM_ITERS; // first is cold

	// ------------------------------------------------------------------------------------------------
	// Test servlets — each needs its own @Rest(path=...) so JettyServerComponent can mount distinct
	// pathSpecs without collision.  BasicRestServlet supplies the standard universal-config behavior.
	// ------------------------------------------------------------------------------------------------

	@Rest(path = "/s0") public static class S0 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s1") public static class S1 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s2") public static class S2 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s3") public static class S3 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s4") public static class S4 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s5") public static class S5 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s6") public static class S6 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s7") public static class S7 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s8") public static class S8 extends BasicRestServlet { private static final long serialVersionUID = 1L; }
	@Rest(path = "/s9") public static class S9 extends BasicRestServlet { private static final long serialVersionUID = 1L; }

	// ------------------------------------------------------------------------------------------------
	// @Configuration classes — JettyServerComponent auto-discovers every @Bean Servlet whose runtime
	// class carries @Rest and mounts it at @Rest(path=...).
	// ------------------------------------------------------------------------------------------------

	@Configuration
	public static class OneServletConfig {
		@Bean public Servlet s0() { return new S0(); }
	}

	@Configuration
	public static class TenServletsConfig {
		@Bean(name = "s0") public Servlet s0() { return new S0(); }
		@Bean(name = "s1") public Servlet s1() { return new S1(); }
		@Bean(name = "s2") public Servlet s2() { return new S2(); }
		@Bean(name = "s3") public Servlet s3() { return new S3(); }
		@Bean(name = "s4") public Servlet s4() { return new S4(); }
		@Bean(name = "s5") public Servlet s5() { return new S5(); }
		@Bean(name = "s6") public Servlet s6() { return new S6(); }
		@Bean(name = "s7") public Servlet s7() { return new S7(); }
		@Bean(name = "s8") public Servlet s8() { return new S8(); }
		@Bean(name = "s9") public Servlet s9() { return new S9(); }
	}

	// ------------------------------------------------------------------------------------------------
	// Timing record.
	// ------------------------------------------------------------------------------------------------

	private record Sample(double buildMs, double startMs) {
		double totalMs() { return buildMs + startMs; }
	}

	// ------------------------------------------------------------------------------------------------
	// JUnit entry point — invoke via:
	//   mvn -pl juneau-utest test -Dtest=BootTimeBenchmark -Drat.skip=true
	// ------------------------------------------------------------------------------------------------

	@Test
	public void measure() throws Exception {
		runAndReport();
	}

	public static void main(String[] args) throws Exception {
		runAndReport();
	}

	private static void runAndReport() throws Exception {
		var hdr = "===== BootTimeBenchmark — Microservice + JettyConfiguration cold/warm boot =====";
		println(hdr);
		println("JDK: " + System.getProperty("java.runtime.version") + "  on " + System.getProperty("os.name"));
		println("Iterations per variant: 1 cold + " + WARM_ITERS + " warm");
		println("");

		var oneServlet = bench("1 servlet", OneServletConfig.class);
		var tenServlets = bench("10 servlets", TenServletsConfig.class);

		println("");
		println("===== Summary =====");
		summarize("1 servlet", oneServlet);
		summarize("10 servlets", tenServlets);
		println("");

		// Linearity check — how much does the 10-servlet warm median exceed 1-servlet warm median?
		var w1 = warmMedian(oneServlet);
		var w10 = warmMedian(tenServlets);
		println(String.format("BOOTBENCH: scaling: warm median 1-servlet=%.1fms, 10-servlets=%.1fms, delta=%.1fms (~%.2f ms/servlet)",
			w1, w10, w10 - w1, (w10 - w1) / 9.0));
	}

	private static Sample[] bench(String label, Class<?> userConfig) throws Exception {
		println("BOOTBENCH: --- variant: " + label + " ---");
		var samples = new Sample[TOTAL_ITERS];
		for (var i = 0; i < TOTAL_ITERS; i++) {
			var s = oneBoot(userConfig);
			samples[i] = s;
			var tag = (i == 0) ? "COLD" : "warm#" + i;
			println(String.format("BOOTBENCH: [%s] %s  build=%.1fms  start=%.1fms  total=%.1fms",
				label, tag, s.buildMs(), s.startMs(), s.totalMs()));
		}
		return samples;
	}

	private static Sample oneBoot(Class<?> userConfig) throws Exception {
		var classes = new ArrayList<Class<?>>();
		classes.add(userConfig);
		classes.add(MicroserviceTestFixture.EphemeralJettyServerConfig.class);
		classes.add(JettyConfiguration.class);

		var t0 = System.nanoTime();
		var ms = Microservice.create()
			.configurations(classes.toArray(new Class<?>[0]))
			.build();
		var t1 = System.nanoTime();
		ms.start();
		var t2 = System.nanoTime();
		try {
			return new Sample(nsToMs(t1 - t0), nsToMs(t2 - t1));
		} finally {
			// Always stop so the bound port is released for the next iteration.
			ms.stop();
		}
	}

	// ------------------------------------------------------------------------------------------------
	// Stats helpers.
	// ------------------------------------------------------------------------------------------------

	private static double nsToMs(long ns) { return ns / 1_000_000.0; }

	private static void summarize(String label, Sample[] s) {
		var cold = s[0];
		var warmBuild = warmMedian(s, true /*build*/);
		var warmStart = warmMedian(s, false /*start*/);
		var warmTotal = warmMedian(s);
		println(String.format("BOOTBENCH: %s : cold total=%.1fms (build=%.1fms, start=%.1fms) | warm-median total=%.1fms (build=%.1fms, start=%.1fms)",
			label, cold.totalMs(), cold.buildMs(), cold.startMs(),
			warmTotal, warmBuild, warmStart));
	}

	private static double warmMedian(Sample[] s) {
		return median(warmValues(s, Sample::totalMs));
	}

	private static double warmMedian(Sample[] s, boolean build) {
		return median(warmValues(s, build ? Sample::buildMs : Sample::startMs));
	}

	private static double[] warmValues(Sample[] s, java.util.function.ToDoubleFunction<Sample> fn) {
		// Skip index 0 (cold).
		var out = new double[s.length - 1];
		for (var i = 1; i < s.length; i++) out[i - 1] = fn.applyAsDouble(s[i]);
		return out;
	}

	private static double median(double[] xs) {
		var copy = xs.clone();
		Arrays.sort(copy);
		var n = copy.length;
		return (n % 2 == 1) ? copy[n / 2] : (copy[n / 2 - 1] + copy[n / 2]) / 2.0;
	}

	private static void println(String s) {
		System.out.println(s);
		System.out.flush();
	}
}
