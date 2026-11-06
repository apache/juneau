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

import org.apache.juneau.commons.template.*;
import org.openjdk.jmh.annotations.*;

/**
 * JMH benchmark for rendering parsed templates with {@link TemplateResolver}.
 *
 * <h5 class='section'>Running:</h5>
 * <pre class='bconsole'>
 * 	$ python3 scripts/microbench.py runtime --include 'TemplateResolverBenchmark.*'
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
public class TemplateResolverBenchmark {

	private TemplateResolver resolver;
	private ParsedTemplate scalar, projection, table;
	private Map<String,Object> scope;

	/** Builds the resolver, the data and the parsed templates. */
	@Setup
	public void setup() {
		resolver = TemplateResolver.create().destination(Destinations.HTML).build();
		var rows100 = new ArrayList<Object>();
		for (var i = 0; i < 100; i++)
			rows100.add(Map.of("id", "r" + i, "msg", "message " + i, "n", (long)i));
		var rows20 = new ArrayList<Object>();
		for (var i = 0; i < 20; i++)
			rows20.add(Map.of("id", "r" + i, "msg", "message " + i, "n", (long)i));
		scope = Map.of("name", "Bob", "count", 7L, "rows", rows100, "few", rows20);
		scalar = resolver.parse("Hello ${name}, you have ${count} items.");
		projection = resolver.parse("#{join(',', rows.![id + ':' + msg + ':' + n])}");
		table = resolver.parse("#{table(list('ID','Message','N'), few.![list(id, msg, n)])}");
	}

	/** @return Rendered text. */
	@Benchmark public String scalarOnly() { return resolver.render(scalar, scope); }

	/** @return Rendered text. */
	@Benchmark public String projection100x3() { return resolver.render(projection, scope); }

	/** @return Rendered text. */
	@Benchmark public String table20() { return resolver.render(table, scope); }
}
