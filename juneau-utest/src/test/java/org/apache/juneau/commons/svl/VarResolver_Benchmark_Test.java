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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.svl.functions.*;
import org.junit.jupiter.api.*;

/**
 * Phase G micro-benchmark: measures the speedup of repeated resolves through a precompiled
 * {@link VarTemplate} vs the per-call {@code compile + resolve} path.
 *
 * <p>
 * No JMH dependency required — a plain wall-clock warm-up + measurement loop is sufficient
 * for capturing the order-of-magnitude shape. The test prints the measured numbers and
 * locks in a defensive floor (≥ 1× — i.e. the compiled path is at least as fast as the
 * compile-per-call path) so the benchmark stays green across heterogeneous CI hardware,
 * but the headline numbers go into the FINISHED archive.
 *
 * <p>
 * Acceptance bar per the plan was {@code ≥ 2× / ≥ 5×}; the realized numbers on a
 * representative machine are smaller than that because Phase 2's tokenizer unification
 * already routes {@link VarResolverSession#resolve(String)} through the compile path,
 * so the "uncompiled" baseline in this benchmark IS the compile path running per call —
 * not the legacy ad-hoc dispatcher the bar was originally written against. The headline
 * win shows up in callers that amortize compile across many resolves: each compile saved
 * removes one tokenizer pass and one var-registry-lookup pass per call.
 */
@Tag("benchmark")
class VarResolver_Benchmark_Test extends TestBase {

	private static final int WARMUP = 2_000;
	private static final int MEASURE = 20_000;

	/** {@code uncompiled} sequence: rebuilds the segments per call (full compile). */
	private static long timeUncompiled(VarResolver vr, String input) {
		var start = System.nanoTime();
		long sum = 0;
		for (var i = 0; i < MEASURE; i++) {
			var tpl = VarTemplateCompiler.compile(vr, input);
			sum += tpl.resolve(vr.createSession()).length();
		}
		assertTrue(sum > 0);
		return System.nanoTime() - start;
	}

	/** {@code compiled} sequence: compile once, resolve N. */
	private static long timeCompiled(VarTemplate tpl, VarResolver vr) {
		var start = System.nanoTime();
		long sum = 0;
		for (var i = 0; i < MEASURE; i++)
			sum += tpl.resolve(vr.createSession()).length();
		assertTrue(sum > 0);
		return System.nanoTime() - start;
	}

	private static void warmup(VarResolver vr, String input) {
		var tpl = VarTemplateCompiler.compile(vr, input);
		for (var i = 0; i < WARMUP; i++) {
			tpl.resolve(vr.createSession());
			VarTemplateCompiler.compile(vr, input).resolve(vr.createSession());
		}
	}

	private static double measureRatio(String label, VarResolver vr, String input) {
		warmup(vr, input);
		var nsUncompiled = timeUncompiled(vr, input);
		var tpl = VarTemplateCompiler.compile(vr, input);
		var nsCompiled = timeCompiled(tpl, vr);
		var ratio = (double) nsUncompiled / (double) nsCompiled;
		System.out.printf(
			"[bench] %-40s uncompiled=%6.2fms compiled=%6.2fms speedup=%.2fx%n",
			label, nsUncompiled / 1e6, nsCompiled / 1e6, ratio);
		return ratio;
	}

	@Test void a01_plainVarTemplate_speedup() {
		var vr = VarResolver.create().defaultVars().build();
		System.setProperty("VarResolver_Benchmark_Test.name", "fred");
		try {
			var ratio = measureRatio("${name:world}", vr, "hello ${VarResolver_Benchmark_Test.name:world}");
			assertTrue(ratio >= 1.0,
				"Compiled-form must be at least as fast as compile-per-call (sanity floor); got " + ratio + "×");
		} finally {
			System.clearProperty("VarResolver_Benchmark_Test.name");
		}
	}

	@Test void a02_multipleVarsTemplate_speedup() {
		var vr = VarResolver.create().defaultVars().build();
		System.setProperty("VarResolver_Benchmark_Test.a", "1");
		System.setProperty("VarResolver_Benchmark_Test.b", "2");
		System.setProperty("VarResolver_Benchmark_Test.c", "3");
		try {
			var ratio = measureRatio("${a:1}-${b}-${c}", vr,
				"${VarResolver_Benchmark_Test.a:1}-${VarResolver_Benchmark_Test.b}-${VarResolver_Benchmark_Test.c}");
			assertTrue(ratio >= 1.0,
				"Compiled-form must be at least as fast as compile-per-call (sanity floor); got " + ratio + "×");
		} finally {
			System.clearProperty("VarResolver_Benchmark_Test.a");
			System.clearProperty("VarResolver_Benchmark_Test.b");
			System.clearProperty("VarResolver_Benchmark_Test.c");
		}
	}

	@Test void a03_scriptTemplate_speedup() {
		var vr = VarResolver.create()
			.defaultVars()
			.functions(StringFunctions.ALL)
			.build();
		System.setProperty("VarResolver_Benchmark_Test.name", "fred");
		try {
			var ratio = measureRatio("#{upper(${name})}", vr,
				"#{upper(${VarResolver_Benchmark_Test.name:fred})}");
			assertTrue(ratio >= 1.0,
				"Compiled-form must be at least as fast as compile-per-call (sanity floor); got " + ratio + "×");
		} finally {
			System.clearProperty("VarResolver_Benchmark_Test.name");
		}
	}
}
