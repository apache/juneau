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

import java.util.concurrent.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.microbench.runtime.BenchBeans.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;

/**
 * JMH benchmark for {@link MarshallingContext} and {@link ClassMeta} cost, on first use and once cached.
 *
 * <p>
 * "First use" builds a new {@link MarshallingContext} through {@code build(MarshallingContext.class)}, which skips the
 * hash-key context cache, so each operation starts with an empty {@code ClassMeta} cache. JVM-wide reflection
 * caches (for example {@code ClassInfo}) stay warm across operations, so these numbers are the cost of a new
 * context configuration in a running application, not of a cold JVM; {@link ColdStartBenchmark} covers the
 * cold-JVM case.
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py runtime --include ClassMetaBenchmark
 * </pre>
 *
 * @since 10.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class ClassMetaBenchmark {

	private MarshallingContext shared;
	private Order order;

	/**
	 * Builds the shared, cached context and warms its {@code ClassMeta} cache for the bean graph.
	 */
	@Setup(Level.Trial)
	public void setup() {
		shared = MarshallingContext.DEFAULT;
		order = BenchBeans.order();
		resolveGraph(shared, null);
	}

	/**
	 * Builds a new uncached context with default settings.
	 *
	 * @return The new context.
	 */
	@Benchmark
	public MarshallingContext contextCreate() {
		return MarshallingContext.create().build(MarshallingContext.class);
	}

	/**
	 * Builds a new uncached context and resolves the {@code ClassMeta} and {@code BeanMeta} of every bean in the graph.
	 *
	 * @param bh JMH blackhole.
	 */
	@Benchmark
	public void classMetaFirstUse(Blackhole bh) {
		resolveGraph(MarshallingContext.create().build(MarshallingContext.class), bh);
	}

	/**
	 * Resolves the {@code ClassMeta} and {@code BeanMeta} of every bean in the graph from a warm context.
	 *
	 * @param bh JMH blackhole.
	 */
	@Benchmark
	public void classMetaCached(Blackhole bh) {
		resolveGraph(shared, bh);
	}

	/**
	 * Builds a JSON serializer on a new uncached context and serializes the graph once.
	 *
	 * <p>
	 * This is the cost an application pays the first time it serializes a bean type with a new serializer
	 * configuration.
	 *
	 * @return The serialized text.
	 * @throws Exception If serialization fails.
	 */
	@Benchmark
	public String firstSerializeNewContext() throws Exception {
		var mc = MarshallingContext.create().build(MarshallingContext.class);
		return JsonSerializer.create().marshallingContext(mc).build(JsonSerializer.class).write(order);
	}

	private static void resolveGraph(MarshallingContext mc, Blackhole bh) {
		consume(bh, mc.getClassMeta(Order.class).getBeanMeta());
		consume(bh, mc.getClassMeta(Customer.class).getBeanMeta());
		consume(bh, mc.getClassMeta(LineItem.class).getBeanMeta());
		consume(bh, mc.getClassMeta(Address.class).getBeanMeta());
		consume(bh, mc.getClassMeta(Status.class));
	}

	private static void consume(Blackhole bh, Object o) {
		if (bh != null)
			bh.consume(o);
	}
}
