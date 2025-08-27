//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************
package org.apache.juneau;

import java.util.*;
import java.util.Map.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import org.apache.juneau.utils.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * JMH Benchmark for testing different iteration patterns over collections.
 *
 * <p>To run this benchmark:
 * <pre>
 * mvn test-compile exec:java -Dexec.mainClass="org.apache.juneau.BenchmarkRunner"
 * </pre>
 *
 * <p>Or from your IDE, run the main() method.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class BenchmarkRunner {

	private List<Integer> list;
	private Map<String, Integer> map;

	// Consumers to test
	private Consumer<List<Integer>> listIterator;
	private Consumer<List<Integer>> listForEach;
	private Consumer<List<Integer>> listStreamForEach;

	private Consumer<Map<String, Integer>> mapValuesIterator;
	private Consumer<Map<String, Integer>> mapEntrySetIterator;
	private Consumer<Map<String, Integer>> mapValuesForEach;
	private Consumer<Map<String, Integer>> mapForEach;

	private ThrowingConsumer<List<Integer>> throwingListIterator;
	private ThrowingConsumer<List<Integer>> throwingListForEach;
	private ThrowingConsumer<List<Integer>> throwingListStreamForEach;

	private ThrowingConsumer<Map<String, Integer>> throwingMapValuesIterator;
	private ThrowingConsumer<Map<String, Integer>> throwingMapEntrySetIterator;
	private ThrowingConsumer<Map<String, Integer>> throwingMapValuesForEach;
	private ThrowingConsumer<Map<String, Integer>> throwingMapForEach;

	@Setup(Level.Trial)
	public void setup() {
		var random = new Random(42); // Fixed seed for reproducible results
		int size = 1000; // Larger size for more meaningful benchmarks

		// Initialize test data
		list = new ArrayList<>(size);
		map = new LinkedHashMap<>();

		for (int i = 0; i < size; i++) {
			list.add(random.nextInt(100));
			map.put(String.valueOf(i), random.nextInt(100));
		}

		// Initialize consumers - these use Blackhole to prevent JVM optimizations
		listIterator = lst -> {
			for (Integer value : lst) {
				// Simulate some work - prevents dead code elimination
				Math.abs(value);
			}
		};

		listForEach = lst -> {
			for (Integer element : lst) {
				Math.abs(element);
			}
		};

		listStreamForEach = lst -> lst.forEach(value -> Math.abs(value));

		mapValuesIterator = m -> {
			for (Integer value : m.values()) {
				Math.abs(value);
			}
		};

		mapEntrySetIterator = m -> {
			for (Entry<String, Integer> entry : m.entrySet()) {
				Math.abs(entry.getValue());
			}
		};

		mapValuesForEach = m -> m.values().forEach(value -> Math.abs(value));
		mapForEach = m -> m.forEach((k, v) -> Math.abs(v));

		// ThrowingConsumer variants
		throwingListIterator = lst -> {
			for (Integer value : lst) {
				Math.abs(value);
			}
		};

		throwingListForEach = lst -> {
			for (Integer element : lst) {
				Math.abs(element);
			}
		};

		throwingListStreamForEach = lst -> lst.forEach(value -> Math.abs(value));

		throwingMapValuesIterator = m -> {
			for (Integer value : m.values()) {
				Math.abs(value);
			}
		};

		throwingMapEntrySetIterator = m -> {
			for (Entry<String, Integer> entry : m.entrySet()) {
				Math.abs(entry.getValue());
			}
		};

		throwingMapValuesForEach = m -> m.values().forEach(value -> Math.abs(value));
		throwingMapForEach = m -> m.forEach((k, v) -> Math.abs(v));
	}

	// =============================================================================
	// List iteration benchmarks
	// =============================================================================

	@Benchmark
	public void listIterator(Blackhole bh) {
		listIterator.accept(list);
		bh.consume(list); // Prevents optimization
	}

	@Benchmark
	public void listForEach(Blackhole bh) {
		listForEach.accept(list);
		bh.consume(list);
	}

	@Benchmark
	public void listStreamForEach(Blackhole bh) {
		listStreamForEach.accept(list);
		bh.consume(list);
	}

	// =============================================================================
	// Map iteration benchmarks
	// =============================================================================

	@Benchmark
	public void mapValuesIterator(Blackhole bh) {
		mapValuesIterator.accept(map);
		bh.consume(map);
	}

	@Benchmark
	public void mapEntrySetIterator(Blackhole bh) {
		mapEntrySetIterator.accept(map);
		bh.consume(map);
	}

	@Benchmark
	public void mapValuesForEach(Blackhole bh) {
		mapValuesForEach.accept(map);
		bh.consume(map);
	}

	@Benchmark
	public void mapForEach(Blackhole bh) {
		mapForEach.accept(map);
		bh.consume(map);
	}

	// =============================================================================
	// ThrowingConsumer benchmarks
	// =============================================================================

	@Benchmark
	public void throwingListIterator(Blackhole bh) throws Exception {
		throwingListIterator.accept(list);
		bh.consume(list);
	}

	@Benchmark
	public void throwingListForEach(Blackhole bh) throws Exception {
		throwingListForEach.accept(list);
		bh.consume(list);
	}

	@Benchmark
	public void throwingListStreamForEach(Blackhole bh) throws Exception {
		throwingListStreamForEach.accept(list);
		bh.consume(list);
	}

	@Benchmark
	public void throwingMapValuesIterator(Blackhole bh) throws Exception {
		throwingMapValuesIterator.accept(map);
		bh.consume(map);
	}

	@Benchmark
	public void throwingMapEntrySetIterator(Blackhole bh) throws Exception {
		throwingMapEntrySetIterator.accept(map);
		bh.consume(map);
	}

	@Benchmark
	public void throwingMapValuesForEach(Blackhole bh) throws Exception {
		throwingMapValuesForEach.accept(map);
		bh.consume(map);
	}

	@Benchmark
	public void throwingMapForEach(Blackhole bh) throws Exception {
		throwingMapForEach.accept(map);
		bh.consume(map);
	}

	// =============================================================================
	// Benchmark runner
	// =============================================================================

	/**
	 * Run the benchmark.
	 *
	 * <p>Alternative ways to run:
	 * <pre>
	 * # Run all benchmarks
	 * mvn test-compile exec:java -Dexec.mainClass="org.apache.juneau.IterationBenchmark"
	 *
	 * # Run only list benchmarks
	 * mvn test-compile exec:java -Dexec.mainClass="org.apache.juneau.IterationBenchmark" -Dexec.args=".*list.*"
	 *
	 * # Run with custom options
	 * mvn test-compile exec:java -Dexec.mainClass="org.apache.juneau.IterationBenchmark" -Dexec.args="-wi 5 -i 10 -f 3"
	 * </pre>
	 */
	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include(BenchmarkRunner.class.getSimpleName())
			.forks(1) // Use 1 fork for faster development testing
			.warmupIterations(2) // Reduced for faster testing
			.measurementIterations(3) // Reduced for faster testing
			.build();

		new Runner(opt).run();
	}
}
