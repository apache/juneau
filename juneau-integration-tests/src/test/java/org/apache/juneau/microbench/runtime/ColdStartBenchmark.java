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

import org.apache.juneau.marshall.json.*;
import org.apache.juneau.microbench.runtime.BenchBeans.*;
import org.openjdk.jmh.annotations.*;

/**
 * JMH single-shot benchmark for the first serialize and parse in a fresh JVM.
 *
 * <p>
 * Each fork runs one operation with no warmup, so the score includes class loading and static initialization of
 * the marshalling stack, building the {@code ClassMeta} graph and interpreted execution. This is the latency the
 * first request of a newly started application sees.
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py runtime --include ColdStartBenchmark
 * </pre>
 *
 * @since 10.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(10)
public class ColdStartBenchmark {

	/**
	 * Serializes the order graph with {@link JsonSerializer#DEFAULT} for the first time in this JVM.
	 *
	 * @return The serialized text.
	 * @throws Exception If serialization fails.
	 */
	@Benchmark
	public String firstJsonSerialize() throws Exception {
		return JsonSerializer.DEFAULT.write(BenchBeans.order());
	}

	/**
	 * Parses a JSON order graph with {@link JsonParser#DEFAULT} for the first time in this JVM.
	 *
	 * <p>
	 * The input is a literal so the serializer stays uninitialized.
	 *
	 * @return The parsed graph.
	 * @throws Exception If parsing fails.
	 */
	@Benchmark
	public Order firstJsonParse() throws Exception {
		return JsonParser.DEFAULT.read(SAMPLE_JSON, Order.class);
	}

	private static final String SAMPLE_JSON = """
		{"id":"ORD-1","status":"PAID","created":"2026-10-19T14:30:15Z","shipBy":"2026-10-24","priority":true,"total":12.5,
		"customer":{"id":1,"name":"Jane","email":"jane@example.org","since":"2019-04-01",
		"addresses":[{"street":"1 Main Street","city":"Springfield","postalCode":"12345","country":"US","primary":true}],
		"preferences":{"language":"en"}},
		"items":[{"sku":"SKU-1","description":"Item","quantity":2,"unitPrice":6.25,"status":"PAID","tags":["a","b"]}],
		"history":["PENDING","PAID"],"attributes":{"k":"v"},"counters":{"c":1}}
		""";
}
