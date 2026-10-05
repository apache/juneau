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
package org.apache.juneau.rest.server.views;

import java.util.*;

import org.apache.juneau.commons.beanquery.*;

/**
 * Test-only helper that converts a resolved {@link Filter} tree into the shared parity corpus's canonical JSON
 * shape: {@code and}/{@code or}/{@code not} wrapping {@code leaf}+{@code expr} nodes, where {@code expr} is
 * {@code {"lit": s}} for a literal {@link SearchExpression} or {@code {"op": name, "args": [...]}} for an
 * operator call. {@link SearchCorpus_Test} compares this against a corpus case's {@code tree} field (itself
 * parsed from JSON into the same {@code Map}/{@code List}/{@code String} shape), so the two sides compare with
 * plain {@link Object#equals}. Package-private: this is a test fixture, not public API, so it does not carry
 * the public-type Javadoc Example this plan otherwise requires.
 */
final class CorpusTrees {

	private CorpusTrees() {}

	/** Converts a {@link Filter} tree to the corpus's canonical {@code Map} shape. */
	static Map<String,Object> of(Filter filter) {
		if (filter instanceof Filter.And a)
			return Map.of("and", listOf(a.items()));
		if (filter instanceof Filter.Or o)
			return Map.of("or", listOf(o.items()));
		if (filter instanceof Filter.Not n)
			return Map.of("not", of(n.item()));
		var leaf = (Filter.Leaf) filter;
		var m = new LinkedHashMap<String,Object>();
		m.put("leaf", leaf.column());
		m.put("expr", exprOf(leaf.expression()));
		return m;
	}

	private static List<Object> listOf(List<Filter> items) {
		var out = new ArrayList<Object>(items.size());
		for (var item : items)
			out.add(of(item));
		return out;
	}

	private static Map<String,Object> exprOf(SearchExpression node) {
		if (node.isLiteral())
			return Map.of("lit", node.value());
		var m = new LinkedHashMap<String,Object>();
		m.put("op", node.name());
		var args = new ArrayList<Object>(node.args().size());
		for (var a : node.args())
			args.add(exprOf(a));
		m.put("args", args);
		return m;
	}
}
