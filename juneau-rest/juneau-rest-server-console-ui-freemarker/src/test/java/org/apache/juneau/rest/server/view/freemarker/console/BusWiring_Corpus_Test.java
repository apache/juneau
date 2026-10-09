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
package org.apache.juneau.rest.server.view.freemarker.console;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Runs the shared R-10 / R-11 corpus ({@code $VIEWS/src/test/resources/bus-wiring-corpus.json}, Q9) through
 * {@link BusWiringValidator}, against the real registry: {@code html}, the ServiceLoader-registered
 * {@code DatatablesCardType}, and the corpus's own {@code cardTypes} stubs.  Task 5's {@code bus-wiring.cjs} runs
 * the same cases through {@code JuneauViews.bus.wiring.validate} and checks {@code expectJs}.
 */
class BusWiring_Corpus_Test extends TestBase {

	private static final Path CORPUS = Path.of(System.getProperty("basedir", "."),
		"../juneau-rest-server-views/src/test/resources/bus-wiring-corpus.json");

	/** A corpus {@code cardTypes} entry; {@code null} roles/ops mean "unknown to Java". */
	private record CorpusType(String type, Set<String> roles, Set<String> ops, List<String> publishes) implements CardTypeHandler {
		@Override public JsonMap toFragment(CardSource source) { return new JsonMap(); }
		@Override public List<String> implicitTopics(JsonMap card) { return publishes; }
		@Override public Set<String> acceptedRoles() { return roles; }
		@Override public Set<String> acceptedOps() { return ops; }
	}

	@SuppressWarnings("unchecked")
	private static Set<String> set(Object list) {
		return list == null ? null : Set.copyOf((List<String>)list);
	}

	@SuppressWarnings("unchecked")
	@TestFactory Stream<DynamicTest> a01_corpus() throws Exception {
		var corpus = JsonMap.ofString(Files.readString(CORPUS, UTF_8));
		assertTrue(CardTypeRegistry.standard().isRegistered("datatables"), "DatatablesCardType is ServiceLoader-registered");
		var b = CardTypeRegistry.standard().copy();
		for (var e : corpus.getMap("cardTypes").entrySet()) {
			var t = (Map<String,Object>)e.getValue();
			b.add(new CorpusType(e.getKey(), set(t.get("roles")), set(t.get("ops")), (List<String>)t.get("publishes")));
		}
		var registry = b.build();
		return corpus.getList("cases").stream().map(o -> (JsonMap)o).map(c -> DynamicTest.dynamicTest(c.getString("name"), () -> {
			var expected = c.getList("expect").stream().map(o -> (Map<String,Object>)o)
				.map(m -> new BusWiringValidator.Problem((String)m.get("code"), (String)m.get("message"))).toList();
			assertEquals(expected, BusWiringValidator.validate(c.getMap("contract"), registry));
		}));
	}

	@Test void a02_corpusCoversEveryJavaCode() throws Exception {
		var corpus = JsonMap.ofString(Files.readString(CORPUS, UTF_8));
		var codes = new TreeSet<String>();
		for (var c : corpus.getList("cases"))
			for (var p : ((JsonMap)c).getList("expect"))
				codes.add(((Map<?,?>)p).get("code").toString());
		assertEquals(new TreeSet<>(List.of("E-40", "E-41", "E-42", "E-43", "E-44", "E-45", "E-46", "E-47", "E-48", "E-49",
			"E-51", "E-52", "E-53", "E-54")), codes, "E-50 is FTL-only (Task 9)");
	}
}
