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
package org.apache.juneau.microbench.observability;

import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.server.metrics.*;
import org.apache.juneau.rest.server.tracing.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;
import org.openjdk.jmh.profile.*;
import org.openjdk.jmh.results.format.*;
import org.openjdk.jmh.runner.*;
import org.openjdk.jmh.runner.options.*;

/**
 * JMH micro-benchmark asserting the observability hot path in {@link org.apache.juneau.rest.server.RestOpInvoker}
 * is zero-allocation when both {@link MetricsRecorder} and {@link TracerHook} resolve to their
 * NoOp singletons.
 *
 * <p>
 * Mirrors the observability block inside {@code RestOpInvoker.invoke(opSession, observable=true)}:
 * </p>
 * <pre class='bjava'>
 * 	MetricsRecorder recorder = NoOpMetricsRecorder.INSTANCE;
 * 	TracerHook tracer = NoOpTracerHook.INSTANCE;
 * 	long startNanos = System.nanoTime();
 * 	Scope scope = tracer.startSpan(<jk>null</jk>);           <jc>// NoOp ignores request arg</jc>
 * 	<jk>try</jk> {
 * 		scope.setStatusCode(200);
 * 	} <jk>finally</jk> {
 * 		scope.close();
 * 		var elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
 * 		recorder.record(OP_NAME, HTTP_METHOD, URI_TEMPLATE, 200, elapsed, <jk>null</jk>);
 * 	}
 * </pre>
 *
 * <p>
 * Expected result: {@code gc.alloc.rate.norm} ≤ 8 bytes/op (zero under typical HotSpot
 * escape-analysis; the 8-byte epsilon absorbs JIT-on-warmup noise).
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py observability
 *
 * 	<jc># Or directly:</jc>
 * 	$ mvn -pl juneau-integration-tests -Pmicrobench test-compile exec:java \
 * 	      -Dexec.mainClass=org.apache.juneau.marshall.microbench.observability.ObservabilityNoopBenchmark
 * </pre>
 *
 * @since 10.0.0
 * @see MetricsRecorder
 * @see TracerHook
 * @see NoOpMetricsRecorder
 * @see NoOpTracerHook
 */
@State(org.openjdk.jmh.annotations.Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
// Class-level @Fork(0) is overridden by OptionsBuilder.forks() in main(); kept at 1 so IDE
// runners that auto-discover benchmarks get proper fork isolation.
@Fork(1)
@SuppressWarnings({
	"java:S8692" // Benchmark output filename intentionally stamps the real run date; fixing the clock would collide results.
})
public class ObservabilityNoopBenchmark {

	private static final String OP_NAME = "org.apache.juneau.rest.server.server.BenchResource.get()";
	private static final String HTTP_METHOD = "GET";
	private static final String URI_TEMPLATE = "/bench";
	private static final int STATUS_CODE = 200;

	// Interface-typed to match the production RestOpInvoker call pattern (virtual dispatch,
	// no static devirtualisation by the benchmarking harness).
	private MetricsRecorder recorder;
	private TracerHook tracer;

	/**
	 * Initialises the NoOp singleton references once per benchmark trial.
	 */
	@Setup(Level.Trial)
	public void setup() {
		recorder = NoOpMetricsRecorder.INSTANCE;
		tracer = NoOpTracerHook.INSTANCE;
	}

	/**
	 * Exercises the observability hot path using NoOp singletons.
	 *
	 * <p>
	 * The {@link Blackhole} parameter prevents the JIT from discarding the scope reference after
	 * {@link org.apache.juneau.rest.server.tracing.Scope#close()} — keeping the virtual-dispatch chain alive for accurate allocation
	 * measurement.
	 *
	 * @param bh JMH blackhole to prevent dead-code elimination.
	 */
	@Benchmark
	public void observabilityNoopBlock(Blackhole bh) {
		long startNanos = System.nanoTime();
		// NoOpTracerHook.startSpan() ignores the request arg; null is safe on the NoOp path.
		@SuppressWarnings({
			"java:S2637"  // Suppression required for test context; see annotation for details.
		})
		var scope = tracer.startSpan(null);
		try {
			scope.setStatusCode(STATUS_CODE);
		} finally {
			scope.close();
			var elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
			recorder.record(OP_NAME, HTTP_METHOD, URI_TEMPLATE, STATUS_CODE, elapsed, null, "", "");
		}
		bh.consume(scope);
	}

	/**
	 * Benchmark entry point.
	 *
	 * <p>
	 * Runs the benchmark with the GC allocation profiler ({@code -prof gc}) and writes
	 * results to {@code juneau-integration-tests/jmh-results/observability-YYYY-MM-DD.json}.
	 * Use {@code python3 scripts/microbench.py observability} for the recommended invocation,
	 * which reads the JSON output and asserts the allocation threshold.
	 *
	 * @param args Unused; JMH options are hard-coded for reproducibility.
	 * @throws RunnerException If JMH fails to run the benchmark.
	 */
	public static void main(String[] args) throws RunnerException {
		var dateTag = LocalDate.now().toString();
		var resultDir = Paths.get("juneau-integration-tests", "jmh-results");
		try {
			Files.createDirectories(resultDir);
		} catch (Exception e) {
			// Best-effort; JMH will fail with a clear message if the path is unwritable.
		}
		var jsonPath = resultDir.resolve("observability-" + dateTag + ".json").toString();

		var opt = new OptionsBuilder()
			.include(ObservabilityNoopBenchmark.class.getSimpleName())
			.warmupIterations(5)
			.warmupTime(TimeValue.seconds(1))
			.measurementIterations(5)
			.measurementTime(TimeValue.seconds(1))
			// forks(0): run in the same JVM as exec:java to avoid the forked process inheriting
			// an incomplete java.class.path (exec-maven-plugin loads via its own ClassLoader,
			// not via the system class path, so ForkedMain is unavailable to child processes).
			.forks(0)
			.addProfiler(GCProfiler.class)
			.resultFormat(ResultFormatType.JSON)
			.result(jsonPath)
			.build();

		new Runner(opt).run();
	}
}
