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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.concurrent.*;

import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.microbench.runtime.BenchBeans.*;
import org.openjdk.jmh.annotations.*;

/**
 * JMH benchmark for serializing and parsing the {@link BenchBeans#order()} graph with the default JSON, XML and HTML
 * serializers and parsers.
 *
 * <p>
 * The serializers and parsers are the shared {@code DEFAULT} instances, so their {@code ClassMeta} caches are warm
 * after the first warmup iteration; the numbers are steady-state marshalling cost.
 * {@link #setup()} round-trips the graph once and fails the trial if the parsed graph does not re-serialize to the
 * same text, so a benchmark can't silently measure a broken parse.
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py runtime --include MarshallBenchmark
 * </pre>
 *
 * @since 10.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class MarshallBenchmark {

	/** Format under test. */
	@Param({ "json", "xml", "html" })
	public String format;

	private WriterSerializer serializer;
	private ReaderParser parser;
	private Order order;
	private String serialized;

	/**
	 * Resolves the serializer and parser for {@link #format} and verifies the round trip.
	 *
	 * @throws Exception If the round trip fails.
	 */
	@Setup(Level.Trial)
	public void setup() throws Exception {
		switch (format) {
			case "json" -> {
				serializer = JsonSerializer.DEFAULT;
				parser = JsonParser.DEFAULT;
			}
			case "xml" -> {
				serializer = XmlSerializer.DEFAULT;
				parser = XmlParser.DEFAULT;
			}
			case "html" -> {
				serializer = HtmlSerializer.DEFAULT;
				parser = HtmlParser.DEFAULT;
			}
			default -> throw iaex("Unknown format ''%s''", format);
		}
		order = BenchBeans.order();
		serialized = serializer.write(order);
		var again = serializer.write(parser.read(serialized, Order.class));
		if (! serialized.equals(again))
			throw isex("Round trip mismatch for %s:%n%s%n%s", format, serialized, again);
	}

	/**
	 * Serializes the order graph to a string.
	 *
	 * @return The serialized text.
	 * @throws Exception If serialization fails.
	 */
	@Benchmark
	public String serialize() throws Exception {
		return serializer.write(order);
	}

	/**
	 * Parses the serialized order graph back into beans.
	 *
	 * @return The parsed graph.
	 * @throws Exception If parsing fails.
	 */
	@Benchmark
	public Order parse() throws Exception {
		return parser.read(serialized, Order.class);
	}
}
