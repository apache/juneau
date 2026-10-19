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

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.microbench.runtime.BenchBeans.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.openjdk.jmh.annotations.*;

/**
 * JMH benchmark for an in-process REST round trip through {@link MockRestClient}.
 *
 * <p>
 * The client is built with {@link MockRestClient#buildLax(Object)} and sends and reads raw strings, so the client side
 * does no marshalling. The measured path is request building, mock transport, server-side dispatch, and the
 * server's JSON parse and serialize of the {@link BenchBeans#order()} graph:
 * <ul>
 * 	<li>{@link #ping()}: <c>GET</c> returning a short string, the dispatch overhead on its own.
 * 	<li>{@link #getOrder()}: <c>GET</c> with a path variable, serializing the order graph.
 * 	<li>{@link #postOrder()}: <c>POST</c> parsing the order graph and serializing it back.
 * </ul>
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py runtime --include RestMockBenchmark
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
public class RestMockBenchmark {

	/** REST resource under test. */
	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class OrderResource {

		private static final Order ORDER = BenchBeans.order();

		/**
		 * Returns a short constant.
		 *
		 * @return The constant.
		 */
		@RestGet("/ping")
		public String ping() {
			return "pong";
		}

		/**
		 * Returns the shared order graph.
		 *
		 * @param id The order id.
		 * @return The order graph.
		 */
		@RestGet("/orders/{id}")
		public Order getOrder(@Path("id") String id) {
			return ORDER;
		}

		/**
		 * Echoes the posted order graph.
		 *
		 * @param order The parsed order graph.
		 * @return The same graph.
		 */
		@RestPost("/orders")
		public Order postOrder(@Content Order order) {
			return order;
		}
	}

	private MockRestClient client;
	private String orderJson;

	/**
	 * Builds the client and checks that every endpoint answers with status 200.
	 *
	 * @throws Exception If an endpoint fails.
	 */
	@Setup(Level.Trial)
	public void setup() throws Exception {
		client = MockRestClient.buildLax(OrderResource.class);
		orderJson = JsonSerializer.DEFAULT.write(BenchBeans.order());
		check(client.get("/ping").run().getStatusCode(), "/ping");
		check(client.get("/orders/123").run().getStatusCode(), "/orders/123");
		var echoed = client.post("/orders", orderJson).contentType("application/json").run().getContent().asString();
		if (! orderJson.equals(echoed))
			throw isex("POST /orders did not echo the order:%n%s%n%s", orderJson, echoed);
	}

	/**
	 * Closes the client.
	 *
	 * @throws Exception If closing fails.
	 */
	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		client.close();
	}

	/**
	 * Round trip of a GET that returns a short string.
	 *
	 * @return The response body.
	 * @throws Exception If the call fails.
	 */
	@Benchmark
	public String ping() throws Exception {
		return client.get("/ping").run().getContent().asString();
	}

	/**
	 * Round trip of a GET that serializes the order graph.
	 *
	 * @return The response body.
	 * @throws Exception If the call fails.
	 */
	@Benchmark
	public String getOrder() throws Exception {
		return client.get("/orders/123").run().getContent().asString();
	}

	/**
	 * Round trip of a POST that parses the order graph and serializes it back.
	 *
	 * @return The response body.
	 * @throws Exception If the call fails.
	 */
	@Benchmark
	public String postOrder() throws Exception {
		return client.post("/orders", orderJson).contentType("application/json").run().getContent().asString();
	}

	private static void check(int status, String path) {
		if (status != 200)
			throw isex("GET %s returned status %s", path, status);
	}
}
