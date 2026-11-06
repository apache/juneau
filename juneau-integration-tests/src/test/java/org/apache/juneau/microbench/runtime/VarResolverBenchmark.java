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
package org.apache.juneau.microbench.runtime;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.svl.*;
import org.openjdk.jmh.annotations.*;

/**
 * JMH benchmark for resolving pre-compiled SVL templates.
 *
 * <p>
 * Covers plain text, a {@code ${...}} var reference, scalar {@code #{...}} expressions and a {@code .![ ]} projection
 * over a list of maps.  It is the reference for before/after comparisons of changes to the segment classes.
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py runtime --include 'VarResolverBenchmark.*'
 * </pre>
 *
 * @since 10.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class VarResolverBenchmark {

	/** Counter var: {@code ${C:x}} returns {@code "x"} prefixed with a constant. */
	public static class CountVar extends SimpleVar {
		/** Constructor. */
		public CountVar() { super("C"); }
		@Override public String resolve(VarResolverSession session, String key) { return "c-" + key; }
	}

	/** Function {@code rows()} returning ten rows of two fields. */
	public static class Rows implements VarFunction {
		private static final List<Object> ROWS = rows();

		@Override public String name() { return "rows"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return "rows"; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return ROWS; }

		private static List<Object> rows() {
			var l = new ArrayList<Object>();
			for (var i = 0; i < 10; i++)
				l.add(Map.of("id", "r" + i, "n", (long)i));
			return l;
		}
	}

	private VarResolver resolver;
	private VarTemplate plain, varRef, mixed, arith, func, projection;

	/** Builds the resolver and compiles the templates. */
	@Setup
	public void setup() {
		resolver = VarResolver.create().defaultFunctions().vars(CountVar.class).functions(Rows.class).build();
		plain = resolver.compile("Hello world, no markers here");
		varRef = resolver.compile("Value: ${C:abc}");
		mixed = resolver.compile("a ${C:x} b ${C:y} c");
		arith = resolver.compile("Sum: #{1 + 2}");
		func = resolver.compile("Name: #{upper('a')}");
		projection = resolver.compile("#{join(',', rows().![id + '-' + n])}");
	}

	/** @return Resolved text. */
	@Benchmark public String plainText() { return plain.resolve(resolver.createSession()); }

	/** @return Resolved text. */
	@Benchmark public String varReference() { return varRef.resolve(resolver.createSession()); }

	/** @return Resolved text. */
	@Benchmark public String mixedVars() { return mixed.resolve(resolver.createSession()); }

	/** @return Resolved text. */
	@Benchmark public String exprArithmetic() { return arith.resolve(resolver.createSession()); }

	/** @return Resolved text. */
	@Benchmark public String exprFunction() { return func.resolve(resolver.createSession()); }

	/** @return Resolved text. */
	@Benchmark public String exprProjection() { return projection.resolve(resolver.createSession()); }
}
